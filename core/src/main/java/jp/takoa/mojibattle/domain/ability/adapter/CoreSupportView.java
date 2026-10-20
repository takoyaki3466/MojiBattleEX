package jp.takoa.mojibattle.domain.ability.adapter;

import jp.takoa.mojibattle.ability.support.SupportView;
import jp.takoa.mojibattle.domain.support.SupportCharacterRuntime;

/** SupportCharacterRuntimeを公開読取り契約へ射影する。 */
public final class CoreSupportView implements SupportView {
    private final SupportCharacterRuntime runtime;

    public CoreSupportView(SupportCharacterRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public String id() {
        return runtime.definition.id;
    }

    @Override public String instanceId() { return runtime.instanceId; }

    @Override
    public float ultimatePoints() {
        return runtime.ultimatePoints();
    }

    @Override
    public float maximumUltimatePoints() {
        return 100f;
    }
}
