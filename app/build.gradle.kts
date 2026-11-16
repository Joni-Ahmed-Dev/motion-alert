import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Push needs Firebase; the build works without it (push is simply skipped).
if (file("google-services.json").exists()) apply(plugin = "com.google.gms.google-services")

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun cfg(key: String): String = localProps.getProperty(key) ?: System.getenv(key) ?: ""

android {
    namespace = "com.joni.motionalert"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.joni.motionalert"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "RELAY_URL", "\"https://motion-alerts.aminuljoni180.workers.dev\"")
        buildConfigField("String", "MOTION_SECRET", "\"${cfg("MOTION_SECRET")}\"")
    }

    // Release signing: set KEYSTORE_PATH, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD (env or local.properties).
    // The keystore is PKCS12 (generated with openssl, no JDK in the sandbox), so storeType must be set.
    signingConfigs {
        if (cfg("KEYSTORE_PATH").isNotEmpty()) {
            create("release") {
                storeFile = file(cfg("KEYSTORE_PATH"))
                storePassword = cfg("KEYSTORE_PASSWORD")
                keyAlias = cfg("KEY_ALIAS")
                keyPassword = cfg("KEY_PASSWORD")
                storeType = "PKCS12"
            }
        }
    }

    buildTypes {
        release {
            // v1.0.0: R8 stays off. A minimal app does not need shrinking, and
            // it removes one whole class of first-build failures.
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-messaging")

    testImplementation("junit:junit:4.13.2")
}
