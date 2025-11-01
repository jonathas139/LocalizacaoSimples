plugins {
    id("com.android.application")
    id("com.google.gms.google-services") // plugin do Firebase
    kotlin("android")
}

android {
    namespace = "com.example.localizacaosimples"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.localizacaosimples"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Firebase BoM gerencia versões automaticamente
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))

    // Firebase Realtime Database KTX para Kotlin
    implementation("com.google.firebase:firebase-database-ktx:20.3.1")

// Firebase Core (necessário para inicializar o Firebase)
    implementation("com.google.firebase:firebase-analytics-ktx:21.3.0")

    // Google Play Services - Localização
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
}
