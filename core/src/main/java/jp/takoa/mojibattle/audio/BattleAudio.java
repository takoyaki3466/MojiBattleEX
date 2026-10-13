package jp.takoa.mojibattle.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;

import java.util.EnumMap;
import java.util.List;

/** 生成済み戦闘効果音を読み込み、設定音量と同時発音数を考慮して再生する。 */
public final class BattleAudio implements Disposable {
    private static final String PREFERENCES_NAME = "mojibattle-user";
    private static final String AUDIO_DIRECTORY = "audio/sfx/";

    private final EnumMap<BattleSoundCue, Sound> sounds = new EnumMap<>(BattleSoundCue.class);
    private final float defaultMasterVolume;

    public BattleAudio(float defaultMasterVolume) {
        this.defaultMasterVolume = MathUtils.clamp(defaultMasterVolume, 0f, 1f);
        sounds.put(BattleSoundCue.PROJECTILE, load("projectile.wav"));
        sounds.put(BattleSoundCue.IMPACT, load("impact.wav"));
        sounds.put(BattleSoundCue.SLASH, load("slash.wav"));
        sounds.put(BattleSoundCue.EXPLOSION, load("explosion.wav"));
        sounds.put(BattleSoundCue.VEHICLE, load("vehicle.wav"));
        sounds.put(BattleSoundCue.SKILL, load("skill.wav"));
        sounds.put(BattleSoundCue.ULTIMATE, load("ultimate.wav"));
        sounds.put(BattleSoundCue.SUPPORT, load("support.wav"));
    }

    /** 1描画フレーム分の通知を再生し、大量同時攻撃では音割れしない数へ制限する。 */
    public void play(List<BattleSoundCue> cues) {
        if (cues.isEmpty()) {
            return;
        }
        Preferences preferences = Gdx.app.getPreferences(PREFERENCES_NAME);
        float master = MathUtils.clamp(
                preferences.getFloat("volume", defaultMasterVolume),
                0f,
                1f);
        if (master <= 0) {
            return;
        }

        EnumMap<BattleSoundCue, Integer> voices = new EnumMap<>(BattleSoundCue.class);
        for (BattleSoundCue cue : cues) {
            int voice = voices.getOrDefault(cue, 0);
            int maximumVoices = cue == BattleSoundCue.IMPACT || cue == BattleSoundCue.PROJECTILE
                    ? 3
                    : 2;
            if (voice >= maximumVoices) {
                continue;
            }
            voices.put(cue, voice + 1);
            float pitch = 1f + (voice - .5f) * .055f;
            sounds.get(cue).play(master * gain(cue), pitch, 0f);
        }
    }

    private static float gain(BattleSoundCue cue) {
        return switch (cue) {
            case PROJECTILE -> .34f;
            case IMPACT -> .52f;
            case SLASH -> .52f;
            case EXPLOSION -> .72f;
            case VEHICLE -> .58f;
            case SKILL -> .44f;
            case ULTIMATE -> .78f;
            case SUPPORT -> .46f;
        };
    }

    private static Sound load(String fileName) {
        return Gdx.audio.newSound(Gdx.files.internal(AUDIO_DIRECTORY + fileName));
    }

    @Override
    public void dispose() {
        sounds.values().forEach(Sound::dispose);
        sounds.clear();
    }
}
