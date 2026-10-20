# MojiBattleEX API 0.8.2 — 全クラス・インターフェース解説

> 対象: 現在の `api/src/main/java` 全103 Javaファイル（役割別の `package-info.java` を含む）。実際のソースを根拠に整理したAPIリファレンスです。

## はじめに

- Javaコードのパッケージは `jp.takoa.mojibattle` を基点とします。
- `record` は不変のデータ保持、`interface` は実装契約、`Spec` はホストに渡す動作の仕様です。
- 一部の `CombatActions` のdefaultメソッドは、ホストが対応していない場合 `UnsupportedOperationException` を送出します。実装環境の対応を確認してください。
- 本資料はソースにある公開APIを説明します。`mod-loader`・`core` 側の挙動は添付にないため、推測で保証しません。

## API全体像

1. `@ModEntry("my_mod")` を付けた `GameMod` 実装を作る。
2. `GameMod.register(ModContext)` で `MojiBattleRegister` を `registrationBus()` に接続する。
3. `registry.impl` の実装定義と `ability` のRuntimeを組み合わせて能力を登録する。
4. Runtimeは `CombatActions` 経由で戦闘操作を要求し、`ConfigView` から設定を読む。
5. 必要なデータ・翻訳・エフェクトを `content` / `resource` / `registry` で扱う。

## パッケージ別一覧

以下は公開型の数です。役割別に分割した`ability`・`battle`の各パッケージには、別途概要用`package-info.java`が1ファイルずつあります。

- `ability.config`: 1型
- `ability.context`: 5型
- `ability.presentation`: 1型
- `ability.runtime`: 6型
- `ability.support`: 5型
- `battle.action`: 1型
- `battle.action.spec`: 14型
- `battle.combatant`: 4型
- `battle.damage`: 3型
- `battle.event`: 1型
- `battle.modifier`: 2型
- `battle.presentation`: 4型
- `battle.rule`: 1型
- `battle.status`: 5型
- `content`: 3型
- `domain.ability`: 1型
- `event`: 4型
- `event.lifecycle`: 1型
- `mod`: 4型
- `registry`: 12型
- `registry.impl`: 7型
- `resource`: 4型

---

## `jp.takoa.mojibattle.ability.*`

役割ごとの配置は次のとおりです。

- `config`: JSON実装非依存の設定ビュー
- `context`: Runtimeへ渡す発動・更新・イベントコンテキスト
- `presentation`: HUDへ渡す表示モデル
- `runtime`: Abilityの実行契約とファクトリー
- `support`: 補助キャラクター固有の操作・状態・分裂仕様

### `AbilityActivationContext`
**種類:** `record`  
**役割:** スキル・必殺技を実行する際の使用者、対象、戦闘操作を渡す不変コンテキスト。

**recordの構成要素:** `CombatantView user`、`CombatantView target`、`CombatActions actions`

### `AbilityFactory`
**種類:** `interface`  
**役割:** 設定値 ConfigView からプレイヤー個別の能力Runtimeを生成する関数型インターフェース。

### `AbilityStateLine`
**種類:** `record`  
**役割:** 戦闘中に表示する能力の状態文言・引数・表示優先順位・アイコンを保持する。

**recordの構成要素:** `String translationKey`、`List<Object> arguments`、`int priority`、`ResourceId iconId`

**主な公開メソッド／コンストラクタ:**

- `public static AbilityStateLine of(String translationKey, int priority, Object... arguments)`

### `ConfigView`
**種類:** `interface`  
**役割:** JSON由来の設定を型付きで読み取るための抽象ビュー。フォールバック値や入れ子の設定を扱う。

**主な公開メソッド／コンストラクタ:**

- `default String stringValue(String key, String fallback)`
- `default float floatValue(String key, float fallback)`
- `default int intValue(String key, int fallback)`
- `default boolean booleanValue(String key, boolean fallback)`
- `default ConfigView objectValue(String key)`
- `default List<Object> listValue(String key)`
- `public boolean contains(String key)`
- `public Object value(String key)`

### `DamageEventContext`
**種類:** `record`  
**役割:** ダメージ発生時の攻撃者、対象、確定ダメージ、操作手段、経過時間を伝える。

**recordの構成要素:** `CombatantView source`、`CombatantView target`、`float dealtDamage`、`CombatActions actions`、`float battleElapsedSeconds`

### `PassiveRuntime`
**種類:** `interface`  
**役割:** パッシブ固有の能力倍率、会心補正、更新処理、各種イベントフックを提供する。

**主な公開メソッド／コンストラクタ:**

