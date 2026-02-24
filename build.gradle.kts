plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.opencode"
version = "0.1.0"

repositories {
    mavenCentral()
}

intellij {
    version.set("2024.1")
    type.set("PY")
    plugins.set(listOf("com.intellij.java"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks {
    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("251.*")
    }
}
