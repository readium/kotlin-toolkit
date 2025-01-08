# Readium Kotlin Toolkit

[Readium Mobile](https://github.com/readium/mobile) is a toolkit for ebooks, audiobooks and comics written in Swift & Kotlin.

:point_up: **Take a look at the [guide to get started](docs/guides/getting-started.md).** A [Test App](test-app) demonstrates how to integrate the Readium Kotlin toolkit in your own reading app.

:question: **Find documentation and API reference at [readium.org/kotlin-toolkit](https://readium.org/kotlin-toolkit)**.

## Minimum Requirements

| Readium | Android min SDK | Android compile SDK | Kotlin compiler (✻) | Gradle (✻) |
|---------|-----------------|---------------------|---------------------|------------|
| 3.0.0   | 21              | 34                  | 1.9.24              | 8.6.0      |
| 2.3.0   | 21              | 33                  | 1.7.10              | 6.9.3      |

✻ Only required if you integrate Readium as a submodule instead of using Maven Central.

## Setting Up Readium

Readium modules are distributed with [Maven Central](https://search.maven.org/search?q=g:org.readium.kotlin-toolkit). Make sure that you have the `$readium_version` property set in your root `build.gradle`, then add the Maven Central repository.

```groovy
buildscript {
    ext.readium_version = '3.0.3'
}

allprojects {
    repositories {
        mavenCentral()
    }
}
```

Then, add the dependencies to the Readium modules you need in your app's `build.gradle`.

```groovy
dependencies {
    implementation "org.readium.kotlin-toolkit:readium-shared:$readium_version"
    implementation "org.readium.kotlin-toolkit:readium-streamer:$readium_version"
    implementation "org.readium.kotlin-toolkit:readium-navigator:$readium_version"
    implementation "org.readium.kotlin-toolkit:readium-opds:$readium_version"
    implementation "org.readium.kotlin-toolkit:readium-lcp:$readium_version"
}
```

:warning: If you target Android devices running below API 26, you must enable [core library desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring) in your application module.

### Using a local Git clone

You may prefer to use a local Git clone if you want to contribute to Readium, or if you are using your own fork.

First, add the repository as a Git submodule of your app repository, then checkout the desired branch or tag:

```sh
git submodule add https://github.com/readium/kotlin-toolkit.git
```

Make sure you have Jetifier enabled in your `gradle.properties` file:

```properties
android.enableJetifier=true
```

Then, include the Readium build to your project's `settings.gradle` file. The Readium dependencies will automatically build against the local sources.

```groovy
// Provide the path to the Git submodule.
includeBuild 'kotlin-toolkit'
```

:warning: When importing Readium locally, you will need to use the same version of the Android Gradle Plugin in your project.

### Building with Readium LCP

Using the toolkit with Readium LCP requires additional dependencies, including the binary `liblcp` provided by EDRLab. [Contact EDRLab](mailto:contact@edrlab.org) to request your private `liblcp` and the setup instructions.
