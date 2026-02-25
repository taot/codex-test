plugins {
    id("java")
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.intellij") version "1.17.2"
}

group = "com.opencode"
version = "0.1.0"

repositories {
    mavenCentral()
}

intellij {
    type.set(providers.gradleProperty("platformType"))
    version.set(providers.gradleProperty("platformVersion"))
    plugins.set(listOf("com.intellij.java"))
}

dependencies {
    implementation(kotlin("stdlib"))
}

tasks {
    patchPluginXml {
        sinceBuild.set(providers.gradleProperty("pluginSinceBuild"))
        untilBuild.set(providers.gradleProperty("pluginUntilBuild"))
        changeNotes.set(
            """
            MVP implementation for Opencode PyCharm plugin with Cursor-like interactions and ACP connectivity.
            """.trimIndent()
        )
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
}
