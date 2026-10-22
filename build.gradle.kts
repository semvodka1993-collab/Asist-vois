plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "ru.example.voiceagent"
    compileSdk = 35
    defaultConfig {
        applicationId = "ru.example.voiceagent"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }
}
