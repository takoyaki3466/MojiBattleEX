package jp.takoa.mojibattle.modloader;

import jp.takoa.mojibattle.mod.GameMod;

import java.nio.file.Path;

/** 依存解決後に生成されたMODエントリーポイント。 */
public record LoadedMod(ModMetadata metadata, Path jar, GameMod instance) {
}
