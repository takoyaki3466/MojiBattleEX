# 外部MOD開発ガイド

## 現在利用できる範囲

外部JARの検出、metadata検証、依存順、`GameMod`、Registryイベント、名前空間付きリソースに加え、
公開APIとJSONだけで既存Ability実装を再利用するキャラクターを追加できる。完全な例は
`examples/example-mod`にある。独自Java Abilityロジックの公開SPIは移行中である。

## JAR構造

```text
META-INF/mojibattle/mod.json
com/example/examplemod/ExampleMod.class
assets/examplemod/i18n/ja_jp.json
assets/examplemod/textures/example.png
data/examplemod/characters/example.json
data/examplemod/abilities/example_skill.json
```

Game APIをJARへ同梱してはいけない。Gradleでは
`compileOnly("jp.takoa.mojibattle:mojibattle-api:0.1.0")`として参照する。

## metadata

```json
{
  "schemaVersion": 1,
  "id": "examplemod",
  "name": "Example MOD",
  "version": "1.0.0",
  "apiVersion": "1",
  "gameVersion": ">=0.1.0",
  "entrypoint": "com.example.examplemod.ExampleMod",
  "dependencies": {
    "required": {
      "mojibattle_game": ">=0.1.0",
      "mojibattle": ">=0.1.0"
    },
    "optional": {}
  }
}
```

`mojibattle_game`はホストを表す予約ID、`mojibattle`は標準コンテンツMODである。依存版指定は空文字または
`>=x.y.z`、gameVersionは完全一致または`>=x.y.z`を受理する。

## エントリーポイント

```java
public final class ExampleMod implements GameMod {
    @Override
    public String id() {
        return "examplemod";
    }

    @Override
    public void register(ModContext context) {
        context.content().registerCharacter(new CharacterDataRegistration(
                id("characters/example.json"),
                id("abilities/example_skill.json"),
                id("abilities/example_ultimate.json"),
                id("abilities/example_passive.json"),
                "i18n"));
    }

    private ResourceId id(String path) {
        return new ResourceId(id(), path);
    }
}
```

外部Definitionの`id`と参照先は`examplemod:example`のように必ず名前空間を付ける。画像は
`examplemod:textures/example.png`の形式で自MODを参照でき、依存先の公開資産もResource IDで参照できる。

JARは`mods`直下へ置く。サブディレクトリ、シンボリックリンク、JAR以外は走査しない。
MODはサンドボックス化されないため、信頼できるJARだけを使用する。