- `default float attackMultiplier(CombatantView owner)`
- `default float moveSpeedMultiplier(CombatantView owner)`
- `default float skillCooldownMultiplier(CombatantView owner)`
- `default float ultimateGainMultiplier(CombatantView owner)`
- `default float incomingDamageMultiplier(CombatantView owner)`
- `default float hitboxMultiplier(CombatantView owner)`
- `default float stunMultiplier(CombatantView owner)`
- `default long additionalUltimateAttackCount(CombatantView owner)`
- `default void update(PassiveUpdateContext context)`
- `default void onSkillActivated(AbilityActivationContext context)`
- `default void onUltimateActivated(AbilityActivationContext context)`
- `default float ultimateDamageMultiplier(CombatantView owner)`
- `default float criticalRateModifier(CombatantView owner)`
- `default boolean ignoresDefense(CombatantView owner)`
- `default List<NormalAttackModifier> normalAttackModifiers(CombatantView owner)`
- `default List<AbilityStateLine> stateLines(CombatantView owner)`
- `default void onDamageDealt(DamageEventContext context)`
- `default void onDamageReceived(DamageEventContext context)`

### `PassiveUpdateContext`
**種類:** `record`  
**役割:** パッシブの時間更新に必要な所有者、操作、デルタ時間、累積経過時間を保持する。

**recordの構成要素:** `CombatantView owner`、`CombatActions actions`、`float deltaSeconds`、`float battleElapsedSeconds`

### `SkillRuntime`
**種類:** `interface`  
**役割:** スキル実装の発動契約。発動成否を返し、必殺技経由の発動も扱える。

**主な公開メソッド／コンストラクタ:**

- `default void activateFromUltimate(AbilityActivationContext context)`

### `SupportAbilityActivationContext`
**種類:** `class`  
**役割:** 補助能力の発動に必要な補助キャラ、所有者、対象、設定、状態保存領域などを渡す。

**主な公開メソッド／コンストラクタ:**

- `public SupportAbilityActivationContext(SupportView support, CombatantView owner, CombatantView target, CombatActions actions, ConfigView config, float battleElapsedSeconds)`
- `public SupportAbilityActivationContext(SupportView support, CombatantView owner, CombatantView target, CombatActions actions, ConfigView config, float battleElapsedSeconds, SupportActions supportActions, SupportStateStore state)`
- `public SupportView support()`
- `public CombatantView owner()`
- `public CombatantView target()`
- `public CombatActions actions()`
- `public ConfigView config()`
- `public float battleElapsedSeconds()`
- `public SupportActions supportActions()`
- `public SupportStateStore state()`

### `SupportAbilityRuntime`
**種類:** `interface`  
**役割:** 補助キャラクターのスキル・必殺技に使う実行インターフェース。

### `SupportActions`
**種類:** `interface`  
**役割:** 補助キャラに固有の戦闘操作（必殺ポイントの返却、分裂、戦場数依存の攻撃補正など）を提供する。

**主な公開メソッド／コンストラクタ:**

- `default void refundUltimatePoints(float points)`
- `default void splitTemporarily(SupportSplitSpec spec)`
- `default void setBattlefieldCountAttackMultiplier(ResourceId id, int priority, float baseMultiplier, float multiplierPerCombatant)`

### `SupportPassiveContext`
**種類:** `record`  
**役割:** 補助パッシブのイベントへ渡す補助キャラ情報、戦闘操作、専用状態、設定をまとめる。

**recordの構成要素:** `SupportView support`、`CombatantView owner`、`CombatantView target`、`CombatActions actions`、`SupportActions supportActions`、`SupportStateStore state`、`ConfigView config`、`float battleElapsedSeconds`

### `SupportPassiveRuntime`
**種類:** `interface`  
**役割:** 補助パッシブの開始・更新・攻撃・能力発動・終了イベントを受ける契約。

**主な公開メソッド／コンストラクタ:**

- `default void onBattleStart(SupportPassiveContext context)`
- `default void update(SupportPassiveContext context, float deltaSeconds)`
- `default void onSkillActivated(SupportPassiveContext context)`
- `default void onUltimateActivated(SupportPassiveContext context)`
- `default void onNormalAttack(SupportPassiveContext context, NormalAttackEvent event)`
- `default List<AbilityStateLine> stateLines(SupportPassiveContext context)`
- `default void onBattleEnd(SupportPassiveContext context)`

### `SupportSplitMemberSpec`
**種類:** `record`  
**役割:** 分裂によって生成する各補助メンバーの表示・攻撃力・間隔・移動速度の定義。

**recordの構成要素:** `String displayGlyph`、`ResourceId texture`、`float attackDamage`、`float attackIntervalSeconds`、`float moveSpeed`

### `SupportSplitSpec`
**種類:** `record`  
**役割:** 補助キャラの一時的な分裂構成、継続時間、共通標的、再結合条件を表す。

**recordの構成要素:** `List<SupportSplitMemberSpec> members`、`float durationSeconds`、`CombatantView commonTarget`、`boolean hideAndDisableOriginal`、`boolean recombineOnEnd`

### `SupportStateStore`
**種類:** `interface`  
**役割:** 補助能力間で保持するホスト管理の状態アクセス手段。

### `SupportView`
**種類:** `interface`  
**役割:** 補助キャラクターの参照専用情報を公開する。

