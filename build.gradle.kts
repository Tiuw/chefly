// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false

    // Versi KSP wajib cocok dengan Kotlin 2.0.21 Anda
    id("com.google.devtools.ksp") version "2.0.21-1.0.27" apply false

    // Samakan versi Hilt dengan compiler di App level
    id("com.google.dagger.hilt.android") version "2.51" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
        // KSP and some Kotlin dev artifacts may be published to Kotlin's dev repo for early Kotlin releases
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
    }
    dependencies {
        // KSP Gradle plugin: try a KSP version compatible with Kotlin 2.0.x
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.0.0-1.0.12")
        // Dagger Hilt Gradle plugin
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.47")
    }
}