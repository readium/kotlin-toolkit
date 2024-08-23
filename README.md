# Readium Kotlin Toolkit

[Readium Mobile](https://github.com/readium/mobile) is a toolkit for ebooks, audiobooks and comics written in Swift & Kotlin.

This toolkit is a modular project, which follows the [Readium Architecture](https://github.com/readium/architecture). The different modules are found under [`readium/`](readium).

* [`shared`](readium/shared) – Shared `Publication` models and utilities
* [`streamer`](readium/streamer) – Publication parsers and local HTTP server
* [`navigator`](readium/navigator) – Plain `Fragment` and `Activity` classes rendering publications
* [`opds`](readium/opds) – Parsers for OPDS catalog feeds
* [`lcp`](readium/lcp) – Service and models for [Readium LCP](https://www.edrlab.org/readium-lcp/)
* [`adapters`](readium/adapters) – Adapters to use third-party libraries with Readium.
  * [`adapters/pdfium`](readium/adapters/pdfium) – Parse and render PDFs using the open source library [PdfiumAndroid](https://github.com/barteksc/PdfiumAndroid).
  * [`adapters/pspdfkit`](readium/adapters/pspdfkit) – Parse and render PDFs using the commercial library [PSPDFKit](https://pspdfkit.com/).

A [Test App](test-app) demonstrates how to integrate the Readium Kotlin toolkit in your own reading app.

:question: **Find documentation and API reference at [readium.org/kotlin-toolkit](https://readium.org/kotlin-toolkit)**.

## Minimum Requirements

| Readium | Android min SDK | Android compile SDK | Kotlin compiler | Gradle |
|---------|-----------------|---------------------|-----------------|--------|
| 2.4.3   | 21              | 33                  | 1.9.24          | 8.6.0  |
| 2.3.0   | 21              | 33                  | 1.7.10          | 6.9.3  |

## Setting Up Readium

Readium modules are distributed with [Maven Central](https://search.maven.org/search?q=g:org.readium.kotlin-toolkit). Make sure that you have the `$readium_version` property set in your root `build.gradle`, then add the Maven Central repository.

```groovy
buildscript {
    ext.readium_version = '2.4.3'
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