**主な公開メソッド／コンストラクタ:**

- `default String instanceId()`

### `UltimateRuntime`
**種類:** `interface`  
**役割:** 必殺技の発動処理を定義する実行インターフェース。

---

## `jp.takoa.mojibattle.battle.*`

役割ごとの配置は次のとおりです。

- `action`: Abilityからホストへ副作用を要求する操作境界
- `action.spec`: 攻撃、投射物、召喚などの不変な操作仕様
- `combatant`: 戦場と戦闘個体の読取り専用ビュー
- `damage`: ダメージ要求・種別・確定結果
- `event`: Runtimeへ通知する戦闘イベント
- `modifier`: 通常攻撃へ合成する補正
- `presentation`: カットインと領域の視覚仕様
- `rule`: 決闘など一時的な戦闘ルール
- `status`: 状態効果の種類・期間・重複・消費規則

### `AreaAttackSpec`
**種類:** `record`  
**役割:** 範囲攻撃の遅延・半径・ダメージ・警告／着弾エフェクトなどの仕様。

**recordの構成要素:** `float delaySeconds`、`float radius`、`DamageRequest damage`、`ResourceId warningEffectId`、`ResourceId impactEffectId`、`ResourceId objectTexture`、`float effectScale`

### `BattlefieldPulseSpec`
**種類:** `record`  
**役割:** 戦場全体へ周期的に作用する吸引・ダメージ・エフェクトの仕様。

**recordの構成要素:** `float durationSeconds`、`float tickIntervalSeconds`、`float pullUnitsPerSecond`、`DamageRequest damagePerTick`、`ResourceId effectId`、`float effectScale`

### `BattlefieldView`
**種類:** `interface`  
**役割:** 現在の戦場に存在する戦闘者を参照するためのビュー。

### `CombatActions`
**種類:** `interface`  
**役割:** ダメージ、回復、状態異常、投射物、領域、移動などをホストに要求する戦闘操作API。

**主な公開メソッド／コンストラクタ:**

- `default BattlefieldView battlefield()`
- `default void damage(CombatantView source, CombatantView target, float amount, DamageType type)`
- `default DamageResult damage(CombatantView source, CombatantView target, DamageRequest request)`
- `default void healRatio(CombatantView target, float maximumHpRatio)`
- `default void addUltimatePoints(CombatantView target, float points)`
- `default void refundUltimatePoints(CombatantView target, float points)`
- `default void applyStatus(CombatantView source, CombatantView target, StatusSpec status)`
- `default void spawnDelayedStrike(CombatantView source, CombatantView target, DelayedStrikeSpec spec)`
- `default void spawnProjectile(CombatantView source, CombatantView target, ProjectileSpec spec)`
- `default void spawnProjectilePattern(CombatantView source, ProjectilePatternSpec spec)`
- `default void spawnAreaAttack(CombatantView source, CombatantView target, AreaAttackSpec spec)`
- `default void spawnFallingStrike(CombatantView source, FallingStrikeSpec spec)`
- `default void spawnLineAttack(CombatantView source, LineAttackSpec spec)`
- `default void summonCompanions(CombatantView source, CompanionSummonSpec spec)`
- `default void spawnMovingHazards(CombatantView source, MovingHazardSpec spec)`
- `default void spawnSpinningAttack(CombatantView source, SpinningAttackSpec spec)`
- `default void startSkillBarrage(CombatantView source, SkillBarrageSpec spec)`
- `default void startProjectileBarrage(CombatantView source, ProjectileBarrageSpec spec)`
- `default void detonateOwnedMarkers(CombatantView source, DetonationSpec spec)`
- `default boolean spawnDetonatableMarker(CombatantView source, ResourceId texture, int maximumOwned)`
- `default void stun(CombatantView target, float durationSeconds)`
- `default void bind(CombatantView target, float durationSeconds)`
- `default void pullToward(CombatantView target, CombatantView destination, float maximumDistance, float remainingDistance)`
- `default boolean teleportBesideTarget(CombatantView mover, float distance)`
- `default void halveAllSkillCooldowns()`
- `default void gatherMainCombatantsAtCenter(float centerScale)`
- `default float arenaHalfExtent()`
- `default long additionalUltimateAttackCount(CombatantView source)`
- `default List<CombatantView> enemiesOf(CombatantView source)`
- `default List<CombatantView> alliesOf(CombatantView source)`
- `default void startDuel(DuelSpec spec)`
- `default void lockNormalAttackTarget(CombatantView attacker, CombatantView target, float durationSeconds)`
- `default void knockbackAwayFrom(CombatantView source, CombatantView target, float distance)`
- `default void activateDomain(CombatantView source, DomainSpec spec)`
- `default boolean isDomainActive(CombatantView source, ResourceId domainId)`
- `default void showCutIn(CombatantView source, CutInSpec spec)`
- `default void startBattlefieldPulse(CombatantView source, BattlefieldPulseSpec spec)`
- `default void swapPositions(CombatantView first, CombatantView second)`
- `default void mirrorAcrossArenaCenter(CombatantView target)`
- `default void cleanseNegativeStatuses(CombatantView target)`

