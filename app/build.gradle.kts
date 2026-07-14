plugins {
    alias(libs.plugins.android.application)

    id(
        "com.google.android.libraries.mapsplatform.secrets-gradle-plugin"
    )
}

android {
    namespace = "com.nisa.hazentra"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.nisa.hazentra"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
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
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.android.volley:volley:1.2.1")
    implementation(
        "androidx.core:core-splashscreen:1.2.0"
    )

    implementation(
        "com.google.android.gms:play-services-maps:19.0.0"
    )

    implementation(
        "com.google.android.gms:play-services-location:21.3.0"
    )
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}