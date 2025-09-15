// Root build.gradle.kts (Gradle 6.7.1 / AGP 4.2.2 / Kotlin 1.5.31)
buildscript {
    val kotlin_version by extra("1.5.31")
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.2.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
    }
}

// Needed in Gradle 6.x so subprojects (like :app) can resolve dependencies
allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
