plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("com.android.application")
}

kotlin {
    androidTarget()
    jvm("desktop")
    ios()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("androidx.activity:activity-compose:1.9.2")
            }
        }
        val desktopMain by getting
        val iosMain by getting
    }
}

android {
    namespace = "com.good4"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.good4"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

compose.desktop.application {
    mainClass = "com.good4.mainKt"
}


