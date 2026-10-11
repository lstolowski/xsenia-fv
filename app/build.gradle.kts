plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "pl.xsenia.fv"
    compileSdk = 37

    defaultConfig {
        applicationId = "pl.xsenia.fv"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    flavorDimensions += "environment"
    productFlavors {
        // Flavor names must not start with "test" (reserved by AGP for test source sets).
        create("ksefTest") {
            dimension = "environment"
            applicationIdSuffix = ".test"
            versionNameSuffix = "-test"
            buildConfigField("String", "KSEF_ENVIRONMENT", "\"TEST\"")
        }
        create("ksefProd") {
            dimension = "environment"
            buildConfigField("String", "KSEF_ENVIRONMENT", "\"PROD\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        // targetSdk 36 matches the only target device (Galaxy S23, Android 16) — see ADR 0002.
        disable += "OldTargetApi"
        warningsAsErrors = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":ksef"))
    implementation(project(":invoice"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
