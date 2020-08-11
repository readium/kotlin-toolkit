# Changelog

All notable changes to this project will be documented in this file.

**Warning:** Features marked as *experimental* may change or be removed in a future release without notice. Use with caution.

## [Unreleased]

## [2.0.0-alpha.1]

### Added

* Support for [Positions List](https://github.com/readium/architecture/tree/master/models/locators/positions), which provides a list of discrete locations in a publication and can be used to implement an approximation of page numbers.
  * Get the visible position from the current `Locator` with `locations.position`.
  * The total number of positions can be retrieved with `publication.positions().size`. It is a suspending function because computing positions the first time can be expensive. 
* The new [Format API](https://github.com/readium/architecture/blob/master/proposals/001-format-api.md) simplifies the detection of file formats, including known publication formats such as EPUB and PDF.
  * [A format can be "sniffed"](https://github.com/readium/architecture/blob/master/proposals/001-format-api.md#sniffing-the-format-of-raw-bytes) from files, raw bytes or even HTTP responses.
  * Reading apps are welcome to [extend the API with custom formats](https://github.com/readium/architecture/blob/master/proposals/001-format-api.md#supporting-a-custom-format).
  * Using `Link.mediaType?.matches()` is now recommended [to safely check the type of a resource](https://github.com/readium/architecture/blob/master/proposals/001-format-api.md#mediatype-class).
  * [More details about the Kotlin implementation can be found in the pull request.](https://github.com/readium/r2-shared-kotlin/pull/100)
* In `Publication` shared models:
  * Support for the [Presentation Hints](https://readium.org/webpub-manifest/extensions/presentation.html) extension.
  * Support for OPDS holds, copies and availability in `Link`, for library-specific features.
  * Readium Web Publication Manifest extensibility is now supported for `Publication`, `Metadata`, link's `Properties` and locator's `Locations`, which means that you are now able to access custom JSON properties in a manifest [by creating Kotlin extensions on the shared models](https://github.com/readium/r2-shared-kotlin/blob/a4e5b4461d6ce9f989a79c8f912f3cbdaff4667e/r2-shared/src/main/java/org/readium/r2/shared/publication/opds/Properties.kt#L16).

### Changed

* [The `Publication` shared models underwent an important refactoring](https://github.com/readium/r2-shared-kotlin/pull/88) and some of these changes are breaking. [Please refer to the migration guide to update your codebase](https://github.com/readium/r2-testapp-kotlin/blob/develop/MIGRATION-GUIDE.md).
  * All the models are now immutable data classes, to improve code safety. This should not impact reading apps unless you created `Publication` or other models yourself.
  * A few types and enums were renamed to follow the Google Android Style coding convention better. Just follow deprecation warnings to update your codebase.

### Deprecated

* `R2SyntheticPageList` was replaced by the aforementioned Positions List and can be safely removed from your codebase.

### Fixed

* **Important:** [Publications parsed from large manifests could crash the application](https://github.com/readium/r2-testapp-kotlin/issues/286) when starting a reading activity. To fix this, **`Publication` must not be put in an `Intent` extra anymore**. Instead, [use the new `Intent` extensions provided by Readium](https://github.com/readium/r2-testapp-kotlin/pull/303). This solution is a crutch until [we move away from `Activity` in the Navigator](https://github.com/readium/r2-navigator-kotlin/issues/115) and let reading apps handle the lifecycle of `Publication` themselves.
* [The local HTTP server was broken](https://github.com/readium/r2-testapp-kotlin/pull/306) when provided with publication filenames containing invalid characters.
* XML namespace prefixes are now properly supported when an author chooses unusual ones (contributed by [@qnga](https://github.com/readium/r2-shared-kotlin/pull/85)).
* The `AndroidManifest.xml` is not forcing anymore `allowBackup` and `supportsRtl`, to let reading apps manage these features themselves (contributed by [@twaddington](https://github.com/readium/r2-shared-kotlin/pull/93)).


[unreleased]: https://github.com/readium/r2-shared-kotlin/compare/master...HEAD
[2.0.0-alpha.1]: https://github.com/readium/r2-shared-kotlin/compare/1.1.6...2.0.0-alpha.1
