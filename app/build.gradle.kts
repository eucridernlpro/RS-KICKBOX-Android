plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val rsSupabaseUrl = providers.gradleProperty("SUPABASE_URL").orElse("").get()
val rsSupabasePublishableKey = providers.gradleProperty("SUPABASE_PUBLISHABLE_KEY").orElse("").get()

android {
    namespace = "com.rskickbox.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rskickbox.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 52
        versionName = "0.52.0"
        buildConfigField("String", "SUPABASE_URL", "\""+rsSupabaseUrl.replace("\\","\\\\").replace("\"","\\\"")+"\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\""+rsSupabasePublishableKey.replace("\\","\\\\").replace("\"","\\\"")+"\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
    implementation("androidx.media3:media3-ui:1.11.1")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    implementation("com.google.zxing:core:3.5.4")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
