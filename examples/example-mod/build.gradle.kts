plugins {
    `java-library`
}

group = "com.example.mojibattle"
version = "1.0.0"

repositories {
    mavenLocal()
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    compileOnly("jp.takoa.mojibattle:mojibattle-api:0.1.0")
}