### `CombatantKind`
**種類:** `enum`  
**役割:** 戦闘者がメイン・補助・召喚などのどの分類かを表す列挙型。

**定義値:** `MAIN`, `SUPPORT`, `SUMMON`

### `CombatantPosition`
**種類:** `record`  
**役割:** 戦場の3次元座標を保持する値オブジェクト。

**recordの構成要素:** `float x`、`float y`、`float z`

**公開定数:** `ORIGIN`

### `CombatantView`
**種類:** `interface`  
**役割:** HP・位置・陣営・能力値など、戦闘者の読み取り専用情報を表す。

**主な公開メソッド／コンストラクタ:**

- `default String instanceId()`
- `default CombatantKind kind()`
- `default float maximumUltimatePoints()`
- `default float defenseMultiplier()`
- `default CombatantPosition position()`

### `CompanionSummonSpec`
**種類:** `record`  
**役割:** 随伴キャラクターの召喚内容を記述する仕様。

**recordの構成要素:** `int count`、`ResourceId texture`、`float maximumHp`、`float defense`、`float attackDamage`、`float attackInterval`、`float projectileSpeed`、`ResourceId impactEffectId`

### `CutInMotion`
**種類:** `enum`  
**役割:** カットイン演出の動きの種類を表す列挙型。

### `CutInSpec`
**種類:** `record`  
**役割:** 戦闘中のカットイン演出の表示内容・動作などを指定する。

**recordの構成要素:** `ResourceId image`、`String titleTranslationKey`、`float durationSeconds`、`int priority`、`CutInMotion motion`、`float intensity`

**主な公開メソッド／コンストラクタ:**

- `public CutInSpec(ResourceId image, String titleTranslationKey, float durationSeconds, int priority)`

### `DamageRequest`
**種類:** `record`  
**役割:** 構造化されたダメージ計算要求。ダメージ種別や追加条件を表す。

**recordの構成要素:** `float baseDamage`、`DamageType type`、`boolean ignoreDefense`、`boolean ignoreDefenseOnCritical`

**主な公開メソッド／コンストラクタ:**

- `public static DamageRequest of(float baseDamage, DamageType type)`

### `DamageResult`
**種類:** `record`  
**役割:** ダメージ処理の結果を返す値オブジェクト。

**recordの構成要素:** `float dealtDamage`、`boolean critical`、`boolean defenseIgnored`

### `DamageType`
**種類:** `enum`  
**役割:** 攻撃を通常攻撃・スキルなどに区分する列挙型。

**定義値:** `NORMAL`, `SKILL`, `ULTIMATE`

### `DelayedStrikeSpec`
**種類:** `record`  
**役割:** 時間差で発動する攻撃の発生条件・ダメージ・演出を定義する。

**recordの構成要素:** `float delaySeconds`、`DamageRequest damage`、`ResourceId effectId`、`ResourceId objectTexture`、`float effectScale`

### `DetonationSpec`
**種類:** `record`  
**役割:** 設置したマーカー等を起爆する際の攻撃仕様。

**recordの構成要素:** `DamageRequest damage`、`float radius`、`float warningSeconds`、`float effectSeconds`、`float incomingDamageMultiplier`、`float debuffDurationSeconds`、`ResourceId texture`、`ResourceId warningEffectId`、`ResourceId impactEffectId`

### `DomainSpec`
**種類:** `record`  
**役割:** 戦場に展開する領域の継続・攻撃・演出などを指定する。

**recordの構成要素:** `ResourceId id`、`float durationSeconds`、`DomainVisualStyle visualStyle`、`float intensity`、`ResourceId ambientEffectId`

### `DomainVisualStyle`
**種類:** `enum`  
**役割:** 領域演出の見た目の種類を表す列挙型。

**定義値:** `MONOCHROME`, `SUNRISE`, `BLOOM`, `ABYSS`

### `DuelSpec`
**種類:** `record`  
**役割:** 限定的な決闘状態に関する条件を表す。

**recordの構成要素:** `List<CombatantView> allies`、`CombatantView enemy`、`float durationSeconds`、`boolean restrictTargets`、`float participantDefenseMultiplier`、`float outsideAttackerDamageMultiplier`

### `FallingStrikeSpec`
**種類:** `record`  
**役割:** 落下攻撃の発生位置・タイミング・威力などを定義する。

**recordの構成要素:** `CombatantPosition position`、`float radius`、`DamageRequest damage`、`float warningSeconds`、`float effectSeconds`、`float spawnHeight`、`ResourceId warningEffectId`、`ResourceId impactEffectId`、`String dropStyle`、`ResourceId objectTexture`、`float horizontalOffset`、`float burnDuration`、`float burnDamage`、`float burnTickInterval`、`float burnUltimatePointsPerTick`、`float startDelaySeconds`

