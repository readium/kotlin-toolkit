# Changelog

All notable changes to this project will be documented in this file.

**Warning:** Features marked as *experimental* may change or be removed in a future release without notice. Use with caution.

## [Unreleased]

### Added

* New APIs using coroutines and R2's `HttpClient` instead of Fuel and kovenant (contributed by [@stevenzeck](https://github.com/readium/r2-opds-kotlin/pull/55)).

### Fixed

* Links in an OPDS 2 feed are normalized to the feed base URL.


## [2.0.0]

### Changed

* Upgraded dependencies.


## [2.0.0-beta.2]

### Changed

* Upgraded dependencies.


## [2.0.0-beta.1]

### Changed

* Upgraded to Kotlin 1.4.10.


## [2.0.0-alpha.2]

### Changed

* Update internal usage of the `Publication` shared models.


## [2.0.0-alpha.1]

### Changed

* Update internal usage of the `Publication` shared models.

### Fixed

* XML namespace prefixes are now properly supported when an author chooses unusual ones (contributed by [@qnga](https://github.com/readium/r2-shared-kotlin/pull/85)).
* The `AndroidManifest.xml` is not forcing anymore `allowBackup` and `supportsRtl`, to let reading apps manage these features themselves (contributed by [@twaddington](https://github.com/readium/r2-opds-kotlin/pull/41)).


[unreleased]: https://github.com/readium/r2-opds-kotlin/compare/master...HEAD
[2.0.0-alpha.1]: https://github.com/readium/r2-opds-kotlin/compare/1.1.4...2.0.0-alpha.1
[2.0.0-alpha.2]: https://github.com/readium/r2-opds-kotlin/compare/2.0.0-alpha.1...2.0.0-alpha.2
[2.0.0-beta.1]: https://github.com/readium/r2-opds-kotlin/compare/2.0.0-alpha.2...2.0.0-beta.1
[2.0.0-beta.2]: https://github.com/readium/r2-opds-kotlin/compare/2.0.0-beta.1...2.0.0-beta.2
[2.0.0]: https://github.com/readium/r2-opds-kotlin/compare/2.0.0-beta.2...2.0.0

