# Changelog

All notable changes to this project will be documented in this file.

**Warning:** Features marked as *experimental* may change or be removed in a future release without notice. Use with caution.

## [Unreleased]

### Added

* (*Experimental*) New `Fragment` implementations as an alternative to the legacy `Activity` ones (contributed by [@johanpoirier](https://github.com/readium/r2-navigator-kotlin/pull/148)).
  * The fragments are chromeless, to let you customize the reading UX.
  * Use the new `NavigatorFragmentFactory` to help build the fragments, as showcased in `R2PdfActivity`.
  * At the moment, highlights and TTS are not yet supported in the new EPUB navigator `Fragment`.
  * [This is now the recommended way to integrate Readium](https://github.com/readium/r2-navigator-kotlin/issues/115) in your applications.


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
