package jp.takoa.mojibattle.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import jp.takoa.mojibattle.GameContext;
import jp.takoa.mojibattle.domain.BattleController;
import jp.takoa.mojibattle.domain.BattleMode;
import jp.takoa.mojibattle.domain.CharacterDefinition;
import jp.takoa.mojibattle.domain.CharacterRuntime;
import jp.takoa.mojibattle.domain.PlayerId;
import jp.takoa.mojibattle.domain.TrainingStatistics;
import jp.takoa.mojibattle.domain.debug.CharacterDebugSnapshot;
import jp.takoa.mojibattle.domain.status.StatusEffect;
import jp.takoa.mojibattle.domain.support.SupportCharacterRuntime;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * 各プレイヤーのHP、スキル、必殺、補助能力および試合情報を重ねて表示するHUD。
 */
public final class BattleHud implements Disposable {
    private static final float ANNOUNCEMENT_TOTAL_SECONDS = 2.4f;

    private final GameContext context;
    private final Stage stage = new Stage(new ScreenViewport());
    private final EnumMap<PlayerId, PlayerHud> playerHuds = new EnumMap<>(PlayerId.class);
    private final Label timer;
    private final Label center;
    private final TrainingDebugHud trainingDebugHud;
    private final Image flash;
    private final boolean trainingMode;
    private float rainbowPhase;

    /**
     * 戦闘参加者ごとのHUD領域と、画面全体に重ねる通知領域を構築する。
     */
    public BattleHud(GameContext context, BattleController battle) {
        this.context = context;
        trainingMode = battle.mode() == BattleMode.TRAINING;

        flash = new Image(context.skin.newDrawable("bar-hp"));
        flash.setTouchable(Touchable.disabled);

        stage.addActor(flash);

        if (trainingMode) {
            trainingDebugHud = new TrainingDebugHud();
            stage.addActor(trainingDebugHud.root);

        } else {
            trainingDebugHud = null;
            Table grid = new Table();
            grid.setFillParent(true);
            grid.pad(8);

            List<PlayerId> ids = battle.playerIds();

            float scale = ids.size() > 2 ? .58f : .72f;

            for (int index = 0; index < ids.size(); index++) {
                PlayerId id = ids.get(index);
                PlayerHud hud = new PlayerHud(id, scale, battle.supports(id).size());
                playerHuds.put(id, hud);

                grid.add(hud.panel).expand().fill().pad(5);

                if (index % 2 == 1) {
                    grid.row();
                }
            }
            if (ids.size() == 3) {
                grid.add(new Table()).expand();
            }
            stage.addActor(grid);
        }

        timer = new Label("", context.skin);
        timer.setFontScale(.9f);

        Table clock = new Table();
        clock.setFillParent(true);
        clock.top();

        if (trainingMode) {
            clock.right();
        }

        clock.add(timer).pad(7);
        stage.addActor(clock);

        center = new Label("", context.skin, "effect-text");

        Table centerTable = new Table();
        centerTable.setFillParent(true);

        if (trainingMode) {
            centerTable.add().expand().fill();
            centerTable.add(center).expand().center();

        } else {
            centerTable.add(center);
        }
        stage.addActor(centerTable);
    }

    /**
     * 戦闘状態を全プレイヤー枠と中央通知へ反映する。
     */
    public void update(BattleController battle) {
        rainbowPhase = (rainbowPhase + 1.8f) % 360f;
        flash.setColor(.35f, .75f, 1, battle.visualPulse() * .14f);

        for (var entry : playerHuds.entrySet()) {
            entry.getValue().update(battle, battle.player(entry.getKey()));
        }

        timer.setText(context.i18n.text("battle.time_remaining", formatTime(battle.timer().remaining())));

        updateCenterAnnouncement(battle);

        if (trainingDebugHud != null) {
            trainingDebugHud.update(battle);
        }
    }

