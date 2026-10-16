# 開発用MOD配置フォルダ

`./gradlew lwjgl3:run`で読み込みたい外部MOD JARを、このフォルダの直下へ配置してください。

実行前に、ここにある`*.jar`と標準`item` JARが`lwjgl3/build/dev-mods/`へ同期されます。サブフォルダ内のJAR、シンボリックリンク、APIを内包したJARは読み込み対象外または検証エラーになります。
