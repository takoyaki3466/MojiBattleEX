# MojiBattleEX MODアーキテクチャ

## モジュール境界

```text
                         +----------------+
                         |      api       |
                         +----------------+
                           ^      ^      ^
                           |      |      |
                  +--------+  +---+---+  +------+
                  | core   |  | loader|  | item |
                  +--------+  +-------+  +------+
                       ^         ^
                       +---------+
                         lwjgl3
```

`api`はlibGDXへ依存しない。`core`は`item`を参照せず、`lwjgl3`も標準MODのJava型を参照しない。
標準`item`を含むMOD JARは`mod-loader`が`mods`直下から読み込む。ServiceLoader列挙は旧配置との互換入口である。
標準`item`の固有Java Abilityは移行互換のため、現在もcore SPIを利用する。

## 起動ライフサイクル

1. `Lwjgl3Launcher`が互換用ServiceLoaderと`mods`ディレクトリを調べる。
2. `ModLoader`が全metadata、API/game version、必須・任意依存、循環を検証する。
3. 依存順の共有`URLClassLoader`から`GameMod`を生成する。
4. `GameBootstrap`がMODごとの`register(ModContext)`を呼ぶ。
5. `ContentRegistrar`へのJSON登録を封印し、Registryの登録イベントを配送する。
6. 標準JSONと外部MOD JSONを読み、値・参照・リソースを検証する。
7. Definitionからプレイヤー別`AbilityLoadout`のFactory表を構築する。
8. Registryを凍結し、各MODの`initialize(ModContext)`を呼ぶ。
9. `GameContext`へサービスを渡し、終了時にイベント、libGDX資産、ClassLoaderを閉じる。

登録失敗時は全Registryを`FAILED`へ遷移させ、ゲームを開始しない。同じJVMでの再試行は非対応である。

## Registryと実行時状態

`RegistryId`は`namespace:path`、`RegistryKey<T>`はRegistry自体の型付きID、`RegistryHandle<T>`は
登録確定後だけ解決できる参照である。`DeferredRegister<T>`はSupplierを登録イベントまで遅延する。
重複ID、ID不一致、未知ID、未束縛Handle、凍結後変更は例外になる。

Registryは共有DefinitionまたはFactoryだけを保持する。`AbilityRegistry`も実体ではなくSupplierを保持し、
`BattleController`は参加キャラクターごとに`AbilityLoadout`を生成する。クールタイムやパッシブ内部状態は
プレイヤー間で共有しない。

## 公開データ登録

第三者MODは`ModContext.content()`へ`CharacterDataRegistration`を渡す。登録単位はキャラクター、スキル、
必殺技、パッシブ、翻訳ディレクトリで、全Resource IDはMOD自身の名前空間でなければならない。
coreは4つのJSONをすべて読み、ID所有権と相互参照を確認してからDefinition Registryへ追加する。

この経路は既存Ability実装をデータから再利用するためのもの。独自Java Ability実装用SPIはまだcoreにあり、
公開APIへの移行対象として残る。

## リソース規約

```text
META-INF/mojibattle/mod.json
assets/<modid>/i18n/<language>.json
assets/<modid>/textures/...
data/<modid>/characters/...
data/<modid>/abilities/...
```

`ResourceManager`はASSETとDATAを分け、open/exists/listを提供する。標準コンテンツの旧`config/...`、
`i18n/...`配置には互換フォールバックがある。設定、翻訳、キャラクター／攻撃／VFX Textureはこの層を通る。
Sound、フォント、共通UI画像はホスト資産として従来のlibGDX内部ファイル経路を維持する。

## ClassLoaderと信頼境界

初期loaderはMOD間依存を扱いやすくするため共有ClassLoaderを使う。Game APIは親から取得し、MOD JARへ
APIクラスが同梱されていればロード前に拒否する。共有方式はライブラリ競合を隔離しない。
またClassLoaderはサンドボックスではなく、MODはユーザーと同じ権限で任意コードを実行できる。

## 残る公開API課題

- 独自Javaスキル・必殺技・パッシブ実装用SPIをapiへ移し、`item -> core`をなくす。
- 外部MOD向け音声、補助キャラクター、フィールド効果、UI拡張の登録契約を追加する。
- ビルドが許可された段階でAPI互換、loader、データ統合、起動の自動テストを追加する。
