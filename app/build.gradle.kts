val keystorePath: String? = providers.gradleProperty("MYAPP_STORE_FILE").orNull
val keystorePassword: String? = providers.gradleProperty("MYAPP_STORE_PASSWORD").orNull
val keyAliasValue: String? = providers.gradleProperty("MYAPP_KEY_ALIAS").orNull
val keyPasswordValue: String? = providers.gradleProperty("MYAPP_KEY_PASSWORD").orNull

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.moodle"
    compileSdk = 30

    defaultConfig {
        applicationId = "com.example.moodle"
        minSdk = 30
        targetSdk = 30
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = false
    }

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
            isMinifyEnabled = true
        }
    }
}

dependencies {
    // Core UI (Views)
    implementation ("androidx.appcompat:appcompat:1.3.1")
    implementation ("com.google.android.material:material:1.4.0")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")

    // AndroidX core + activity/fragment
    implementation ("androidx.core:core-ktx:1.6.0")
    implementation ("androidx.activity:activity-ktx:1.3.1")
    implementation ("androidx.fragment:fragment-ktx:1.3.6")

   // Lifecycle (SDK 30–friendly)
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.3.1")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.3.1")
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.3.1")
    implementation ("androidx.savedstate:savedstate:1.1.0")

    // DataStore
    implementation ("androidx.datastore:datastore-preferences:1.0.0")

    // Networking (yours are fine)
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coroutines (keep, or pin to 1.6.4 if you want to be extra conservative)
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // (tests) pick older versions compatible with the above if needed
    testImplementation ("junit:junit:4.13.2")
    androidTestImplementation ("androidx.test.ext:junit:1.1.3")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.4.0")
}
