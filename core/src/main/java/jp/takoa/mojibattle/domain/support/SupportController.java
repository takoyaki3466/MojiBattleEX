package jp.takoa.mojibattle.domain.support;

import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.PlayerTeam;
import jp.takoa.mojibattle.domain.combat.Projectile;
import jp.takoa.mojibattle.registry.Registry;
import jp.takoa.mojibattle.registry.impl.SupportAbilityImpl;
import jp.takoa.mojibattle.registry.EffectIds;
import jp.takoa.mojibattle.registry.impl.SupportPassiveImpl;
import jp.takoa.mojibattle.ability.context.SupportPassiveContext;
import jp.takoa.mojibattle.battle.event.NormalAttackEvent;
import jp.takoa.mojibattle.battle.modifier.SupportNormalAttackModifier;
import jp.takoa.mojibattle.domain.ability.adapter.CoreCombatantView;
import jp.takoa.mojibattle.domain.ability.adapter.CoreSupportView;
import jp.takoa.mojibattle.domain.ability.adapter.MapConfigView;
import jp.takoa.mojibattle.domain.ability.adapter.CoreCombatActions;

/**
 * 補助キャラクターの追従移動、自動通常攻撃および能力発動を制御する。
 */
public final class SupportController {
    private static final float ARRIVAL_TOLERANCE = .3f;
    private final SupportAbilityService abilities;
    private final Registry<SupportPassiveImpl> passives;

    public SupportController(Registry<SupportAbilityImpl> implementations) {
        this(implementations, new Registry<>("support passive implementation", SupportPassiveImpl::id));
    }

    public SupportController(Registry<SupportAbilityImpl> implementations, Registry<SupportPassiveImpl> passives) {
        abilities = new SupportAbilityService(implementations);
        this.passives = passives;
    }

    public void initialize(BattleController battle, PlayerTeam team, SupportCharacterRuntime support) {
        String id = support.definition.supportPassiveImplementationId;
        if (id == null) return;
        support.passive(java.util.Objects.requireNonNull(
                passives.require(id).factory().create(MapConfigView.of(support.definition.parameters)),
                "Support passive factory returned null: " + id));
        support.passive().onBattleStart(context(battle, team, support));
    }

    /**
     * 補助キャラクターを更新し、味方追従と射程内の自動攻撃を処理する。
     */
    public void update(
            BattleController battle,
            SupportCharacterRuntime support,
            CharacterRuntime ally,
            CharacterRuntime enemy,
            int supportIndex,
            int supportCount,
            float delta) {
        support.update(delta, battle.supportUltimateChargeMultiplier());
        if (support.hiddenAndDisabled() || !support.isAlive()) return;
        Object lockedTarget = battle.lockedNormalAttackTarget(support);
        if (lockedTarget instanceof CharacterRuntime lockedMain) enemy = lockedMain;
        if (support.passive() != null) {
            support.passive().update(context(battle, battle.team(support.owner), support), delta);
            support.passiveStateLines = java.util.List.copyOf(java.util.Objects.requireNonNull(
                    support.passive().stateLines(context(battle, battle.team(support.owner), support)), "support passive stateLines"));
        }

        if (support.skillReady()) {
            useSkill(battle, battle.team(support.owner), support);
        }

        float centeredIndex = supportIndex - (Math.max(1, supportCount) - 1) * .5f;
        float side = centeredIndex * support.definition.followDistance * .9f;
        float back = 2.7f + Math.abs(centeredIndex) * .35f;
        Vector3 desired = new Vector3(ally.position)
                .mulAdd(new Vector3(-ally.facing.z, 0, ally.facing.x), side)
                .mulAdd(ally.facing, -back);
        Vector3 movement = desired.sub(support.position);
        movement.y = 0;

        float distance = movement.len();

        if (distance > ARRIVAL_TOLERANCE) {
            float travelDistance = Math.min(support.definition.moveSpeed * battle.fieldMoveMultiplier(support) * delta, distance);
            support.position.mulAdd(movement.nor(), travelDistance);
        }

        support.position.y = ally.position.y;

        float attackRangeSquared = support.definition.attackRange * support.definition.attackRange;

        if (support.attackReady() && support.position.dst2(enemy.position) <= attackRangeSquared) {
            AttackProfile profile = profile(battle, support);
            Vector3 start = new Vector3(support.position).add(0, 1, 0);
            Vector3 velocity = new Vector3(enemy.position).add(0, 1, 0).sub(start).nor().scl(support.definition.projectileSpeed);
            battle.notifyNormalAttack(support);
            for (int projectileIndex=0; projectileIndex<profile.projectiles; projectileIndex++) {
                float angle = profile.projectiles == 1 ? 0 : -profile.spread/2 + profile.spread*projectileIndex/(profile.projectiles-1f);
                for (int hit=0; hit<profile.hits; hit++) {
                    Projectile shot = new Projectile(battle.nextAttackId(), support.owner, support.definition.displayGlyph, profile.texture, profile.effect, start, new Vector3(velocity).rotate(Vector3.Y, angle), .42f, support.definition.baseDamage*profile.damage, 0, 2.5f).criticalProfile(support.definition.criticalEnabled, support.definition.criticalRate, support.definition.criticalDamageMultiplier).sourceInstanceId(support.instanceId);
                    if (support.definition.curvedProjectile) shot.curve(24f);
                    battle.spawn(shot);
                }
            }
            support.resetAttack(support.definition.attackInterval*profile.interval);
        }
    }