### `LineAttackSpec`
**種類:** `record`  
**役割:** 直線状に広がる攻撃の仕様を表す。

**recordの構成要素:** `float length`、`float width`、`DamageRequest damage`、`float warningSeconds`、`float effectSeconds`、`int directions`、`ResourceId warningEffectId`、`ResourceId impactEffectId`、`ResourceId objectTexture`

### `MovingHazardSpec`
**種類:** `record`  
**役割:** 戦場を移動する危険領域・ダメージ判定を定義する。

**recordの構成要素:** `int count`、`float speed`、`float radius`、`DamageRequest damage`、`float stunSeconds`、`float lifetimeSeconds`、`ResourceId texture`、`ResourceId impactEffectId`

### `NormalAttackEvent`
**種類:** `record`  
**役割:** 通常攻撃発生時に渡されるイベント情報。

**recordの構成要素:** `String attackerInstanceId`、`ResourceId characterId`、`Team team`、`AttackerType attackerType`

**主な公開メソッド／コンストラクタ:**

- `public AttackType attackType()`

### `NormalAttackModifier`
**種類:** `record`  
**役割:** メイン側の通常攻撃を追加補正する定義。

**recordの構成要素:** `ResourceId id`、`int priority`、`float attackIntervalMultiplier`、`int projectileCountDelta`、`float spreadDegrees`、`int hitsPerAttackDelta`、`float damageMultiplier`、`ResourceId projectileTexture`、`ResourceId effectId`

### `ProjectileBarrageSpec`
**種類:** `record`  
**役割:** 投射物による連射・弾幕攻撃の仕様。

**recordの構成要素:** `int count`、`float intervalSeconds`、`DamageRequest damage`、`float speed`、`float lifetimeSeconds`、`float radius`、`ResourceId texture`、`ResourceId impactEffectId`、`String enhancedNormalGlyph`、`float enhancedNormalScale`

### `ProjectilePatternSpec`
**種類:** `record`  
**役割:** 投射物の発射パターンと分布を指定する。

**recordの構成要素:** `List<ProjectileShotSpec> shots`、`float speed`、`float radius`、`float lifetimeSeconds`、`boolean radial`、`boolean boomerang`、`float boomerangDistance`、`float ultimatePointsPerHit`、`boolean pointsOnHit`、`String followUpGlyph`、`ResourceId followUpTexture`、`float followUpDamage`、`float followUpRadius`、`float followUpSpeed`、`ResourceId activationEffectId`、`ResourceId hitEffectId`、`ResourceId activationTexture`、`float activationEffectScale`、`FallingStrikeSpec fallingStrike`、`boolean ultimateAttack`

### `ProjectileShotSpec`
**種類:** `record`  
**役割:** 投射物1発分の攻撃情報を定義する。

**recordの構成要素:** `String glyph`、`ResourceId texture`、`float lateralOffset`、`float verticalOffset`、`float baseDamage`、`float stunSeconds`、`float bindSeconds`

### `ProjectileSpec`
**種類:** `record`  
**役割:** 単一の投射物の挙動やダメージ等を表す。

**recordの構成要素:** `DamageRequest damage`、`float speed`、`float radius`、`float lifetimeSeconds`、`int projectileCount`、`float spreadDegrees`、`int hitsPerProjectile`、`ResourceId textureId`、`ResourceId effectId`

### `SkillBarrageSpec`
**種類:** `record`  
**役割:** スキル発動を連続的に実施する弾幕・連撃の仕様。

**recordの構成要素:** `String skillId`、`int count`、`float intervalSeconds`

### `SpinningAttackSpec`
**種類:** `record`  
**役割:** 回転する攻撃オブジェクトの挙動・攻撃判定を指定する。

**recordの構成要素:** `float length`、`float width`、`DamageRequest damage`、`float healOnHit`、`float missingHealthBonus`、`float lifetimeSeconds`、`ResourceId texture`、`ResourceId impactEffectId`

### `StatusConsumption`
**種類:** `enum`  
**役割:** 状態異常の消費条件・消費挙動を表す。

**定義値:** `NONE`, `ON_DAMAGE_RECEIVED`

### `StatusDuration`
**種類:** `record`  
**役割:** 状態異常の継続時間・終了条件を表す。

**recordの構成要素:** `float seconds`、`boolean permanentFlag`

**主な公開メソッド／コンストラクタ:**

- `public static StatusDuration seconds(float seconds)`
- `public static StatusDuration permanent()`

### `StatusEffectType`
**種類:** `enum`  
**役割:** 状態異常の効果の種類を表す列挙型。

