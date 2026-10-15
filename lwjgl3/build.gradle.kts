import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.tasks.Jar
import java.security.MessageDigest

plugins {
    application
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    implementation(project(":api"))
    implementation(project(":core"))
    implementation(project(":mod-loader"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:${property("gdxVersion")}")
    runtimeOnly("com.badlogicgames.gdx:gdx-platform:${property("gdxVersion")}:natives-desktop")
    runtimeOnly("com.badlogicgames.gdx:gdx-freetype-platform:${property("gdxVersion")}:natives-desktop")
}

sourceSets.main {
    resources.srcDir(rootProject.file("assets"))
}

application {
    mainClass.set("jp.takoa.mojibattle.lwjgl3.Lwjgl3Launcher")
    applicationName = "MojiBattleEX"
}

val standardModJar = project(":item").tasks.named<Jar>("jar")
val prepareDevMods = tasks.register<Sync>("prepareDevMods") {
    from(standardModJar)
    into(layout.buildDirectory.dir("dev-mods"))
}

tasks.named<Jar>("jar") {
    archiveFileName.set("MojiBattleEX.jar")
    manifest.attributes["Main-Class"] = application.mainClass.get()
}

tasks.named<JavaExec>("run") {
    dependsOn(prepareDevMods)
    workingDir = rootProject.file("assets")
    systemProperty("mojibattle.modsDir", layout.buildDirectory.dir("dev-mods").get().asFile.absolutePath)
}

val packageName = "MojiBattleEX"
val packageVendor = "takoa"
val packageDescription = "A loud local two-player 3D character battle game"
val packageVersion = project.version.toString()
val packageIcon = rootProject.layout.projectDirectory.file("packaging/windows/MojiBattleEX.ico")
val packageRoot = layout.buildDirectory.dir("jpackage")
val bundledModsDirectory = packageRoot.map { it.dir("bundled-mods") }
val installDistribution = tasks.named<Sync>("installDist")
val prepareBundledMods = tasks.register<Sync>("prepareBundledMods") {
    dependsOn(standardModJar)
    from(standardModJar)
    into(bundledModsDirectory)
}
val prepareJpackageInput = tasks.register<Sync>("prepareJpackageInput") {
    dependsOn(installDistribution)
    from(installDistribution.map { it.destinationDir.resolve("lib") })
    into(packageRoot.map { it.dir("input") })
}
val javaToolchains = extensions.getByType<JavaToolchainService>()
val jpackageExecutable = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(21))
}.map { launcher ->
    val executable = if (System.getProperty("os.name").lowercase().contains("windows")) "jpackage.exe" else "jpackage"
    launcher.metadata.installationPath.file("bin/$executable").asFile
}

fun jpackageBaseArguments(destination: File): List<String> {
    val arguments = mutableListOf(
        "--name", packageName,
        "--app-version", packageVersion,
        "--vendor", packageVendor,
        "--description", packageDescription,
        "--input", packageRoot.get().dir("input").asFile.absolutePath,
        "--main-jar", "MojiBattleEX.jar",
        "--main-class", application.mainClass.get(),
        "--dest", destination.absolutePath,
        "--java-options", "-Dfile.encoding=UTF-8",
        "--java-options", "-Dmojibattle.modsDir=\$APPDIR/mods",
        "--java-options", "-Xms256m",
        "--java-options", "-Xmx2g"
    )
    if (packageIcon.asFile.isFile) {
        arguments.addAll(listOf("--icon", packageIcon.asFile.absolutePath))
    }
    return arguments
}

val jpackageAppImage = tasks.register<Exec>("jpackageAppImage") {
    group = "distribution"
    description = "Creates a self-contained Windows application image with a bundled runtime and MODs."
    dependsOn(prepareJpackageInput, prepareBundledMods)
    doFirst {
        val destination = packageRoot.get().dir("app-image").asFile
        delete(destination.resolve(packageName))
        destination.mkdirs()
        commandLine(listOf(jpackageExecutable.get().absolutePath, "--type", "app-image") + jpackageBaseArguments(destination))
    }
    doLast {
        project.sync {
            from(bundledModsDirectory)
            into(packageRoot.get().dir("app-image/$packageName/app/mods"))
        }
    }
}

val jpackagePortableZip = tasks.register<Zip>("jpackagePortableZip") {
    group = "distribution"
    description = "Creates a portable Windows ZIP from the jpackage application image."
    dependsOn(jpackageAppImage)
    from(packageRoot.map { it.dir("app-image/$packageName") })
    into(packageName)
    destinationDirectory.set(packageRoot.map { it.dir("distributions") })
    archiveFileName.set("$packageName-$packageVersion-windows-x64.zip")
}

val jpackageWindowsExe = tasks.register<Exec>("jpackageWindowsExe") {
    group = "distribution"
    description = "Creates the Windows EXE installer. WiX Toolset must be installed and available on PATH."
    dependsOn(jpackageAppImage)
    doFirst {
        if (!System.getProperty("os.name").lowercase().contains("windows"))
            throw GradleException("The EXE installer can only be built on Windows.")
        val destination = packageRoot.get().dir("installer").asFile
        val temporary = packageRoot.get().dir("temp-exe").asFile
        delete(destination.resolve("$packageName-$packageVersion.exe"), temporary)
        destination.mkdirs()
        commandLine(listOf(
            jpackageExecutable.get().absolutePath,
            "--type", "exe",
            "--name", packageName,
            "--app-image", packageRoot.get().dir("app-image/$packageName").asFile.absolutePath,
            "--dest", destination.absolutePath,
            "--temp", temporary.absolutePath,
            "--win-menu",
            "--win-menu-group", packageName,
            "--win-shortcut",
            "--win-dir-chooser",
            "--win-per-user-install",
            "--win-upgrade-uuid", "f851cedf-2fff-4053-90eb-1cb7c31cc44b"
        ))
    }
}

val releaseChecksums = tasks.register("releaseChecksums") {
    group = "distribution"
    description = "Writes SHA-256 checksums for the portable ZIP and EXE installer."
    dependsOn(jpackagePortableZip, jpackageWindowsExe)
    doLast {
        val distributionDirectory = packageRoot.get().dir("distributions").asFile
        distributionDirectory.mkdirs()
        val artifacts = listOf(
            jpackagePortableZip.get().archiveFile.get().asFile,
            packageRoot.get().dir("installer").file("$packageName-$packageVersion.exe").asFile
        )
        val lines = artifacts.map { file ->
            require(file.isFile) { "Release artifact is missing: ${file.absolutePath}" }
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            val hash = digest.digest().joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
            "$hash  ${file.name}"
        }
        distributionDirectory.resolve("SHA256SUMS.txt").writeText(lines.joinToString(System.lineSeparator(), postfix = System.lineSeparator()))
    }
}

tasks.register("packageRelease") {
    group = "distribution"
    description = "Creates the portable ZIP, Windows EXE installer, and SHA-256 checksum file."
    dependsOn(releaseChecksums)
}
