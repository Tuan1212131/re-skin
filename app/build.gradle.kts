plugins {
    id("com.android.application")
}

android {
    namespace = "com.re.skin"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.re.skin"
        minSdk = 23
        targetSdk = 28
        versionCode = 2
        versionName = "1.1"
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                arguments += listOf("-DANDROID_STL=c++_static")
            }
        }
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }

    externalNativeBuild {
        cmake { path = file("src/main/cpp/CMakeLists.txt"); version = "3.22.1" }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    ndkVersion = "27.2.12479018"
    buildFeatures { aidl = true }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.github.topjohnwu.libsu:core:5.2.2")
    implementation("com.github.topjohnwu.libsu:service:5.2.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
}

configurations.configureEach {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7-jvm")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8-jvm")
}