**定義値:** `ATTACK_MULTIPLIER`, `INCOMING_DAMAGE_MULTIPLIER`, `MOVE_SPEED_MULTIPLIER`, `CRITICAL_RATE_ADDITION`, `CRITICAL_DAMAGE_MULTIPLIER`, `ULTIMATE_GAIN_MULTIPLIER`, `SKILL_COOLDOWN_MULTIPLIER`, `NORMAL_ATTACK_INTERVAL_MULTIPLIER`, `DAMAGE_OVER_TIME`, `HEAL_OVER_TIME`, `NEXT_INCOMING_DAMAGE_MULTIPLIER`, `BONUS_DAMAGE_MULTIPLIER`

### `StatusSpec`
**種類:** `record`  
**役割:** 状態異常のID・効果量・持続・重複・消費条件をまとめる。

**recordの構成要素:** `ResourceId id`、`StatusEffectType type`、`float magnitude`、`StatusDuration duration`、`StatusStacking stacking`、`int maximumStacks`、`float tickIntervalSeconds`、`StatusConsumption consumption`

**主な公開メソッド／コンストラクタ:**

- `public StatusSpec(ResourceId id, StatusEffectType type, float magnitude, StatusDuration duration, StatusStacking stacking, int maximumStacks, float tickIntervalSeconds)`
- `public static StatusSpec multiplier(ResourceId id, StatusEffectType type, float magnitude, StatusDuration duration)`

### `StatusStacking`
**種類:** `enum`  
**役割:** 状態異常の重複時の扱いを表す列挙型。

### `SupportNormalAttackModifier`
**種類:** `record`  
**役割:** 補助キャラの通常攻撃に対する威力、間隔、弾数などの補正を表す。

**recordの構成要素:** `ResourceId id`、`int priority`、`Stacking stacking`、`float damageMultiplier`、`float intervalMultiplier`、`int projectileCount`、`int hitsPerAttack`、`float spreadAngle`、`ResourceId projectileTexture`、`ResourceId effect`

---

## `jp.takoa.mojibattle.content`

### `CharacterDataRegistration`
**種類:** `record`  
**役割:** メインキャラクターに紐づくスキル・必殺・パッシブ等のリソースを登録する。

**recordの構成要素:** `ResourceId character`、`ResourceId skill`、`ResourceId ultimate`、`ResourceId passive`、`String translationDirectory`

**主な公開メソッド／コンストラクタ:**

- `public String namespace()`
- `public ResourceId translation(String language)`

### `FieldEffectDataRegistration`
**種類:** `record`  
**役割:** フィールド効果のデータリソースを登録する。

**recordの構成要素:** `ResourceId fieldEffect`

**主な公開メソッド／コンストラクタ:**

- `public String namespace()`

### `SupportCharacterDataRegistration`
**種類:** `record`  
**役割:** 補助キャラクターデータおよび翻訳ディレクトリを登録する。

**recordの構成要素:** `ResourceId support`、`String translationDirectory`

**主な公開メソッド／コンストラクタ:**

- `public String namespace()`
- `public ResourceId translation(String language)`

---

## `jp.takoa.mojibattle.domain.ability`

### `AbilityConfigs`
**種類:** `class`  
**役割:** 代表的な能力設定の形式を内包するユーティリティ／定義コンテナ。

**内包される公開クラス:**

- `AbilityConfigs.ProjectileSkill`: 投射スキルの設定形式を定義する内包クラス。
- `AbilityConfigs.AreaUltimate`: 範囲型必殺技の設定形式を定義する内包クラス。
- `AbilityConfigs.TimedPassive`: 時間依存パッシブの設定形式を定義する内包クラス。

---

## `jp.takoa.mojibattle.event`

### `EventBus`
**種類:** `class`  
**役割:** イベントの購読・投稿・解除を管理する実装クラス。

**主な公開メソッド／コンストラクタ:**

- `public <E> EventSubscription subscribe(Class<E> eventType, Consumer<? super E> listener)`
- `public void close()`
- `public <E> void post(E event)`

### `EventDispatchException`
**種類:** `class`  
**役割:** イベントハンドラ実行に失敗したことを通知する例外。

**主な公開メソッド／コンストラクタ:**

- `public EventDispatchException(Class<?> eventType, RuntimeException cause)`

### `EventSubscription`
**種類:** `interface`  
**役割:** イベント購読の解除ハンドル。closeで解除する。

### `RegistrationBus`
**種類:** `interface`  
**役割:** MODに公開するレジストリ登録イベントの購読インターフェース。

**主な公開メソッド／コンストラクタ:**

- `default String ownerNamespace()`

---

## `jp.takoa.mojibattle.event.lifecycle`

### `RegisterEvent`
**種類:** `record`  
**役割:** あるレジストリの登録フェーズを通知するイベント。

---

## `jp.takoa.mojibattle.mod`

### `GameApiVersion`
**種類:** `class`  
**役割:** APIとゲームのバージョン定数を公開する。

**公開定数:** `API_MAJOR`, `API_VERSION`, `GAME_VERSION`

### `GameMod`
**種類:** `interface`  
**役割:** MODの登録と初期化のエントリーポイントインターフェース。

**主な公開メソッド／コンストラクタ:**

