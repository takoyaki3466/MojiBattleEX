# Minecraft MOD型アーキテクチャ 静的確認結果

ビルド、コンパイル、Gradleタスク、テスト、アプリケーション起動、ゲーム実行は行っていない。

実施した確認:

- Java 151ファイルのpackage宣言と配置パスが一致する。
- `core` から `jp.takoa.mojibattle.item` への参照は0件。
- 旧 `jp.takoa.mojibattle.item.ability` 参照は0件。
- 個別Ability実装はcoreに残っておらず、`item/impl/ability` に配置されている。
- init 6クラスはDeferredRegister、RegistryHandle、ID、Factory参照だけを持つ。
- RegistryKeyは9種あり、RegistryManagerが一意に生成する。
- DeferredRegisterの宣言IDはスキル4、必殺技10、パッシブ13、エフェクト16、
  フィールド効果10、補助能力12で、各init内に重複はない。
- `namespace:path` の正規化とnamespaceなし旧IDの互換経路を確認した。
- 初期化順は登録イベント完了後にJSONを読み、検証後にRuntimeFactoryで生成し、最後に凍結する。
- RegistryHandleは未束縛取得と二重束縛を拒否する。
- AbilityRuntimeFactoryはDefinitionごとにFactoryを評価し、状態付きAbilityをHandleで共有しない。
- ライフサイクルEventBusとゲームEventBusは別インスタンスである。
- EventBusは同一リスナーの重複購読を拒否し、解除Handleと決定的な配送順を持つ。
- JSONローダー、意味Validator、RegistryResolver、RuntimeFactoryは別クラスである。
- `git diff --check` で空白エラーは検出されていない（改行コード警告のみ）。

静的確認だけでは保証していない項目:

- Java型検査およびlibGDX APIとのバイナリ互換性
- 実際の初期化時におけるイベント配送とアセットロード
- 描画、入力、音声、ネイティブライブラリ、戦闘バランス
- 実行時のフレームレートとメモリ使用量

## 2026-10-14 MODローダー移行追補

依頼条件に従い、Gradle、Javaコンパイラ、テスト、アプリケーション、ゲームは実行していない。

- `api`、`core`、`mod-loader`、`item`、`lwjgl3`のJava 175ファイルについて、package宣言と配置パスの一致を確認した。
- 上記175ファイルに重複FQCNがないことを確認した。
- プロジェクト内の明示importが実在するプロジェクト型を参照することを文字列ベースで確認した。
- `api`にlibGDX、core、itemのimportがないことを確認した。
- `core`にitemまたはmod-loaderへのimportがないことを確認した。
- `lwjgl3`にitemのJava型へのimportがないことを確認した。
- 全Javaファイルの単純な波括弧数が一致することを確認した。
- assetsおよびitem metadataのJSON 73ファイルを`ConvertFrom-Json`で解析した。
- `git diff --check`で空白エラーがないことを確認した（GitのLF/CRLF変換警告は除く）。
- `settings.gradle.kts`の5モジュールと各`project(...)`依存を目視し、Gradleモジュールの循環参照がないことを確認した。
- API同梱拒否対象が、実際の`api`モジュールの5パッケージ接頭辞と一致することを確認した。

静的確認では保証していない項目:

- Java型検査、libGDX APIのメソッドシグネチャ、およびGradle DSLの評価
- 外部JARの実走査、ClassLoader生成、ServiceLoaderとの結合
- stream終了時のJARハンドル解放、Windows上のファイルロック挙動
- 設定・翻訳の実読込み、および既存ゲーム挙動との実行時互換性

## 2026-10-15 Phase 1～9追補

ビルド、コンパイル、テスト、Gradleタスク、アプリケーション、ゲームは実行していない。

- 本体5モジュール179型と外部サンプル1型、合計Java 180ファイルを対象にした。
- `tools/check-mod-architecture.ps1`で本体179型のpackage配置、重複FQCN、API禁止依存を確認した。
- assets、標準MOD metadata、外部サンプルを含むJSON 79ファイルを`ConvertFrom-Json`で解析した。
- プロジェクト内部の明示importが、対応するソース型へ解決できることを文字列ベースで確認した。
- 標準MODとサンプルMODのmetadataが`mojibattle_game`ホスト依存を宣言することを確認した。
- サンプルが参照する標準Ability実装ID、VFX ID、画像パスの存在を静的に照合した。
- `lwjgl3`の通常依存から`item`がなく、配布／開発用Copy入力としてのみ参照されることを確認した。
- Java、Kotlin DSL、JSON、Markdown、PowerShellの対象ファイルに末尾空白がないことを確認した。
- `git diff --check`で空白エラーがないことを確認した（LF/CRLF変換警告は除く）。

静的検査ではJava型検査、Gradle DSL評価、JAR生成、ClassLoader、libGDX Texture生成、画面反映、
jpackageの`$APPDIR`展開を保証していない。これらは実行禁止が解除された後に確認する必要がある。
