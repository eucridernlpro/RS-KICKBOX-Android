plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val rsSupabaseUrl = providers.gradleProperty("SUPABASE_URL").orElse("").get()
val rsSupabasePublishableKey = providers.gradleProperty("SUPABASE_PUBLISHABLE_KEY").orElse("").get()
val rsReleaseStoreFile = providers.gradleProperty("RS_RELEASE_STORE_FILE").orElse("").get()
val rsReleaseStorePassword = providers.gradleProperty("RS_RELEASE_STORE_PASSWORD").orElse("").get()
val rsReleaseKeyAlias = providers.gradleProperty("RS_RELEASE_KEY_ALIAS").orElse("").get()
val rsReleaseKeyPassword = providers.gradleProperty("RS_RELEASE_KEY_PASSWORD").orElse("").get()
val rsReleaseSigningReady = listOf(
    rsReleaseStoreFile,
    rsReleaseStorePassword,
    rsReleaseKeyAlias,
    rsReleaseKeyPassword
).all { it.isNotBlank() }

android {
    namespace = "com.rskickbox.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rskickbox.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 154
        versionName = "0.153.0"
        buildConfigField("String", "SUPABASE_URL", "\""+rsSupabaseUrl.replace("\\","\\\\").replace("\"","\\\"")+"\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\""+rsSupabasePublishableKey.replace("\\","\\\\").replace("\"","\\\"")+"\"")
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file(System.getProperty("user.home")+"/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (rsReleaseSigningReady) {
            create("release") {
                storeFile = file(rsReleaseStoreFile)
                storePassword = rsReleaseStorePassword
                keyAlias = rsReleaseKeyAlias
                keyPassword = rsReleaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isMinifyEnabled = false
            if (rsReleaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.04.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.media3:media3-transformer:1.11.1")
    implementation("androidx.media3:media3-effect:1.11.1")
    implementation("androidx.media3:media3-common:1.11.1")
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-session:1.11.1")
    implementation("androidx.media3:media3-ui:1.11.1")
    implementation("io.getstream:stream-webrtc-android:1.3.10")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    implementation("com.google.zxing:core:3.5.4")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation(platform("io.github.jan-tennert.supabase:bom:3.8.0"))
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:functions-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.ktor:ktor-client-android:3.5.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
