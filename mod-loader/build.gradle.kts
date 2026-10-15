plugins {
    `java-library`
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    api(project(":api"))
    implementation("com.badlogicgames.gdx:gdx:${property("gdxVersion")}")
}