    private void updateCenterAnnouncement(BattleController battle) {
        if (battle.announcement() != null) {
            float remaining = battle.announcement().duration();
            float elapsed = ANNOUNCEMENT_TOTAL_SECONDS - remaining;
            float pulse = Math.abs(MathUtils.sin(elapsed * 10));

            center.setText(context.i18n.text("battle.ultimate_announcement", battle.announcement().player().label(), context.i18n.text(battle.announcement().i18nKey())));
            center.setFontScale(1.1f + pulse * .55f);
            center.setColor(1, .32f + pulse * .68f, .12f + (1 - pulse) * .88f, Math.min(1, Math.min(elapsed * 5, remaining * 3)));

            flash.setColor(1, .08f + .7f * pulse, .12f, .12f + pulse * .16f);

            return;
        }

        center.setFontScale(1);
        center.setColor(Color.WHITE);
        center.setText(battle.criticalPulse() > 0 ? context.i18n.text("battle.critical") : battle.finalMinuteStarted() ? context.i18n.text("battle.final_minute") : "");

        if (battle.criticalPulse() > 0) {
            center.setFontScale(1.45f);
            center.setColor(Color.GOLD);
        }
    }

    /**
     * 1プレイヤー分のメイン・補助ゲージを所有し、毎フレーム表示値を同期するHUD領域。
     */
    private final class PlayerHud {
        private final PlayerId id;
        private final Table panel = new Table();
        private final Table mainPanel = new Table();
        private final Table supportPanel = new Table();

        private final ContinuousImageGauge hp = gauge("gauge-hp");
        private final ContinuousImageGauge skill = gauge("gauge-skill");
        private final ContinuousImageGauge ultimate = gauge("gauge-ultimate");
        private final List<SupportHud> supportHuds = new ArrayList<>();

        private final Label title = label();
        private final Label hpText = label();
        private final Label skillReady = label();
        private final Label ultimateReady = label();
        private final Label details = label();

        private PlayerHud(PlayerId id, float scale, int supportCount) {
            this.id = id;
            mainPanel.setBackground(context.skin.getDrawable("panel"));
            mainPanel.pad(9);

            for (Label value : List.of(title, hpText, skillReady, ultimateReady, details)) {
                value.setFontScale(scale);
            }

            title.setColor(Color.WHITE);
            mainPanel.add(title).growX().left().row();

            hpText.setFontScale(scale * .95f);
            mainPanel.add(meter("battle.hp_gauge", hp, hpText, scale)).growX().padTop(5).row();
            mainPanel.add(meter("battle.ultimate_gauge", ultimate, ultimateReady, scale)).growX().padTop(4).row();
            mainPanel.add(meter("battle.skill_gauge", skill, skillReady, scale)).growX().padTop(4).row();
            details.setWrap(true);
            mainPanel.add(details).growX().left().padTop(3).row();

            float supportScale = supportCount >= 4 ? scale * .7f : supportCount == 3 ? scale * .78f : scale * .9f;
            for (int index = 0; index < supportCount; index++) {
                SupportHud hud = new SupportHud(supportScale);
                supportHuds.add(hud);
                supportPanel.add(hud.panel).growX().fillX().uniformX().pad(1);
            }

            panel.add(mainPanel).growX().top().row();
            panel.add().expandY().row();
            panel.add(supportPanel).growX().fillX().bottom();
        }

        private Table meter(String captionKey, ContinuousImageGauge gauge, Label state, float scale) {
            Table meter = new Table();

            Label caption = new Label(context.i18n.text(captionKey), context.skin);
            caption.setFontScale(scale * .9f);

            meter.add(caption).width(86).left();
            meter.add(gauge).growX().height(28).padLeft(5).padRight(7);
            meter.add(state).width(150).right();

            return meter;
        }

        private void update(BattleController battle, CharacterRuntime player) {
            List<SupportCharacterRuntime> supports = battle.supports(id);

            updateGaugeValues(player);
            updateName(player);
            updateMainAbilityStates(player);
            for (int index = 0; index < supportHuds.size(); index++) {
                SupportCharacterRuntime support = index < supports.size() ? supports.get(index) : null;
                supportHuds.get(index).update(support);
            }

            String fields = String.join(" / ", battle.fieldEffectIds().stream().map(key -> context.i18n.text("field." + key)).toList());
            details.setText(statusBadges(player) + "  " + context.i18n.text("battle.field_effects", fields));
        }

        private void updateGaugeValues(CharacterRuntime player) {
            hp.setValue(player.hpRatio());

            hpText.setText(context.i18n.text("battle.hp_value", Math.round(player.hp()), Math.round(player.maxHp())));

            skill.setValue(player.skillChargeRatio());
            ultimate.setValue(player.ultimatePoints() / 100f);
        }

