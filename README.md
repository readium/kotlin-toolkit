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

## Using Readium

:question: **Find documentation and API reference at [readium.org/kotlin-toolkit](https://readium.org/kotlin-toolkit)**.

Readium modules are distributed through [JitPack](https://jitpack.io/#readium/kotlin-toolkit). It's also possible to clone the repository (or a fork) and depend on the modules locally.

### From the JitPack Maven repository

Make sure that you have the `$readium_version` property set in your root `build.gradle` and add the JitPack repository.

```groovy
buildscript {
    ext.readium_version = '2.2.0'
}

allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

Then, add the dependencies to the Readium modules you need in your app's `build.gradle`.

```groovy
dependencies {
    implementation "com.github.readium.kotlin-toolkit:readium-shared:$readium_version"
    implementation "com.github.readium.kotlin-toolkit:readium-streamer:$readium_version"
    implementation "com.github.readium.kotlin-toolkit:readium-navigator:$readium_version"
    implementation "com.github.readium.kotlin-toolkit:readium-opds:$readium_version"
    implementation "com.github.readium.kotlin-toolkit:readium-lcp:$readium_version"
}
```

### From a local Git clone

You may prefer to use a local Git clone if you want to contribute to Readium, or if you are using your own fork.

First, add the repository as a Git submodule of your app repository, then checkout the desired branch or tag:

```sh
git submodule add https://github.com/readium/kotlin-toolkit.git
```

Then, add the following to your project's `settings.gradle` file, altering the paths if needed. Keep only the modules you want to use.

```groovy
include ':readium:shared'
project(':readium:shared').projectDir = file('kotlin-toolkit/readium/shared')

include ':readium:streamer'
project(':readium:streamer').projectDir = file('kotlin-toolkit/readium/streamer')

include ':readium:navigator'
project(':readium:navigator').projectDir = file('kotlin-toolkit/readium/navigator')

include ':readium:opds'
project(':readium:opds').projectDir = file('kotlin-toolkit/readium/opds')

include ':readium:lcp'
project(':readium:lcp').projectDir = file('kotlin-toolkit/readium/lcp')
```

You should now see the Readium modules appear as part of your project. You can depend on them as you would on any other local module:

```
dependencies {
    implementation project(':readium:shared')
    implementation project(':readium:streamer')
    implementation project(':readium:navigator')
    implementation project(':readium:opds')
    implementation project(':readium:lcp')
}
```

### Building with Readium LCP

Using the toolkit with Readium LCP requires additional dependencies, including the binary `liblcp` provided by EDRLab. [Contact EDRLab](mailto:contact@edrlab.org) to request your private `liblcp` and the setup instructions.
