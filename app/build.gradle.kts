plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.colorr"
    compileSdk = 34

    buildFeatures {
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.example.colorr"
        minSdk = 24
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("com.getkeepsafe.taptargetview:taptargetview:1.13.3")
    implementation("com.getkeepsafe.taptargetview:taptargetview:1.12.0")
    implementation("com.getkeepsafe.taptargetview:taptargetview:1.13.0")
    implementation("com.github.Mirkoddd:Range-SeekBar:1.1.0")
    implementation("com.github.rtugeek:colorseekbar:1.7.7")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.viewbinding)
    implementation(libs.databinding.viewbinding)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
repositories {
    google()
    mavenCentral()
}