        private void updateName(CharacterRuntime player) {
            String playerName = player.definition.displayName != null ? player.definition.displayName : context.i18n.text(player.definition.nameKey);
            String awakened = player.awakened() ? "  " + context.i18n.text("battle.awakened") : "";

            title.setText(id.label() + "  " + playerName + awakened);
        }

        private void updateMainAbilityStates(CharacterRuntime player) {

            if (player.definition.trainingDummy) {
                skill.setValue(0);
                ultimate.setValue(0);
                skillReady.setText("-");
                ultimateReady.setText("-");
                return;
            }

            setCooldownState(skillReady, player.skillReady(), player.skillCooldown(), "battle.skill_available", false);
            setUltimateState(ultimateReady, player.ultimatePoints(), "battle.ultimate_available");
        }

        private void setReady(Label label, boolean ready, String key) {
            label.setText(ready ? context.i18n.text(key) : "");
            if (ready) {
                label.setColor(new Color().fromHsv(rainbowPhase + id.ordinal() * 70, 1, 1));
            }
        }

        private void setCooldownState(Label label, boolean ready, float remaining, String readyKey, boolean automatic) {
            if (ready) {
                setReady(label, true, readyKey);
                return;
            }

            label.setColor(Color.WHITE);
            String messageKey = automatic ? "battle.auto_cooldown" : "battle.cooldown_seconds";
            label.setText(context.i18n.text(messageKey, String.format("%.1f", remaining)));
        }

        private void setUltimateState(Label label, float points, String readyKey) {
            if (points >= 100) {
                setReady(label, true, readyKey);
                return;
            }

            label.setColor(Color.WHITE);
            label.setText(context.i18n.text("battle.percent", Math.round(points)));
        }

        /** 1体分の補助能力ゲージを所有し、他の補助枠と同じ幅で横並びにする。 */
        private final class SupportHud {
            private final Table panel = new Table();
            private final ContinuousImageGauge skill = gauge("gauge-support-skill");
            private final ContinuousImageGauge ultimate = gauge("gauge-support-ultimate");
            private final Label title = label();
            private final Label skillState = label();
            private final Label ultimateState = label();

            private SupportHud(float scale) {
                panel.setBackground(context.skin.getDrawable("button-down"));
                panel.pad(2);
                title.setFontScale(scale * .9f);
                skillState.setFontScale(scale * .68f);
                ultimateState.setFontScale(scale * .68f);
                title.setWrap(false);
                skillState.setWrap(false);
                ultimateState.setWrap(false);
                title.setAlignment(com.badlogic.gdx.utils.Align.left);
                skillState.setAlignment(com.badlogic.gdx.utils.Align.right);
                ultimateState.setAlignment(com.badlogic.gdx.utils.Align.right);
                panel.add(title).growX().left().row();
                panel.add(supportMeter(
                                "battle.support_ultimate_gauge",
                                ultimate,
                                ultimateState,
                                scale))
                        .growX()
                        .padTop(1)
                        .row();
                panel.add(supportMeter(
                                "battle.support_skill_gauge",
                                skill,
                                skillState,
                                scale))
                        .growX()
                        .padTop(1);
            }

            private Table supportMeter(
                    String captionKey,
                    ContinuousImageGauge gauge,
                    Label state,
                    float scale) {
                Table meter = new Table();
                Label caption = new Label(context.i18n.text(captionKey), context.skin);
                caption.setFontScale(scale * .68f);
                meter.add(caption).width(34).left();
                meter.add(gauge).growX().height(13).padLeft(2).padRight(2);
                meter.add(state).width(54).right();
                return meter;
            }

            private void update(SupportCharacterRuntime support) {
                if (support == null) {
                    title.setText(context.i18n.text("battle.no_support"));
                    skill.setValue(0);
                    ultimate.setValue(0);
                    skillState.setText("");
                    ultimateState.setText("");
                    return;
                }

                String name = support.definition.displayName != null
                        ? support.definition.displayName
                        : context.i18n.text(support.definition.nameKey);
                title.setText(context.i18n.text("battle.support", name));
                skill.setValue(support.skillChargeRatio());
                ultimate.setValue(support.ultimateChargeRatio());
                setCooldownState(
                        skillState,
                        false,
                        support.skillCooldown(),
                        "battle.support_skill_available",
                        false);
                setUltimateState(
                        ultimateState,
                        support.ultimatePoints(),
                        "battle.support_ultimate_available");
            }
        }
    }

