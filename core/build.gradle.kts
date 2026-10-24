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
