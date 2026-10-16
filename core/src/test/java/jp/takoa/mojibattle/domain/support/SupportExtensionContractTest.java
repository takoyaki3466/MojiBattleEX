package jp.takoa.mojibattle.domain.support;

import com.badlogic.gdx.math.Vector3;
import jp.takoa.mojibattle.ability.*;
import jp.takoa.mojibattle.battle.*;
import jp.takoa.mojibattle.domain.PlayerId;
import jp.takoa.mojibattle.resource.ResourceId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 0.7公開契約だけでdx・眉の要求を表現でき、個体状態が混線しないことを固定する。 */
final class SupportExtensionContractTest {
    private static final ResourceId DX_MULTI = ResourceId.parse("test:dx_multi");
    private static final ResourceId BROW_RUSH = ResourceId.parse("test:brow_rush");

    @Test void dxUltimatePermanentlyChangesOnlyItsOwnNormalAttack() {
        Fixture dx = fixture("test:dx", "p1:support:0");
        new DxPassive().onUltimateActivated(dx.context);
        var modifier = dx.actions.normalAttackModifiers().getFirst();
        assertEquals(DX_MULTI, modifier.id());
        assertEquals(3, modifier.hitsPerAttack());
        assertEquals(.6f, modifier.intervalMultiplier());
        assertTrue(fixture("test:dx", "p1:support:1").actions.normalAttackModifiers().isEmpty());
    }

    @Test void browCountsOwnAndEnemyAttackActionsCapsAt200AndKeepsInstancesSeparate() {
        Fixture first = fixture("test:brow", "p1:support:0");
        Fixture second = fixture("test:brow", "p1:support:1");
        BrowPassive passive = new BrowPassive();
        NormalAttackEvent own = new NormalAttackEvent(first.runtime.instanceId, ResourceId.parse("test:brow"), NormalAttackEvent.Team.ALLY, NormalAttackEvent.AttackerType.SUPPORT);
        NormalAttackEvent enemy = new NormalAttackEvent("p2:main:enemy", ResourceId.parse("test:enemy"), NormalAttackEvent.Team.ENEMY, NormalAttackEvent.AttackerType.MAIN);
        for (int i=0;i<250;i++) passive.onNormalAttack(first.context, i%2==0?own:enemy);
        assertEquals(200, first.runtime.state.getInt("browLevel",0));
        assertEquals(0, second.runtime.state.getInt("browLevel",0));
        passive.onSkillActivated(first.context);
        assertEquals(200, first.runtime.state.getInt("browLevel",0));
        assertEquals(BROW_RUSH, first.actions.normalAttackModifiers().getFirst().id());
        passive.onUltimateActivated(first.context);
        assertEquals(100, first.runtime.state.getInt("browLevel",0));
        assertEquals(200, first.combatActions.projectileHits);
    }

    @Test void stateStoreIsInvalidatedAtBattleEnd() {
        Fixture fixture = fixture("test:brow", "p1:support:0");
        fixture.runtime.state.putString("marker", "value");
        fixture.runtime.close();
        assertThrows(IllegalStateException.class, () -> fixture.runtime.state.getString("marker", ""));
    }

    private static Fixture fixture(String id, String instanceId) {
        SupportCharacterDefinition definition = new SupportCharacterDefinition();
        definition.id=id; definition.attackInterval=1; definition.ultimate=new SupportAbilityDefinition(); definition.ultimate.pointCost=100;
        SupportCharacterRuntime runtime = new SupportCharacterRuntime(PlayerId.ONE, definition, new Vector3(), instanceId);
        CoreSupportActions actions = new CoreSupportActions(runtime);
        SupportView support = new SupportView() {
            public String id(){return id;} public String instanceId(){return instanceId;}
            public float ultimatePoints(){return 0;} public float maximumUltimatePoints(){return 100;}
        };
        CombatantView owner = new CombatantView() {
            public String id(){return "test:owner";} public float hp(){return 100;} public float maximumHp(){return 100;}
            public float ultimatePoints(){return 0;} public boolean alive(){return true;}
        };
        RecordingActions combatActions = new RecordingActions();
        return new Fixture(runtime, actions, combatActions, new SupportPassiveContext(support,owner,owner,combatActions,actions,runtime.state,ConfigView.empty(),0));
    }

    private record Fixture(SupportCharacterRuntime runtime, CoreSupportActions actions, RecordingActions combatActions, SupportPassiveContext context) { }

    private static final class RecordingActions implements CombatActions {
        int projectileHits;
        public void damage(CombatantView source, CombatantView target, float amount) { }
        public void heal(CombatantView target, float amount) { }
        public void emitEffect(CombatantView origin, ResourceId effectId, float scale, float durationSeconds) { }
        @Override public void spawnProjectile(CombatantView source, CombatantView target, ProjectileSpec spec) {
            projectileHits += spec.projectileCount() * spec.hitsPerProjectile();
        }
    }

    private static final class DxPassive implements SupportPassiveRuntime {
        @Override public void onUltimateActivated(SupportPassiveContext c) {
            c.supportActions().addNormalAttackModifier(new SupportNormalAttackModifier(DX_MULTI,10,
                    SupportNormalAttackModifier.Stacking.REPLACE,1,.6f,1,3,0,null,null));
        }
    }

    private static final class BrowPassive implements SupportPassiveRuntime {
        @Override public void onNormalAttack(SupportPassiveContext c, NormalAttackEvent event) {
            if (event.team() == NormalAttackEvent.Team.ENEMY || event.attackerInstanceId().equals(c.support().instanceId())) add(c,1);
        }
        @Override public void onSkillActivated(SupportPassiveContext c) {
            add(c,20);
            c.supportActions().addNormalAttackModifier(new SupportNormalAttackModifier(BROW_RUSH,5,
                    SupportNormalAttackModifier.Stacking.MULTIPLY,1,.5f,1,1,1,null,null),3);
        }
        @Override public void onUltimateActivated(SupportPassiveContext c) {
            int level=c.state().getInt("browLevel",0);
            for (int remaining=level; remaining>0; remaining-=Math.min(64,remaining)) {
                int hits=Math.min(64,remaining);
                c.actions().spawnProjectile(c.owner(),c.target(),new ProjectileSpec(
                        DamageRequest.of(1,DamageType.ULTIMATE),12,.2f,2,1,0,hits,null,null));
            }
            c.state().putInt("browLevel",level/2);
        }
        @Override public java.util.List<AbilityStateLine> stateLines(SupportPassiveContext c) {
            return java.util.List.of(AbilityStateLine.of("support.brow.level",100,c.state().getInt("browLevel",0)));
        }
        private static void add(SupportPassiveContext c,int amount){ c.state().putInt("browLevel",Math.min(200,c.state().getInt("browLevel",0)+amount)); }
    }
}
