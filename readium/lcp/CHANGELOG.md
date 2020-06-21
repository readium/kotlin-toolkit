# Changelog

All notable changes to this project will be documented in this file.

**Warning:** Features marked as *experimental* may change or be removed in a future release without notice. Use with caution.

## [Unreleased]

### Added

* Support for [PDF](https://readium.org/lcp-specs/notes/lcp-for-pdf.html) and [Readium Audiobooks](https://readium.org/lcp-specs/notes/lcp-for-audiobooks.html) protected with LCP.

### Changed

* `LCPAuthenticating` can now return hashed passphrases in addition to clear ones. [This can be used by reading apps](https://github.com/readium/r2-lcp-kotlin/pull/64) fetching hashed passphrases from a web service or [Authentication for OPDS](https://readium.org/lcp-specs/notes/lcp-key-retrieval.html), for example.

### Fixed

* [`OutOfMemoryError` when downloading a large publication](https://github.com/readium/r2-lcp-kotlin/issues/70). This fix removed the dependency to [Fuel](https://github.com/kittinunf/fuel).
* The `AndroidManifest.xml` is not forcing anymore `allowBackup` and `supportsRtl`, to let reading apps manage these features themselves (contributed by [@twaddington](https://github.com/readium/r2-lcp-kotlin/pull/63)).

[unreleased]: https://github.com/readium/r2-lcp-kotlin/compare/master...HEAD
[x.x.x]: https://github.com/readium/r2-lcp-kotlin/compare/1.1.3...x.x.x
