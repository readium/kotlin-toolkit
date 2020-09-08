# Changelog

All notable changes to this project will be documented in this file.

**Warning:** Features marked as *experimental* may change or be removed in a future release without notice. Use with caution.

## [Unreleased]

### Added

* LCP implementation of the [Content Protection API](https://github.com/readium/architecture/blob/master/proposals/006-content-protection.md) to work with the new [Streamer API](https://github.com/readium/architecture/blob/master/proposals/005-streamer-api.md) (contributed by [@qnga](https://github.com/readium/r2-lcp-kotlin/pull/79)).
  * It is highly recommended that you upgrade to the new `Streamer` API to open publications, which will simplify DRM unlocking.
* `LcpService::isLcpProtected()` provides a way to check if a file is protected with LCP.

### Changed

* The public API got modernized to be more Kotlin idiomatic (contributed by [@qnga](https://github.com/readium/r2-lcp-kotlin/pull/84)).
  * All asynchronous APIs are now suspending to take advantage of Kotlin's coroutines.
  * Follow the deprecation warnings to upgrade to the new names.
* `LcpAuthenticating` is now provided with more information and you will need to update your implementation.
* Publications are now downloaded to a temporary location, to make sure disk storage can be recovered automatically by the system. After acquiring the publication, you need to move the downloaded file to another permanent location.

### Fixed

* Issues with LSD interactions:
  * Exceptions handling with `renew` and `return` interactions.
  * Presentation of the `renew` interaction through an HTML page.

## [2.0.0-alpha.1]

### Added

* Support for [PDF](https://readium.org/lcp-specs/notes/lcp-for-pdf.html) and [Readium Audiobooks](https://readium.org/lcp-specs/notes/lcp-for-audiobooks.html) protected with LCP.

### Changed

* `LCPAuthenticating` can now return hashed passphrases in addition to clear ones. [This can be used by reading apps](https://github.com/readium/r2-lcp-kotlin/pull/64) fetching hashed passphrases from a web service or [Authentication for OPDS](https://readium.org/lcp-specs/notes/lcp-key-retrieval.html), for example.

### Fixed

* [`OutOfMemoryError` when downloading a large publication](https://github.com/readium/r2-lcp-kotlin/issues/70). This fix removed the dependency to [Fuel](https://github.com/kittinunf/fuel).
* The `AndroidManifest.xml` is not forcing anymore `allowBackup` and `supportsRtl`, to let reading apps manage these features themselves (contributed by [@twaddington](https://github.com/readium/r2-lcp-kotlin/pull/63)).


[unreleased]: https://github.com/readium/r2-lcp-kotlin/compare/master...HEAD
[2.0.0-alpha.1]: https://github.com/readium/r2-lcp-kotlin/compare/1.1.3...2.0.0-alpha.1
