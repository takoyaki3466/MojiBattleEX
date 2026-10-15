import org.gradle.jvm.tasks.Jar

plugins {
    `java-library`
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    api(project(":api"))
    api(project(":core"))
}

sourceSets.main {
    resources.srcDir(rootProject.file("assets"))
}

tasks.named<Jar>("jar") {
    archiveFileName.set("MojiBattleEX-StandardContent-${project.version}.jar")
}
