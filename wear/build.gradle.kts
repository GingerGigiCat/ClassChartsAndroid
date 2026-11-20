import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.gigi.classchartsandroid.wear"
    compileSdk = 36

    defaultConfig {
        // Same id as the phone app, which is what Wear OS expects for a paired watch app.
        applicationId = "com.gigi.classchartsandroid"
        minSdk = 30 // Wear OS 3
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

// 1.7 needs Android Gradle plugin 9.1 and compileSdk 37; 1.6 works with this project's AGP 8.13.
val wearCompose = "1.6.2"

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.wear.compose:compose-material3:$wearCompose")
    implementation("androidx.wear.compose:compose-foundation:$wearCompose")
    implementation("androidx.wear.compose:compose-navigation:$wearCompose")
}
