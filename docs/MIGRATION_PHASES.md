# MODシステム Phase 1～9 実装記録

ビルド、コンパイル、テスト、Gradleタスク、ゲーム起動を行わず、静的検査だけで移行した記録である。

## Phase 1: 依存関係整理

- `core`から`item`参照を排除し、Composition Rootをcoreへ移した。
- JSONロード、意味検証、Registry解決、実行Factoryを分離した。

## Phase 2: API分離

- 独立`api`へGameMod、ModContext、Registry、EventBus、Resource、戦闘ビューを配置した。
- `ContentRegistrar`により、MODはcore DTOを使わずJSON一式を宣言できる。
- APIはlibGDXへ依存しない。

## Phase 3: core整理

- 起動順序を`core.bootstrap.GameBootstrap`へ集約した。
- 状態付きAbilityをプレイヤー単位の`AbilityLoadout`へ分離した。
- `GdxTextureLoader`で公開ResourceManagerとlibGDXの境界を限定した。

## Phase 4: Registry整備

- 名前空間ID、型付きKey、DeferredRegister、Handle、凍結、失敗状態を実装した。
- 重複、ID不一致、未知ID、未束縛Handle、凍結後変更を拒否する。

## Phase 5: 標準item MOD化

- `ItemContentModule`、metadata、ServiceLoader互換定義を追加した。
- 標準JARも開発・配布の`mods`へ置き、外部MODと同じloaderで読む構成にした。
- 固有Java Abilityは互換のためcore SPIを利用しており、`item -> core`完全撤去は次期API課題である。

## Phase 6: MOD loader

- JAR走査、metadata、version、依存、循環、ロード順、entrypointを検証する。
- 共有URLClassLoaderを一度だけ生成し、Game API同梱JARを拒否する。

## Phase 7: アセット・データ分離

- `assets/<modid>`と`data/<modid>`をResourceManagerで扱う。
- 外部キャラクター、Ability、翻訳を登録・統合する。
- 外部JAR画像をbyte streamからTextureへ変換する。

## Phase 8: Gradle・配布

- 5モジュールを定義し、API publication、開発用MOD配置、配布用MOD配置を設定した。
- jpackage入力へ標準MOD JARを`mods`として含める構成にした。

## Phase 9: 外部MOD開発環境

- `examples/example-mod`へ独立Gradle設定、GameMod、metadata、4種のJSON、翻訳を追加した。
- サンプルはAPIへcompileOnly依存し、標準Abilityをデータから再利用する。

実行確認は今回の禁止条件により未実施である。
