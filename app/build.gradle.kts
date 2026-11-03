plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.jarvis.hybrid"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.jarvis.hybrid"
        minSdk = 26
        targetSdk = 35
        versionCode = 15
        versionName = "2.0-alpha"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
