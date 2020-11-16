# Changelog

All notable changes to this project will be documented in this file.

**Warning:** Features marked as *experimental* may change or be removed in a future release without notice. Use with
caution.

## [Unreleased]

### Changed

* Upgraded to Kotlin 1.4.10.

### Fixed

* EPUBs declaring multiple languages were laid out from right to left if the first language had an RTL reading
progression. Now if no reading progression is set, the `effectiveReadingProgression` will be LTR.
* [#152](https://github.com/readium/r2-navigator-kotlin/issues/152) Panning through a zoomed-in fixed layout EPUB (contributed by [@johanpoirier](https://github.com/readium/r2-navigator-kotlin/pull/172)).


## [2.0.0-alpha.2]

### Added

* Support for the new `Publication` model using the [Content Protection](https://readium.org/architecture/proposals/006-content-protection) for DRM rights and the [Fetcher](https://readium.org/architecture/proposals/002-composite-fetcher-api) for resource access.
* (*Experimental*) New `Fragment` implementations as an alternative to the legacy `Activity` ones (contributed by [@johanpoirier](https://github.com/readium/r2-navigator-kotlin/pull/148)).
  * The fragments are chromeless, to let you customize the reading UX.
  * To create the fragments use the matching factory such as `EpubNavigatorFragment.createFactory()`, as showcased in `R2EpubActivity`.
  * At the moment, highlights and TTS are not yet supported in the new EPUB navigator `Fragment`.
  * [This is now the recommended way to integrate Readium](https://github.com/readium/r2-navigator-kotlin/issues/115) in your applications.

### Changed

* `currentLocator` is now a `StateFlow` instead of `LiveData`, to better support chromeless navigators such as an audiobook navigator.
  * If you were observing `currentLocator` in a UI context, you can continue to do so with `currentLocator.asLiveData()`.
* Improvements to the PDF navigator:
  * The navigator doesn't require PDF publications to be served from an HTTP server anymore. A side effect is that the navigator is now able to open larger PDF files.
  * `PdfNavigatorFragment.Listener::onResourceLoadFailed()` can be used to report fatal errors to the user, such as when trying to open a PDF document that is too large for the available memory.
  * A dedicated `PdfNavigatorFragment.createFactory()` was added, which deprecates the use of `NavigatorFragmentFactory`.

### Fixed

* Prevent switching to the next resource by mistake when scrolling through an EPUB resource in scroll mode.


## [2.0.0-alpha.1]

### Added

* The [position](https://github.com/readium/architecture/tree/master/models/locators/positions) is now reported in the locators for EPUB, CBZ and PDF.
* (*Experimental*) [PDF navigator](https://github.com/readium/r2-navigator-kotlin/pull/130).
  * Supports both single PDF and LCP protected PDF.
  * As a proof of concept, [it is implemented using `Fragment` instead of `Activity`](https://github.com/readium/r2-navigator-kotlin/issues/115). `R2PdfActivity` showcases how to use the `PdfNavigatorFragment` with the new `NavigatorFragmentFactory`.
  * The navigator is based on [AndroidPdfViewer](https://github.com/barteksc/AndroidPdfViewer), which may increase the size of your apps. Please open an issue if this is a problem for you, as we are considering different solutions to fix this in a future release.

### Changed

* [Upgraded to Readium CSS 1.0.0-beta.1.](https://github.com/readium/r2-navigator-kotlin/pull/134)
  * Two new fonts are available: AccessibleDfa and IA Writer Duospace.
  * The file structure now follows strictly the one from [ReadiumCSS's `dist/`](https://github.com/readium/readium-css/tree/master/css/dist), for easy upgrades and custom builds replacement.

### Deprecated

* `Navigator.currentLocation` and `NavigatorDelegate.locationDidChange()` are deprecated in favor of a unified `Navigator.currentLocator`, which is observable thanks to `LiveData`.

### Fixed

* **Important:** [Publications parsed from large manifests could crash the application](https://github.com/readium/r2-testapp-kotlin/issues/286) when starting a reading activity. To fix this, **`Publication` must not be put in an `Intent` extra anymore**. Instead, [use the new `Intent` extensions provided by Readium](https://github.com/readium/r2-testapp-kotlin/pull/303). This solution is a crutch until [we move away from `Activity` in the Navigator](https://github.com/readium/r2-navigator-kotlin/issues/115) and let reading apps handle the lifecycle of `Publication` themselves.
* [Crash when opening a publication with a space in its filename](https://github.com/readium/r2-navigator-kotlin/pull/136).
* [Jumping to an EPUB location from the search](https://github.com/readium/r2-navigator-kotlin/pull/111).
* The `AndroidManifest.xml` is not forcing anymore `allowBackup` and `supportsRtl`, to let reading apps manage these features themselves (contributed by [@twaddington](https://github.com/readium/r2-navigator-kotlin/pull/118)).


[unreleased]: https://github.com/readium/r2-navigator-kotlin/compare/master...HEAD
[2.0.0-alpha.1]: https://github.com/readium/r2-navigator-kotlin/compare/1.1.6...2.0.0-alpha.1
[2.0.0-alpha.2]: https://github.com/readium/r2-navigator-kotlin/compare/2.0.0-alpha.1...2.0.0-alpha.2

