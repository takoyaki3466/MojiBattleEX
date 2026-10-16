import org.gradle.api.file.DuplicatesStrategy
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
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.1")
}

sourceSets.main {
    resources.srcDir(rootProject.file("assets"))
}

application {
    mainClass.set("jp.takoa.mojibattle.lwjgl3.Lwjgl3Launcher")
    applicationName = "MojiBattleEX"
}

tasks.named<Jar>("jar") {
    archiveFileName.set("MojiBattleEX.jar")
    manifest.attributes["Main-Class"] = application.mainClass.get()
}

val standardItemJar = project(":item").tasks.named<Jar>("jar")
val developmentModsDirectory = layout.buildDirectory.dir("dev-mods")
val projectModsDirectory = rootProject.layout.projectDirectory.dir("mods")

/** 標準itemとプロジェクト直下の外部MODを、開発実行専用ディレクトリへ毎回同期する。 */
val prepareRunMods = tasks.register<Sync>("prepareRunMods") {
    group = "application"
    description = "Stages the standard item MOD and root mods/*.jar for lwjgl3:run."
    dependsOn(standardItemJar)
    from(standardItemJar.flatMap { it.archiveFile })
    from(projectModsDirectory) {
        include("*.jar")
    }
    into(developmentModsDirectory)
    duplicatesStrategy = DuplicatesStrategy.FAIL
}

tasks.named<JavaExec>("run") {
    dependsOn(prepareRunMods)
    workingDir = rootProject.file("assets")

    doFirst {
        systemProperty(
            "mojibattle.modsDir", developmentModsDirectory.get().asFile.absolutePath
        )
    }
}

val integrationTestStandardMod = project(":item").tasks.named<Jar>("jar")
tasks.test {
    useJUnitPlatform()
    dependsOn(integrationTestStandardMod)
    systemProperty(
        "mojibattle.test.standardModJar",
        integrationTestStandardMod.flatMap { it.archiveFile }.get().asFile.absolutePath
    )
    providers.gradleProperty("expansionModJar").orNull?.let {
        systemProperty("mojibattle.test.expansionModJar", project.file(it).absolutePath)
    }
}

val packageName = "MojiBattleEX"
val packageVendor = "takoa"
val packageDescription = "A loud local two-player 3D character battle game"
val packageVersion = project.version.toString()
val packageIcon = rootProject.layout.projectDirectory.file("packaging/windows/MojiBattleEX.ico")
val packageRoot = layout.buildDirectory.dir("jpackage")
val installDistribution = tasks.named<Sync>("installDist")
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
        "--name",
        packageName,
        "--app-version",
        packageVersion,
        "--vendor",
        packageVendor,
        "--description",
        packageDescription,
        "--input",
        packageRoot.get().dir("input").asFile.absolutePath,
        "--main-jar",
        "MojiBattleEX.jar",
        "--main-class",
        application.mainClass.get(),
        "--dest",
        destination.absolutePath,
        "--java-options",
        "-Dfile.encoding=UTF-8",
        "--java-options",
        "-Dmojibattle.modsDir=\$APPDIR/mods",
        "--java-options",
        "-Xms256m",
        "--java-options",
        "-Xmx2g"
    )
    if (packageIcon.asFile.isFile) {
        arguments.addAll(listOf("--icon", packageIcon.asFile.absolutePath))
    }
    return arguments
}

