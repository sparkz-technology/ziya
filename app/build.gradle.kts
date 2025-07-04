// plugins {
//     alias(libs.plugins.android.application)
//     alias(libs.plugins.kotlin.android)
// }

// android {
//     namespace = "com.example.ziya"
//     compileSdk = 35

//     defaultConfig {
//         applicationId = "com.example.ziya"
//         minSdk = 24
//         targetSdk = 34
//         versionCode = 1
//         versionName = "1.0"

//         testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//     }
//   signingConfigs {
//         release {
//             storeFile file("debug.keystore")  // Path to your keystore file
//             storePassword "Rithinika@07"
//             keyAlias "androidkey"
//             keyPassword "Rithinika@07"
//         }
//     }
    
//     buildTypes {
//         release {
//             isMinifyEnabled = false
//             proguardFiles(
//                 getDefaultProguardFile("proguard-android-optimize.txt"),
//                 "proguard-rules.pro"
//             )
//         }
//     }
//     compileOptions {
//         sourceCompatibility = JavaVersion.VERSION_11
//         targetCompatibility = JavaVersion.VERSION_11
//     }
//     kotlinOptions {
//         jvmTarget = "11"
//     }
// }

// dependencies {

//     implementation(libs.androidx.core.ktx)
//     implementation(libs.androidx.appcompat)
//     implementation(libs.material)
//     testImplementation(libs.junit)
//     androidTestImplementation(libs.androidx.junit)
//     androidTestImplementation(libs.androidx.espresso.core)
// }
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.ziya"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.ziya"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("debug.keystore") // Path to your keystore file
            storePassword = "Rithinika@07"
            keyAlias = "androidkey"
            keyPassword = "Rithinika@07"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
