# MojiBattleEX API — Ability簡易登録

## レジスターと登録

能力種別ごとの型付きレジスターを作り、Runtimeクラスを指定します。補助スキルと補助必殺技は既存API上で同じ`SupportAbilityRuntime`／レジストリを共有します。

```java
public static final MojiBattleRegister.Skill SKILLS = MojiBattleRegister.createSkill(MOD_ID);
public static final MojiBattleRegister.Ultimate ULTIMATES = MojiBattleRegister.createUltimate(MOD_ID);
public static final MojiBattleRegister.Passive PASSIVES = MojiBattleRegister.createPassive(MOD_ID);
public static final MojiBattleRegister.SupportAbility SUPPORT_ABILITIES =
        MojiBattleRegister.createSupportAbility(MOD_ID);
public static final MojiBattleRegister.SupportPassive SUPPORT_PASSIVES =
        MojiBattleRegister.createSupportPassive(MOD_ID);

public static final RegistryHandle<SkillImpl> TREE =
        SKILLS.register("return_to_forest", ReturnToForestSkill.class);
public static final RegistryHandle<UltimateImpl> STORM =
        ULTIMATES.register("forest_storm", ForestStormUltimate.class);
public static final RegistryHandle<PassiveImpl> ROOTS =
        PASSIVES.register("deep_roots", DeepRootsPassive.class);
public static final RegistryHandle<SupportAbilityImpl> AID =
        SUPPORT_ABILITIES.register("forest_aid", ForestAid.class);
public static final RegistryHandle<SupportPassiveImpl> AURA =
        SUPPORT_PASSIVES.register("forest_aura", ForestAura.class);
```

各レジスターは`GameMod.register`で従来どおり`attach(context.registrationBus())`します。`register(path, Supplier)`および能力種別ごとの詳細Factory登録も維持されています。

## RuntimeとConfig

Runtimeクラスには、publicな`(ConfigRecord)`コンストラクタを1つだけ用意します。Runtime契約の実装、具象クラス、コンストラクタの一意性、Config recordの宣言は登録時に検証されます。`AbstractRuntime`の継承は任意です。

```java
public record Config(
        @DisplayValue
        @ConfigFloat(defaultValue = 30f)
        float damage,
        @ConfigInt(defaultValue = 3, min = 1)
        int hits,
        @ConfigBoolean(defaultValue = true)
        boolean knockback,
        @ConfigString(defaultValue = "forest")
        String style,
        @ConfigList(ConfigList.ValueType.STRING)
        List<String> tags,
        @ConfigResource(name = "impactEffectId", defaultValue = "mojibattle:wind")
        ResourceId impactEffect
) { }
```

- `@ConfigFloat`と`@ConfigInt`は既定値、最小値、最大値、境界を含むかどうかを宣言します。
- `@ConfigBoolean`、`@ConfigString`、`@ConfigResource`は各型の既定値を宣言します。
- `@ConfigList`は`List<String>`、`List<Float>`と、その2次元Listを不変Listとして読み取ります。
- 任意のResourceIdは`@ConfigResource(defaultValue = "", nullable = true)`で宣言できます。
- `@RequiredConfig`を併記すると、既定値を使わずJSONでの指定を必須にします。
- `name`でJSONキーを変更できます。重複キーは登録時に拒否されます。
- `@DisplayValue`は複数指定でき、record宣言順でPresentationの引数になります。未指定なら翻訳キーだけを生成します。
- JSON値は既定値より優先されます。不正な型、null、非整数のint、NaN、Infinity、範囲外、空文字禁止違反、不正なResourceIdは例外になります。
- 複数項目間の制約は`register(path, Runtime.class, additionalValidator)`で追加できます。

`ConfigSchema`と`ConfigKey`を使う従来方式も利用できます。アノテーション方式のrecord解析結果はレジスター宣言時に保持され、Runtime生成ごとにクラス構造を再解析するグローバルキャッシュは使いません。

## PresentationとJSON互換性

スキル、必殺技、メインパッシブだけが自動Presentationを持ち、それぞれ既存の省略キー形式`.skill.<path>`、`.ultimate.<path>`、`.passive.<path>`を使います。補助能力にはPresentationを追加しません。

`nameKey`、`descriptionKey`、`cooldownSeconds`、`pointCost`など既存DTOの項目は従来どおりJSON側で管理されます。`parameters`は既存のフラット化ビューを通してConfig recordへ渡されるため、JSON形式の変更は不要です。

## 互換性

- `SkillImpl`、`UltimateImpl`、`PassiveImpl`、`SupportAbilityImpl`の既存コンストラクタと詳細登録は維持されています。
- `SupportPassiveImpl(String, AbilityFactory)`も維持され、検証付きの3引数コンストラクタが追加されています。
- 汎用`MojiBattleRegister.create(...)`と`register(path, Supplier)`は引き続き利用できます。
- 簡易登録を使う場合だけ、能力種別ごとの`createSkill`などを選択します。

この変更ではGradle、Javaコンパイル、テスト、ゲーム起動を実行していません。
