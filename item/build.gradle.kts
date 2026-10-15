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