fun verifyApplicationImage(image: File) {
    val appDirectory = image.resolve("app")
    val config = appDirectory.resolve("$packageName.cfg")
    val configLines = config.readLines()
    val classpathEntries = configLines.filter { it.startsWith("app.classpath=") }
    val javaOptions = configLines.filter { it.startsWith("java-options=") }

    require(image.resolve("$packageName.exe").isFile) { "Application launcher is missing" }
    require(appDirectory.resolve("MojiBattleEX.jar").isFile) { "Main JAR is missing" }
    require(appDirectory.resolve("api-$packageVersion.jar").isFile) { "API JAR is missing" }
    require(appDirectory.resolve("core-$packageVersion.jar").isFile) { "Core JAR is missing" }
    require(appDirectory.resolve("mod-loader-$packageVersion.jar").isFile) { "MOD loader JAR is missing" }
    val modsDirectory = appDirectory.resolve("mods")

    require(modsDirectory.isDirectory) {
        "MOD directory is missing"
    }

    require(modsDirectory.listFiles()?.none { it.extension.equals("jar", ignoreCase = true) } == true) {
        "Core-only package must not contain bundled MOD JARs"
    }

    require(image.resolve("runtime/bin/server/jvm.dll").isFile) { "Bundled JVM is missing" }
    require(image.resolve("runtime/lib/modules").isFile) { "Bundled Java modules image is missing" }
    require(appDirectory.listFiles()?.any { it.name.matches(Regex("lwjgl-.*-natives-windows\\.jar")) } == true) {
        "Windows LWJGL native JAR is missing"
    }
    require(classpathEntries.none { it.contains("/mods/") || it.contains("\\mods\\") }) {
        "MOD JARs must not be placed on the application classpath"
    }
    require(javaOptions.contains("java-options=-Dfile.encoding=UTF-8")) { "UTF-8 JVM option is missing" }
    require(javaOptions.contains("java-options=-Dmojibattle.modsDir=\$APPDIR/mods")) {
        "Relocatable MOD directory JVM option is missing"
    }
    require(javaOptions.contains("java-options=-Xms256m") && javaOptions.contains("java-options=-Xmx2g")) {
        "Heap JVM options are missing"
    }
}

fun registerJpackageAppImage(
    taskName: String, outputDirectory: String, console: Boolean, taskDescription: String
) = tasks.register<Exec>(taskName) {
    group = "distribution"
    description = taskDescription
    dependsOn(prepareJpackageInput)
    doFirst {
        if (console && !System.getProperty("os.name").lowercase().contains("windows")) {
            throw GradleException("The debug app image with --win-console can only be built on Windows.")
        }
        val destination = packageRoot.get().dir(outputDirectory).asFile
        delete(destination.resolve(packageName))
        destination.mkdirs()
        val consoleArguments = if (console) listOf("--win-console") else emptyList()
        commandLine(
            listOf(
                jpackageExecutable.get().absolutePath, "--type", "app-image"
            ) + consoleArguments + jpackageBaseArguments(destination)
        )
    }
    doLast {
        val image = packageRoot.get().dir("$outputDirectory/$packageName").asFile

        val modsDirectory = image.resolve("app/mods")

        if (!modsDirectory.mkdirs() && !modsDirectory.isDirectory) {
            throw GradleException(
                "Failed to create MOD directory: ${modsDirectory.absolutePath}"
            )
        }

        verifyApplicationImage(image)
    }
}

val jpackageAppImage = registerJpackageAppImage(
    "jpackageAppImage",
    "app-image",
    false,
    "Creates a self-contained Windows application image with a bundled runtime and MODs."
)

val jpackageDebugAppImage = registerJpackageAppImage(
    "jpackageDebugAppImage",
    "debug-app-image",
    true,
    "Creates a Windows app image with a console for startup diagnostics."
)

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
        if (!System.getProperty("os.name").lowercase()
                .contains("windows")
        ) throw GradleException("The EXE installer can only be built on Windows.")
        val destination = packageRoot.get().dir("installer").asFile
        val temporary = packageRoot.get().dir("temp-exe").asFile
        delete(destination.resolve("$packageName-$packageVersion.exe"), temporary)
        destination.mkdirs()
        commandLine(
            listOf(
                jpackageExecutable.get().absolutePath,
                "--type",
                "exe",
                "--name",
                packageName,
                "--app-image",
                packageRoot.get().dir("app-image/$packageName").asFile.absolutePath,
                "--dest",
                destination.absolutePath,
                "--temp",
                temporary.absolutePath,
                "--win-menu",
                "--win-menu-group",
                packageName,
                "--win-shortcut",
                "--win-dir-chooser",
                "--win-per-user-install",
                "--win-upgrade-uuid",
                "f851cedf-2fff-4053-90eb-1cb7c31cc44b"
            )
        )
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
        distributionDirectory.resolve("SHA256SUMS.txt")
            .writeText(lines.joinToString(System.lineSeparator(), postfix = System.lineSeparator()))
    }
}

tasks.register("packageRelease") {
    group = "distribution"
    description = "Creates the portable ZIP, Windows EXE installer, and SHA-256 checksum file."
    dependsOn(releaseChecksums)
}
