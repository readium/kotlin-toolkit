# Readium Kotlin Toolkit

[Readium Mobile](https://github.com/readium/mobile) is a toolkit for ebooks, audiobooks and comics written in Swift & Kotlin.

:point_up: **Take a look at the [guide to quickly get started](docs/guides/getting-started.md).** A [Test App](test-app) demonstrates how to integrate the Readium Kotlin toolkit in your own reading app.

:question: **Find documentation and API reference at [readium.org/kotlin-toolkit](https://readium.org/kotlin-toolkit)**.

## Features

✅ Implemented &nbsp;&nbsp;&nbsp;&nbsp; 🚧 Partially implemented  &nbsp;&nbsp;&nbsp;&nbsp; 📆 Planned &nbsp;&nbsp;&nbsp;&nbsp; 👀 Want to do &nbsp;&nbsp;&nbsp;&nbsp; ❓ Not planned

### Formats

| Format | Status |
|---|:---:|
| EPUB 2 | ✅ |
| EPUB 3 | ✅ |
| Readium Web Publication | 🚧 |
| PDF | ✅ |
| Readium Audiobook | ✅ |
| Zipped Audiobook | ✅ |
| Standalone audio files (MP3, AAC, etc.) | ✅ |
| Readium Divina | 🚧 |
| CBZ (Comic Book ZIP) | 🚧 |
| CBR (Comic Book RAR) | ❓ |
| [DAISY](https://daisy.org/activities/standards/daisy/) | 👀 |

### Features

A number of features are implemented only for some publication formats.

| Feature | EPUB (reflow) | EPUB (FXL) | PDF |
|---|:---:|:---:|:---:|
| Pagination | ✅ | ✅ | ✅ |
| Scrolling | ✅ | 👀 | ✅ |
| Right-to-left (RTL) | ✅ | ✅ |  ✅ |
| Search in textual content | ✅ | ✅ | 👀 |
| Highlighting (Decoration API) | ✅ | ✅ | 👀 |
| Text-to-speech (TTS) | ✅ | ✅ | 👀 |
| Media overlays | 📆 | 📆 | |

### OPDS Support

| Feature | Status |
|---|:---:|
| [OPDS Catalog 1.2](https://specs.opds.io/opds-1.2) | ✅ | 
| [OPDS Catalog 2.0](https://drafts.opds.io/opds-2.0) | ✅ | 
| [Authentication for OPDS](https://drafts.opds.io/authentication-for-opds-1.0.html) | 📆 |
| [Readium LCP Automatic Key Retrieval](https://readium.org/lcp-specs/notes/lcp-key-retrieval.html) | 📆 |

### DRM Support

| Feature | Status |
|---|:---:|
| [Readium LCP](https://www.edrlab.org/projects/readium-lcp/) | ✅ |
| [Adobe ACS](https://www.adobe.com/fr/solutions/ebook/content-server.html) | ❓ |

## User Guides

Guides are available to help you make the most of the toolkit.

### Publication

* [Opening a publication](docs/guides/open-publication.md) – parse a publication package (EPUB, PDF, etc.) or manifest (RWPM) into Readium `Publication` models
* [Extracting the content of a publication](docs/guides/content.md) – API to extract the text content of a publication for searching or indexing it
* [Supporting PDF documents](docs/guides/pdf.md) – setup the PDF support in the toolkit
* [Text-to-speech](docs/guides/tts.md) – read aloud the content of a textual publication using speech synthesis
* [Accessibility](docs/guides/accessibility.md) – inspect and present accessibility metadata to users

### Navigator

* [Navigator](docs/guides/navigator/navigator.md) - an overview of the Navigator to render a `Publication`'s content to the user
* [Configuring the Navigator](docs/guides/navigator/preferences.md) – setup and render Navigator user preferences (font size, colors, etc.)
* [Font families in the EPUB navigator](docs/guides/navigator/epub-fonts.md) – support custom font families with reflowable EPUB publications
* [Media Navigator](docs/guides/navigator/media-navigator.md) – use the Media Navigator to read aloud a publication (audiobook, TTS, etc.)

### DRM

* [Supporting Readium LCP](docs/guides/lcp.md) – open and render LCP DRM protected publications

## Setting up the Readium Kotlin toolkit

### Minimum Requirements

| Readium   | Android min SDK | Android compile SDK | Kotlin compiler (✻) | Gradle (✻) |
|-----------|-----------------|---------------------|---------------------|------------|
| `develop` | 21              | 36                  | 2.1.21              | 8.14.1     |
| 3.1.0     | 21              | 35                  | 2.1.20              | 8.13       |
| 3.0.0     | 21              | 34                  | 1.9.24              | 8.6.0      |
| 2.3.0     | 21              | 33                  | 1.7.10              | 6.9.3      |

✻ Only required if you integrate Readium as a submodule instead of using Maven Central.

### Dependencies

Readium modules are distributed with [Maven Central](https://search.maven.org/search?q=g:org.readium.kotlin-toolkit). Make sure that you have the `$readium_version` property set in your root `build.gradle`, then add the Maven Central repository.

```groovy
buildscript {
    ext.readium_version = '3.1.1'
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

#### Using a local Git clone

You may prefer to use a local Git clone if you want to contribute to Readium, or if you are using your own fork.

First, add the repository as a Git submodule of your app repository, then checkout the desired branch or tag:

```sh
git submodule add https://github.com/readium/kotlin-toolkit.git
```

Then, include the Readium build to your project's `settings.gradle` file. The Readium dependencies will automatically build against the local sources.

```groovy
// Provide the path to the Git submodule.
includeBuild 'kotlin-toolkit'
```

:warning: When importing Readium locally, you will need to use the same version of the Android Gradle Plugin in your project.

### Building with Readium LCP

Using the toolkit with Readium LCP requires additional dependencies, including the binary `liblcp` provided by EDRLab. [Contact EDRLab](mailto:contact@edrlab.org) to request your private `liblcp` and the setup instructions.
