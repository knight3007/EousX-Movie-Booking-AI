import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.google.services)
}

// 1. ĐOẠN CODE ĐỌC FILE LOCAL.PROPERTIES
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}
val tmdbApiKey = localProperties.getProperty("TMDB_API_KEY") ?: ""

android {
    namespace = "com.uit.eousx"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.uit.eousx"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 2. TRUYỀN KEY VÀO BUILDCONFIG
        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Jetpack Compose & Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.foundation)

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.play.services.auth)

    // Network (Retrofit, Moshi, OkHttp)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi.kotlin)

    // Local Database (Room)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Utilities
    implementation(libs.coil.compose)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    // Thư viện Lottie cho Animation
    implementation("com.airbnb.android:lottie-compose:6.3.0")

    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.core:core-splashscreen:1.0.1")

    testImplementation(libs.junit)
}

ksp {
    arg("dagger.hilt.android.internal.projectType", "app")
    arg("dagger.hilt.android.internal.disableAndroidSuperclassValidation", "true")
}
