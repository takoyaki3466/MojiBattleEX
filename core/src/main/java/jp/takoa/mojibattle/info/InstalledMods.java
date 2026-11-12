package jp.takoa.mojibattle.info;

import java.util.List;

/**
 * 起動時に読み込んだMODの一覧を保持する。ランチャーがMODの探索後に設定し、情報画面が参照する。
 *
 * <p>どのスレッドからも読めるよう、変更不可の一覧を volatile で保持する。</p>
 */
public final class InstalledMods {
    private static volatile List<InstalledMod> mods = List.of();

    private InstalledMods() {
    }

    /** 読込み順の一覧を設定する。nullは空として扱う。 */
    public static void set(List<InstalledMod> installed) {
        mods = installed == null ? List.of() : List.copyOf(installed);
    }

    /** 読込み順の一覧。 */
    public static List<InstalledMod> all() {
        return mods;
    }

    /** フォントへ収録するため、表示に使う文字（名前、ID、バージョン、ファイル名）を連結して返す。 */
    public static String displayCharacters() {
        StringBuilder text = new StringBuilder();
        for (InstalledMod mod : mods) {
            text.append(mod.name()).append(mod.id()).append(mod.version()).append(mod.file());
        }
        return text.toString();
    }
}