- `default String id()`
- `default void initialize(ModContext context)`

### `ModContext`
**種類:** `record`  
**役割:** MOD ID、登録イベントバス、リソースマネージャを束ねる。

**recordの構成要素:** `String modId`、`RegistrationBus registrationBus`、`ResourceManager resources`

### `ModEntry`
**種類:** `@interface`  
**役割:** GameMod実装クラスにMOD IDを付ける実行時アノテーション。

---

## `jp.takoa.mojibattle.registry`

### `AbilityPresentation`
**種類:** `record`  
**役割:** 能力説明を翻訳キーと引数の行リストで表す。

**recordの構成要素:** `List<Line> lines`

**主な公開メソッド／コンストラクタ:**

- `public static AbilityPresentation of(String key, Object... arguments)`
- `public AbilityPresentation append(String key, Object... arguments)`

### `CombatEffectDefinition`
**種類:** `record`  
**役割:** 戦闘エフェクトのテクスチャ、スケール、アニメーション、用途を定義する。

**recordの構成要素:** `String id`、`String texturePath`、`float scale`、`float duration`、`String animationType`、`float pulseSpeed`、`float rotationSpeed`、`Set<Usage> usages`、`int legacyIndex`

### `EffectIds`
**種類:** `class`  
**役割:** 標準エフェクトIDを集めた定数クラス。

**公開定数:** `NEUTRAL`

### `MojiBattleRegister`
**種類:** `class`  
**役割:** 登録宣言とイベント時の実登録を分離する遅延登録クラス。

**主な公開メソッド／コンストラクタ:**

- `public static <T> MojiBattleRegister<T> create(RegistryKey<T> registryKey, String namespace)`
- `public RegistryHandle<T> register(String path, Supplier<? extends T> factory)`
- `public void attach(RegistrationBus registrationBus)`
- `public RegistryKey<T> registryKey()`
- `public java.util.Collection<RegistryHandle<T>> entries()`

### `MojiBattleRegistries`
**種類:** `class`  
**役割:** APIに存在するレジストリキー群およびレジストリ管理への入口。

**公開定数:** `SKILL_IMPLEMENTATIONS`, `ULTIMATE_IMPLEMENTATIONS`, `PASSIVE_IMPLEMENTATIONS`, `FIELD_EFFECT_IMPLEMENTATIONS`, `SUPPORT_ABILITY_IMPLEMENTATIONS`, `SUPPORT_PASSIVE_IMPLEMENTATIONS`, `EFFECTS`, `CHARACTERS`, `SUPPORT_CHARACTERS`, `FIELD_EFFECTS`, `SKILL_DEFINITIONS`, `ULTIMATE_DEFINITIONS`, `PASSIVE_DEFINITIONS`

**主な公開メソッド／コンストラクタ:**

- `public MojiBattleRegistries(RegistryManager manager)`
- `public RegistryManager manager()`
- `public CombatEffectDefinition effectFromLegacyIndex(int index)`

### `Registry`
**種類:** `class`  
**役割:** IDをキーとしてオブジェクトを登録・検索・凍結する実体レジストリ。

**主な公開メソッド／コンストラクタ:**

- `public Registry(String kind, Function<T, String> idExtractor)`
- `public T register(T value)`
- `public T register(RegistryId id, T value)`
- `public T require(String id)`
- `public T get(String id)`
- `public void replace(String id, T value)`
- `public void removeIf(Predicate<T> predicate)`
- `public boolean contains(String id)`
- `public Collection<T> values()`
- `public void freeze()`
- `public boolean frozen()`
- `public RegistryState state()`

### `RegistryHandle`
**種類:** `class`  
**役割:** 遅延登録した要素を確定後に取得する参照ハンドル。

**主な公開メソッド／コンストラクタ:**

- `public RegistryId id()`
- `public boolean isBound()`
- `public T get()`

### `RegistryId`
**種類:** `record`  
**役割:** namespace:path 形式のレジストリIDを表す。

**recordの構成要素:** `String namespace`、`String path`

**公開定数:** `DEFAULT_NAMESPACE`

**主な公開メソッド／コンストラクタ:**

- `public static RegistryId parse(String value)`
- `public static RegistryId of(String namespace, String path)`
- `public String toString()`

### `RegistryKey`
**種類:** `record`  
**役割:** レジストリ自体のIDと登録する要素型を結びつける。

**主な公開メソッド／コンストラクタ:**

- `public static <T> RegistryKey<T> create(String id, Class<T> elementType)`

### `RegistryManager`
**種類:** `class`  
**役割:** 複数レジストリの生成・取得・登録イベント配信・凍結を管理する。

**主な公開メソッド／コンストラクタ:**

- `public <T> Registry<T> create(RegistryKey<T> key, String kind, Function<T, String> idExtractor)`
- `public void publishRegistrationEvents(EventBus lifecycleBus)`
- `public <T> Registry<T> require(RegistryKey<T> key)`
- `public void freezeAll()`
- `public boolean frozen()`
- `public boolean failed()`

