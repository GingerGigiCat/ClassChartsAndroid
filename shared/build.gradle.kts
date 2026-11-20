import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidLibrary {
        namespace = "com.gigi.cca.shared"
        androidResources.enable = true
        compileSdk {
            version = release(36) {
                minorApiLevel = 1
            }
        }
        minSdk = 26
    }

    // Desktop (Windows, macOS, Linux)
    jvm()

    // Web (Kotlin/Wasm). Needs the proxy in web-proxy/ because ClassCharts blocks browser requests.
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "classcharts"
        browser {
            commonWebpackConfig {
                outputFileName = "classcharts.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation("org.jetbrains.compose.material3:material3:1.9.0")
                implementation("org.jetbrains.androidx.navigation:navigation-compose:2.9.2")
                implementation("be.digitalia.compose.htmlconverter:htmlconverter:1.1.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                api("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1") // in the Repository API, used by the wear app
                implementation(libs.ktor.client.core)
                implementation("co.touchlab:kermit:2.0.4")
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.activity.compose)
                implementation(libs.ktor.client.okhttp)
            }
        }

        jvmMain {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.okhttp)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
            }
        }

        wasmJsMain {
            dependencies {
                implementation("io.ktor:ktor-client-js:${libs.versions.ktor.get()}")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.gigi.cca.shared.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.Msi, TargetFormat.Dmg)
            packageName = "ClassCharts"
            packageVersion = "1.0.0"
        }
    }
}
