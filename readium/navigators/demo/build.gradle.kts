plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.parcelize")
    alias(libs.plugins.ksp)
}

android {
    namespace = "org.readium.navigator.demo"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.readium.navigator.demo"
        minSdk = 21
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
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.androidx.compose.compiler.get()
    }
    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes.add("META-INF/*")
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java")
            res.srcDirs("src/main/res")
            assets.srcDirs("src/main/assets")
        }
    }
}

dependencies {
    implementation(project(":readium:readium-shared"))
    implementation(project(":readium:readium-streamer"))
    implementation(project(":readium:readium-navigator"))
    implementation(project(":readium:navigators:readium-navigator-web"))
    implementation(project(":readium:navigators:readium-navigator-pdf"))
    implementation(project(":readium:adapters:pdfium"))

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.timber)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlin.stdlib)
    implementation(libs.androidx.legacy.v4)
    implementation(libs.bundles.compose)
    implementation(libs.androidx.core)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.fragment.compose)
    implementation(libs.androidx.appcompat)
}