    public void useSkill(BattleController battle, PlayerTeam team, SupportCharacterRuntime support) {
        if (abilities.useSkill(battle, team, support) && support.passive() != null) support.passive().onSkillActivated(context(battle, team, support));
    }

    public void useUltimate(BattleController battle, PlayerTeam team, SupportCharacterRuntime support) {
        if (abilities.useUltimate(battle, team, support) && support.passive() != null) support.passive().onUltimateActivated(context(battle, team, support));
    }

    public void normalAttack(BattleController battle, SupportCharacterRuntime observer, NormalAttackEvent event) {
        if (observer.passive() != null) observer.passive().onNormalAttack(context(battle, battle.team(observer.owner), observer), event);
    }

    public void close(BattleController battle, SupportCharacterRuntime support) {
        if (support.passive() != null) support.passive().onBattleEnd(context(battle, battle.team(support.owner), support));
        support.close();
    }

    private static SupportPassiveContext context(BattleController battle, PlayerTeam team, SupportCharacterRuntime support) {
        CharacterRuntime target=battle.targetOf(support.owner);
        return new SupportPassiveContext(new CoreSupportView(support),new CoreCombatantView(battle,team.main()),target==null?null:new CoreCombatantView(battle,target),new CoreCombatActions(battle),new CoreSupportActions(battle,support),support.state(),MapConfigView.of(support.definition.parameters),battle.elapsedSeconds());
    }

    private static AttackProfile profile(BattleController battle, SupportCharacterRuntime support) {
        float damage=1, interval=1, spread=0; int projectiles=1,hits=1; String texture=support.definition.projectileTexture,effect=EffectIds.WIND;
        int living = battle.livingBattleCombatants().size();
        damage *= support.countAttackMultiplier(living);
        var ordered=support.normalAttackModifiers().stream().sorted(java.util.Comparator.comparingInt(SupportNormalAttackModifier::priority).thenComparing(v->v.id().toString())).toList();
        for(var m:ordered){
            switch(m.stacking()){
                case ADD -> { damage+=m.damageMultiplier(); interval+=m.intervalMultiplier(); projectiles+=m.projectileCount(); hits+=m.hitsPerAttack(); spread+=m.spreadAngle(); }
                case MULTIPLY -> { damage*=m.damageMultiplier(); interval*=m.intervalMultiplier(); projectiles*=m.projectileCount(); hits*=m.hitsPerAttack(); spread*=m.spreadAngle(); }
                case REPLACE -> { damage=m.damageMultiplier(); interval=m.intervalMultiplier(); projectiles=m.projectileCount(); hits=m.hitsPerAttack(); spread=m.spreadAngle(); }
            }
            if(m.projectileTexture()!=null) texture=m.projectileTexture().toString();
            if(m.effect()!=null) effect=m.effect().toString();
        }
        return new AttackProfile(Math.max(0,damage),Math.max(.01f,interval),Math.max(1,projectiles),Math.max(1,hits),spread,texture,effect);
    }
    private record AttackProfile(float damage,float interval,int projectiles,int hits,float spread,String texture,String effect){}
}
