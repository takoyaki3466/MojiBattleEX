# Example MOD

本体の`api`成果物だけへ`compileOnly`依存し、既存のAbility実装をJSONから再利用する例です。

1. 本体で`api`をMaven Localへ公開する。
2. このディレクトリを独立したGradleプロジェクトとしてビルドする。
3. 生成JARをゲームの`mods`直下へ配置する。

このリポジトリでは依頼条件により上記Gradleタスクを実行していません。
