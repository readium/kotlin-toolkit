import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // FIXME: For now, we cannot use the versions catalog in precompiled scripts: https://github.com/gradle/gradle/issues/15383
    id("com.android.library")
    id("com.vanniktech.maven.publish")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.parcelize")
}

group = property("pom.groupId") as String

android {
    resourcePrefix = "readium_"

    compileSdk = (property("android.compileSdk") as String).toInt()

    defaultConfig {
        minSdk = (property("android.minSdk") as String).toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    buildFeatures {
        // FIXME: Look into whether we can remove this.
        buildConfig = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"))
        }
    }
}

kotlin {
    explicitApi()
    
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
        jvmTarget = JvmTarget.JVM_11
        allWarningsAsErrors = true
    }
}

dependencies {
    //noinspection UseTomlInstead
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}

mavenPublishing {
    coordinates(
        groupId = group.toString(),
        artifactId = property("pom.artifactId") as String,
        version = property("pom.version") as String
    )

    pom {
        name.set(property("pom.artifactId") as String)
        description.set("A toolkit for ebooks, audiobooks and comics written in Kotlin")
        url.set("https://github.com/readium/kotlin-toolkit")
        licenses {
            license {
                name.set("BSD-3-Clause license")
                url.set("https://github.com/readium/kotlin-toolkit/blob/main/LICENSE")
            }
        }
        developers {
            developer {
                id.set("aferditamuriqi")
                name.set("Aferdita Muriqi")
                email.set("aferdita.muriqi@gmail.com")
            }
            developer {
                id.set("mickael-menu")
                name.set("Mickaël Menu")
                email.set("mickael.menu@gmail.com")
            }
            developer {
                id.set("qnga")
                name.set("Quentin Gliosca")
                email.set("quentin.gliosca@gmail.com")
            }
        }
        scm {
            url.set("https://github.com/readium/kotlin-toolkit")
            connection.set("scm:git:github.com/readium/kotlin-toolkit.git")
            developerConnection.set("scm:git:ssh://github.com/readium/kotlin-toolkit.git")
        }
    }

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
}