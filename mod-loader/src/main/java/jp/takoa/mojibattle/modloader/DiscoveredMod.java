package jp.takoa.mojibattle.modloader;

import java.nio.file.Path;

/** 検証前のMOD JARと、そこから読み出したmetadataの組。 */
public record DiscoveredMod(Path jar, ModMetadata metadata) {
}
