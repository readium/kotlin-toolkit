# Changelog

All notable changes to this project will be documented in this file.

**Warning:** Features marked as *experimental* may change or be removed in a future release without notice. Use with caution.

[unreleased]: https://github.com/readium/r2-opds-kotlin/compare/master...HEAD
[1.2.0]: https://github.com/readium/r2-opds-kotlin/compare/1.1.4...1.2.0

## [Unreleased]

## [1.2.0]

### Changed

* Update internal usage of the `Publication` shared models.

### Fixed

* XML namespace prefixes are now properly supported when an author chooses unusual ones (contributed by [@qnga](https://github.com/readium/r2-shared-kotlin/pull/85)).
* The `AndroidManifest.xml` is not forcing anymore `allowBackup` and `supportsRtl`, to let reading apps manage these features themselves (contributed by [@twaddington](https://github.com/readium/r2-opds-kotlin/pull/41)).
