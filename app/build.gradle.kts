// :app build.gradle.kts (AGP 4.2.x expects "apply(plugin=…)", not catalogs/alias)

val keystorePath: String? = providers.gradleProperty("MYAPP_STORE_FILE").forUseAtConfigurationTime().orNull
val keystorePassword: String? = providers.gradleProperty("MYAPP_STORE_PASSWORD").forUseAtConfigurationTime().orNull
val keyAliasValue: String? = providers.gradleProperty("MYAPP_KEY_ALIAS").forUseAtConfigurationTime().orNull
val keyPasswordValue: String? = providers.gradleProperty("MYAPP_KEY_PASSWORD").forUseAtConfigurationTime().orNull

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
}

android {
    // AGP 4.2 uses *Version() methods in Kotlin DSL
    compileSdkVersion(26)

    defaultConfig {
        applicationId = "com.example.moodle"
        minSdkVersion(21)
        targetSdkVersion(26)
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }

    // No Compose in this toolchain; do not enable buildFeatures.compose
    signingConfigs {
        if (keystorePath != null && keystorePassword != null && keyAliasValue != null && keyPasswordValue != null) {
            create("release") {
                val path = keystorePath!!
                require(file(path).exists()) { "Keystore not found at $path" }
                storeFile = file(path)
                storePassword = keystorePassword!!
                keyAlias = keyAliasValue!!
                keyPassword = keyPasswordValue!!
            }
        }
    }
    buildTypes {
        getByName("release") {
            if (signingConfigs.findByName("release") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
        }
    }
}

dependencies {
    // === Legacy Support Libraries compatible with compileSdk/targetSdk 26 ===
    implementation("com.android.support:appcompat-v7:26.1.0")
    implementation("com.android.support:design:26.1.0")                // Material design widgets (pre-AndroidX)
    implementation("com.android.support.constraint:constraint-layout:1.1.3")

    // Fragments, v4 support (if you need it)
    implementation("com.android.support:support-v4:26.1.0")
    implementation("com.android.support:recyclerview-v7:26.1.0")

    // Architecture Components (pre-AndroidX)
    implementation("android.arch.lifecycle:runtime:1.1.1")
    implementation("android.arch.lifecycle:viewmodel:1.1.1")
    implementation("android.arch.lifecycle:livedata:1.1.1")
    // If you use @OnLifecycleEvent or need generated code:
    kapt("android.arch.lifecycle:compiler:1.1.1")

    // NOTE: Remove AndroidX KTX libs (core-ktx, activity-ktx, fragment-ktx) – not available pre-AndroidX
    // NOTE: Replace DataStore (AndroidX) with SharedPreferences on SDK 26

    // Networking (unchanged)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
     implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")

    // Coroutines compatible with Kotlin 1.5.31 / Java 8
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("com.android.support.test:runner:1.0.2")
    androidTestImplementation("com.android.support.test.espresso:espresso-core:3.0.2")
}