### `RegistryState`
**種類:** `enum`  
**役割:** レジストリの状態を表す列挙型。

**定義値:** `DECLARING`, `REGISTERING`, `FROZEN`, `FAILED`

---

## `jp.takoa.mojibattle.registry.impl`

### `FieldEffectImplementation`
**種類:** `record`  
**役割:** フィールド効果の設定検証関数を登録する実装情報。

**recordの構成要素:** `String id`、`Consumer<ConfigView> validator`

### `PassiveImplementation`
**種類:** `record`  
**役割:** パッシブのRuntime生成、検証、説明表示の定義。

**recordの構成要素:** `String id`、`AbilityFactory<? extends PassiveRuntime> factory`、`Consumer<ConfigView> validator`、`Function<ConfigView`、`AbilityPresentation> presentation`

### `SkillImplementation`
**種類:** `record`  
**役割:** スキルのRuntime生成、検証、倍率付き説明表示の定義。

**recordの構成要素:** `String id`、`AbilityFactory<? extends SkillRuntime> factory`、`Consumer<ConfigView> validator`、`BiFunction<ConfigView`、`Float`、`AbilityPresentation> presentation`

### `SupportAbilityImplementation`
**種類:** `record`  
**役割:** 補助能力のRuntime生成と設定検証の定義。

**recordの構成要素:** `String id`、`AbilityFactory<? extends SupportAbilityRuntime> factory`、`Consumer<ConfigView> validator`

### `SupportPassiveImplementation`
**種類:** `record`  
**役割:** 補助パッシブのRuntime生成定義。

**recordの構成要素:** `String id`、`AbilityFactory<? extends SupportPassiveRuntime> factory`

### `UltimateImplementation`
**種類:** `record`  
**役割:** 必殺技のRuntime生成、検証、説明表示の定義。

**recordの構成要素:** `String id`、`AbilityFactory<? extends UltimateRuntime> factory`、`Consumer<ConfigView> validator`、`Function<ConfigView`、`AbilityPresentation> presentation`

---

## `jp.takoa.mojibattle.resource`

### `ClasspathResourceManager`
**種類:** `class`  
**役割:** クラスパスからMODリソースを開く ResourceManager の実装。

**主な公開メソッド／コンストラクタ:**

- `public ClasspathResourceManager(ClassLoader classLoader)`
- `public InputStream open(ResourceKind kind, ResourceId id) throws IOException`
- `public boolean exists(ResourceKind kind, ResourceId id)`
- `public List<ResourceId> list(ResourceKind kind, String namespace, String pathPrefix)`

### `ResourceId`
**種類:** `record`  
**役割:** リソースをnamespace:pathで識別する値オブジェクト。

**recordの構成要素:** `String namespace`、`String path`

**主な公開メソッド／コンストラクタ:**

- `public static ResourceId parse(String value)`
- `public String toString()`

### `ResourceKind`
**種類:** `enum`  
**役割:** リソースの分類と格納ディレクトリを定義する列挙型。

**定義値:** `ASSET`, `DATA`

**主な公開メソッド／コンストラクタ:**

- `public String directory()`

### `ResourceManager`
**種類:** `interface`  
**役割:** リソースを開く・存在確認・列挙する抽象インターフェース。

---

## 実装例: MOD登録とスキル

以下はAPIの型に合わせた最小例です。MODをJARからどのように探索するかはローダー側の実装に依存します。

```java
@ModEntry("examplemod")
public final class ExampleMod implements GameMod {
    @Override
    public void register(ModContext context) {
        ModSkills.REGISTRY.attach(context.registrationBus());
    }
}
```

```java
public final class ModSkills {
    public static final MojiBattleRegister<SkillImplementation> REGISTRY =
            MojiBattleRegister.create(MojiBattleRegistries.SKILL_IMPLEMENTATIONS, "examplemod");

    public static final RegistryHandle<SkillImplementation> RETURN_TO_FOREST =
            REGISTRY.register("return_to_forest", () -> new SkillImplementation(
                    "examplemod:return_to_forest",
                    config -> new ReturnToForestSkill(config),
                    config -> { },
                    (config, multiplier) -> AbilityPresentation.of(
                            "examplemod.skill.return_to_forest"
                    )
            ));

    private ModSkills() { }
}
```

上記の `ReturnToForestSkill` は `SkillRuntime` を実装し `ConfigView` を受け取るコンストラクタを持つ独自クラスを想定しています。

## 注意点

- `MojiBattleRegister` の `RegistryHandle.get()` は登録完了後に使用してください。
- `GameMod.id()` は `@ModEntry` を取得するため、アノテーションなしで呼ぶと例外になります（独自に `id()` を上書きする場合は別）。
- 失敗する可能性のある戦闘操作、設定値の妥当性、名前空間の一致はMOD開発側でも検証してください。
- 詳細な引数名・型は各クラスの公開シグネチャを確認してください。
