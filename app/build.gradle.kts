plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.pantilt"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.pantiltserial"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Dependencias básicas que ya trae Android Studio...
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // --- TUS NUEVAS DEPENDENCIAS ---
    implementation("com.github.mik3y:usb-serial-for-android:3.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
}