    /**
     * 訓練中の計測値と戦闘計算上の実効値を、左半面へ常時表示するパネル。
     */
    private final class TrainingDebugHud {
        private final Table root = new Table();
        private final Label content = new Label("", context.skin);

        private TrainingDebugHud() {
            root.setFillParent(true);
            root.left().top();

            Label heading = new Label(context.i18n.text("training.debug.title"), context.skin, "title");
            heading.setFontScale(.78f);
            content.setFontScale(.48f);
            content.setWrap(true);
            content.setAlignment(com.badlogic.gdx.utils.Align.topLeft);

            ScrollPane scroll = new ScrollPane(content);
            scroll.setFadeScrollBars(false);
            scroll.setScrollingDisabled(true, false);

            Table panel = new Table();
            panel.setBackground(context.skin.getDrawable("panel"));
            panel.pad(12);
            panel.add(heading).growX().left().padBottom(8).row();
            panel.add(scroll).grow();

            root.add(panel).width(Value.percentWidth(.5f, root)).growY();
            root.add().expandX().fillX();
        }

        private void update(BattleController battle) {
            StringBuilder text = new StringBuilder();

            appendDamage(text, battle.trainingStatistics());
            appendFields(text, battle);
            appendPassives(text, battle);
            appendCharacter(text, battle, PlayerId.ONE, "training.debug.self_title");
            appendCharacter(text, battle, PlayerId.TWO, "training.debug.dummy_title");
            text.append('\n').append(context.i18n.text("training.controls"));

            content.setText(text);
        }

        private void appendDamage(StringBuilder text, TrainingStatistics statistics) {
            text.append(context.i18n.text("training.debug.damage", format1(statistics.lastDamage()), format1(statistics.damage()), statistics.hits(), format1(statistics.dps()))).append("\n\n");
        }

        private void appendFields(StringBuilder text, BattleController battle) {
            text.append(context.i18n.text("training.debug.field_title")).append('\n');

            if (battle.fieldEffectIds().isEmpty()) {
                text.append(context.i18n.text("training.debug.none"));

            } else {
                for (String id : battle.fieldEffectIds()) {

                    text.append("• ").append(context.i18n.text("field." + id));
                    String detailKey = "field.description." + id;

                    if (context.i18n.contains(detailKey)) {
                        text.append(" — ").append(context.i18n.text(detailKey));
                    }
                    text.append('\n');
                }
            }
            text.append('\n');
        }

        private void appendPassives(StringBuilder text, BattleController battle) {
            CharacterRuntime player = battle.player(PlayerId.ONE);
            text.append(context.i18n.text("training.debug.passive_title")).append('\n');
            String[] passiveIds = player.definition.equippedPassiveIds();

            if (passiveIds.length == 0) {
                text.append(context.i18n.text("training.debug.none")).append('\n');
            }

            for (String passiveId : passiveIds) {
                CharacterDefinition source = passiveSource(passiveId);

                String nameKey = source == null ? null : "ability." + source.id + ".passive.name";
                String detailKey = source == null ? null : "guide.passive_effect." + source.id;
                String name = nameKey != null && context.i18n.contains(nameKey) ? context.i18n.text(nameKey) : passiveId;

                text.append("• ").append(name);
                if (detailKey != null && context.i18n.contains(detailKey)) {
                    text.append(" — ").append(context.i18n.text(detailKey));
                }

                text.append('\n');
            }
            CharacterDebugSnapshot snapshot = battle.debugSnapshot(PlayerId.ONE);
            text.append(context.i18n.text("training.debug.passive_multipliers", format2(snapshot.effectiveAttackMultiplier()), format2(snapshot.incomingDamageMultiplier()), format2(snapshot.effectiveHitboxMultiplier()), format2(snapshot.skillCooldownMultiplier()), format2(snapshot.ultimateGainMultiplier()), format2(snapshot.stunMultiplier()), format2(snapshot.ultimateDamageMultiplier()))).append("\n\n");
        }

        private CharacterDefinition passiveSource(String passiveId) {
            for (CharacterDefinition definition : context.characters.all()) {
                for (String equippedId : definition.equippedPassiveIds()) {

                    if (passiveId.equals(equippedId)) {
                        return definition;
                    }
                }
            }
            return null;
        }

