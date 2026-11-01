import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val uploadKeystoreBase64 = System.getenv("ANDROID_KEYSTORE_BASE64")
val releaseSigningEnabled = !uploadKeystoreBase64.isNullOrBlank()
val uploadKeystoreFile = layout.buildDirectory.file("upload-key.jks").get().asFile
if (releaseSigningEnabled) {
    uploadKeystoreFile.parentFile.mkdirs()
    uploadKeystoreFile.writeBytes(Base64.getDecoder().decode(uploadKeystoreBase64))
}

android {
    namespace = "com.magikalretrogrades.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.magikalretrogrades.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures { compose = true }

    signingConfigs {
        if (releaseSigningEnabled) create("release") {
            storeFile = uploadKeystoreFile
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("ANDROID_KEY_ALIAS")
            keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
        }
    }

    buildTypes {
        getByName("release") {
            if (releaseSigningEnabled) signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("io.github.cosinekitty:astronomy:2.1.19")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
