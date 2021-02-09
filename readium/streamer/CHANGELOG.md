# Changelog

All notable changes to this project will be documented in this file.

**Warning:** Features marked as *experimental* may change or be removed in a future release without notice. Use with caution.

## [Unreleased]

### Changed

* The HTTP server now requests that publication resources are not cached by browsers.
  * Caching poses a security risk for protected publications.

## [2.0.0-beta.1]

### Changed

* Upgraded to Kotlin 1.4.10.
* `Streamer` is now expecting a `PublicationAsset` instead of a `File`. You can create custom implementations of `PublicationAsset` to open a publication from different medium, such as a file, a remote URL, in-memory bytes, etc.
  * `FileAsset` can be used to replace `File` and provides the same behavior.


## [2.0.0-alpha.2]

### Added

* [Streamer API](https://readium.org/architecture/proposals/005-streamer-api) offers a simple interface to parse a publication and replace standalone parsers.
* A generic `ImageParser` for bitmap-based archives (CBZ or exploded directories) and single image files.
* A generic `AudioParser` for audio-based archives (Zipped Audio Book or exploded directories) and single audio files.

### Changed

* `Container` and `ContentFilters` were replaced by a shared implementation of a [`Fetcher`](https://readium.org/architecture/proposals/002-composite-fetcher-api).

### Fixed

* Readium can now open PDF documents of any size without crashing. However, LCP protected PDFs are still limited by the available memory.
* Various HTTP server fixes and optimizations.


## [2.0.0-alpha.1]

### Added

* Support for [Positions List](https://github.com/readium/architecture/tree/master/models/locators/positions) with EPUB, CBZ and PDF. Positions provide a list of discrete locations in a publication and can be used to implement an approximation of page numbers.
  * Get the visible position from the current `Locator` with `locations.position`.
  * The total number of positions can be retrieved with `publication.positions().size`. It is a suspending function because computing positions the first time can be expensive. 
* `ReadiumWebPubParser` to parse all Readium Web Publication profiles, including [Audiobooks](https://readium.org/webpub-manifest/extensions/audiobook.html), [LCP for Audiobooks](https://readium.org/lcp-specs/notes/lcp-for-audiobooks.html) and [LCP for PDF](https://readium.org/lcp-specs/notes/lcp-for-pdf.html). It parses both manifests and packages.
* (*Experimental*) `PDFParser` to parse single PDF documents.
  * The PDF parser is based on [PdfiumAndroid](https://github.com/barteksc/PdfiumAndroid/), which may increase the size of your apps. Please open an issue if this is a problem for you, as we are considering different solutions to fix this in a future release.

### Changed

* The CSS, JavaScript and fonts injection in the `Server` was refactored to reduce the risk of collisions and simplify your codebase.
  * **This is a breaking change**, [to upgrade your app you need to](https://github.com/readium/r2-testapp-kotlin/pull/321/files#diff-9bb6ad21df8b48f171ba6266616662ac):
    * Provide the application's `Context` when creating a `Server`.
    * Remove the following injection statements, which are now handled directly by the Streamer:
```kotlin
server.loadCustomResource(assets.open("scripts/crypto-sha256.js"), "crypto-sha256.js", Injectable.Script)   
server.loadCustomResource(assets.open("scripts/highlight.js"), "highlight.js", Injectable.Script)
```

### Fixed

* The EPUB parser underwent a significant refactoring to fix a number of issues (contributed by [@qnga](https://github.com/readium/r2-streamer-kotlin/pull/89))
  * [Metadata parsing was updated to follow our up-to-date specifications](https://github.com/readium/r2-streamer-kotlin/pull/102).
  * XML namespace prefixes are now properly supported, when an author chooses unusual ones.
  * Similarly, default vocabularies and prefixes for EPUB 3 Property Data Types are now properly handled.
* [`Server` was broken](https://github.com/readium/r2-testapp-kotlin/pull/306) when provided with publication filenames containing invalid characters.
* [EPUB publishers' default styles are not overriden by Readium CSS anymore](https://github.com/readium/r2-navigator-kotlin/issues/132).
* The `AndroidManifest.xml` is not forcing anymore `allowBackup` and `supportsRtl`, to let reading apps manage these features themselves (contributed by [@twaddington](https://github.com/readium/r2-streamer-kotlin/pull/93)).


[unreleased]: https://github.com/readium/r2-streamer-kotlin/compare/master...HEAD
[2.0.0-alpha.1]: https://github.com/readium/r2-streamer-kotlin/compare/1.1.5...2.0.0-alpha.1
[2.0.0-alpha.2]: https://github.com/readium/r2-streamer-kotlin/compare/2.0.0-alpha.1...2.0.0-alpha.2
[2.0.0-beta.1]: https://github.com/readium/r2-streamer-kotlin/compare/2.0.0-alpha.2...2.0.0-beta.1