        private void appendCharacter(StringBuilder text, BattleController battle, PlayerId playerId, String headingKey) {
            CharacterDebugSnapshot value = battle.debugSnapshot(playerId);

            text.append(context.i18n.text(headingKey)).append('\n');
            text.append(context.i18n.text("training.debug.character_stats", value.characterId(), format1(value.hp()), format1(value.maximumHp()), format1(value.missingHp()), flag(value.alive()), flag(value.awakened()), flag(value.zeroDefense()), format2(value.baseAttack()), format2(value.effectiveAttackMultiplier()), format2(value.baseMoveSpeed()), format2(value.effectiveMoveSpeed()), format2(value.incomingDamageMultiplier()), format2(value.collisionRadius()), format2(value.effectiveHitboxMultiplier()), format1(value.skillCooldown()), Math.round(value.skillChargeRatio() * 100), value.skillStocks(), value.maximumSkillStocks(), Math.round(value.ultimatePoints()), format1(value.totalDamage()), format1(value.stunRemaining()), flag(value.movementLocked()), format1(value.positionX()), format1(value.positionY()), format1(value.positionZ()), format2(value.facingX()), format2(value.facingZ()), flag(value.criticalEnabled()), format1(value.criticalRate() * 100), format2(value.criticalDamageMultiplier()))).append('\n');

            appendStatuses(text, battle.player(playerId));
            text.append('\n');
        }

        private void appendStatuses(StringBuilder text, CharacterRuntime player) {
            text.append(context.i18n.text("training.debug.status_title")).append(':').append('\n');
            List<StatusEffect> effects = player.statuses.active();

            if (effects.isEmpty()) {
                text.append("• ").append(context.i18n.text("training.debug.none")).append('\n');
                return;
            }

            for (StatusEffect effect : effects) {
                text.append("• ").append(context.i18n.text("training.debug.status_entry", effect.id, effect.type.name(), format2(effect.magnitude), effect.stacks, format1(effect.remaining), effect.source.label())).append('\n');
            }
        }

        private String flag(boolean value) {
            return context.i18n.text(value ? "training.debug.yes" : "training.debug.no");
        }
    }

    private ContinuousImageGauge gauge(String fillDrawableName) {
        return new ContinuousImageGauge(context.skin.newDrawable("gauge-empty"), context.skin.newDrawable(fillDrawableName));
    }

    /**
     * 背景画像へ塗り画像を重ね、割合に応じて塗り画像の横幅だけを変える連続矩形ゲージ。
     */
    private static final class ContinuousImageGauge extends Widget {
        private final Drawable background;
        private final Drawable fill;
        private float value;

        private ContinuousImageGauge(Drawable background, Drawable fill) {
            this.background = background;
            this.fill = fill;
            setTouchable(Touchable.disabled);
        }

        private void setValue(float value) {
            this.value = MathUtils.clamp(value, 0f, 1f);
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            validate();
            Color color = getColor();
            batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);

            background.draw(batch, getX(), getY(), getWidth(), getHeight());

            if (value > 0f) {
                fill.draw(batch, getX(), getY(), getWidth() * value, getHeight());
            }

            batch.setColor(Color.WHITE);
        }
    }

    private Label label() {
        return new Label("", context.skin);
    }

    private String statusBadges(CharacterRuntime player) {
        List<StatusEffect> activeEffects = player.statuses.active();

        if (activeEffects.isEmpty()) {
            return "";
        }

        StringBuilder value = new StringBuilder("[");

        for (StatusEffect effect : activeEffects) {
            if (value.length() > 1) {
                value.append(' ');
            }
            value.append(effect.type.name());
        }

        return value.append(']').toString();
    }

    private static String formatTime(float seconds) {
        int total = Math.max(0, (int) Math.ceil(seconds));
        return "%d:%02d".formatted(total / 60, total % 60);
    }

    private static String format1(float value) {
        return String.format("%.1f", value);
    }

    private static String format2(float value) {
        return String.format("%.2f", value);
    }

    public void draw(BattleController battle) {
        update(battle);
        stage.act();
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        flash.setBounds(trainingMode ? width / 2f : 0, 0, trainingMode ? width / 2f : width, height);
    }

    public Stage stage() {
        return stage;
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
