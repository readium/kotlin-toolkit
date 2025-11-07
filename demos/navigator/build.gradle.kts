import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.parcelize")
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
}

android {
    compileSdk = (property("android.compileSdk") as String).toInt()

    defaultConfig {
        minSdk = (property("android.minSdk") as String).toInt()
        targetSdk = (property("android.targetSdk") as String).toInt()

        applicationId = "org.readium.navigator.demo"

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
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
    namespace = "org.readium.demo.navigator"
}

kotlin {

    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
}

dependencies {
    implementation(project(":readium:readium-shared"))
    implementation(project(":readium:readium-streamer"))
    implementation(project(":readium:readium-navigator"))
    implementation(project(":readium:navigators:web:readium-navigator-web-reflowable"))
    implementation(project(":readium:navigators:web:readium-navigator-web-fixedlayout"))
    implementation(project(":readium:adapters:pdfium"))

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.timber)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlin.stdlib)
    implementation(libs.bundles.compose)
    implementation(libs.google.material)
    implementation(libs.androidx.core)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.browser)
}
