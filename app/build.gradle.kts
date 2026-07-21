plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.szn.merger"
    compileSdk {
        version = release(34)
    }

    defaultConfig {
        applicationId = "com.szn.merger"
        minSdk = 29
        targetSdk = 34
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
    implementation(libs.appcompat)
    implementation("com.android.tools.build:apksig:8.2.2")
    implementation(files("libs/apkeditor.jar"))
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.material)
    implementation("com.google.android.material:material:1.12.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("org.bouncycastle:bcprov-jdk18on:1.81")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.81")
}