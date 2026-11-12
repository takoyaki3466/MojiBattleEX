plugins {
    `java-library`
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    api(project(":api"))
    api("com.badlogicgames.gdx:gdx:${property("gdxVersion")}")
    api("com.badlogicgames.gdx:gdx-freetype:${property("gdxVersion")}")
    implementation("com.badlogicgames.gdx-controllers:gdx-controllers-core:${property("gdxControllersVersion")}")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.1")
    testImplementation(project(":item"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.1")
}

tasks.test {
    useJUnitPlatform()
}

/**
 * ルートの version（例: v1.1-release-0）を、情報画面とクラッシュレポートが読む実行時リソースとして生成する。
 * バージョンの定義はルートの build.gradle.kts だけに置く。
 */
val gameVersionInfoDirectory = layout.buildDirectory.dir("generated/game-version")
val generateGameVersionInfo = tasks.register("generateGameVersionInfo") {
    val displayVersion = project.version.toString()
    val outputDirectory = gameVersionInfoDirectory
    inputs.property("version", displayVersion)
    outputs.dir(outputDirectory)
    doLast {
        val file = outputDirectory.get().file("META-INF/mojibattle/game-version.properties").asFile
        file.parentFile.mkdirs()
        file.writeText("version=$displayVersion\n", Charsets.UTF_8)
    }
}

sourceSets.main {
    resources.srcDir(generateGameVersionInfo)
}
