# Changelog

All notable changes to this project will be documented in this file.

**Warning:** Features marked as *alpha* may change or be removed in a future release without notice. Use with caution.

## [Unreleased]

### Changed

* Upgraded to Kotlin 1.5.20.


## [2.0.0]

### Fixed

* [#267](https://github.com/readium/r2-testapp-kotlin/issues/267) Prints and copy characters left are now properly reported (contributed by [@qnga](https://github.com/readium/r2-lcp-kotlin/pull/104)).


## [2.0.0-beta.2]

### Added

* You can observe the progress of an acquisition by providing an `onProgress` closure to `LcpService.acquirePublication()`.
* Extensibility in licenses' `Rights` model.

### Changed

* The Renew Loan API got revamped to better support renewal through a web page.
    * You will need to implement `LcpLicense.RenewListener` to coordinate the UX interaction.
    * If your application fits Material Design guidelines, take a look at `MaterialRenewListener` for a default implementation.
* Removed dependency on Joda's `DateTime` in public APIs.
    * You can always create a `DateTime` from the standard `Date` objects if you relied on Joda's features in the callers.

### Fixed

* [#287](https://github.com/readium/r2-testapp-kotlin/issues/287) Make sure the passphrase input is visible on smaller devices in the authentication dialog.


## [2.0.0-beta.1]

### Changed

* Upgraded to Kotlin 1.4.10.

### Fixed

* When acquiring a publication, falls back on the media type declared in the license link if the server returns an unknown media type.


## [2.0.0-alpha.2]

### Added

* LCP implementation of the [Content Protection API](https://readium.org/architecture/proposals/006-content-protection) to work with the new [Streamer API](https://readium.org/architecture/proposals/005-streamer-api) (contributed by [@qnga](https://github.com/readium/r2-lcp-kotlin/pull/79)).
  * It is highly recommended that you upgrade to the new `Streamer` API to open publications, which will simplify DRM unlocking.
* Two default implementations of `LcpAuthenticating`:
  * `LcpDialogAuthentication` to prompt the user for its passphrase with the official LCP dialog.
  * `LcpPassphraseAuthentication` to provide directly a passphrase, pulled for example from a database or a web service.
* `LcpService::isLcpProtected()` provides a way to check if a file is protected with LCP.
* All the `LcpException` errors are now implementing `UserException` and are suitable for user display. Use `getUserMessage()` to get the localized message.

### Changed

* The public API got modernized to be more Kotlin idiomatic (contributed by [@qnga](https://github.com/readium/r2-lcp-kotlin/pull/84)).
  * All asynchronous APIs are now suspending to take advantage of Kotlin's coroutines.
* `LcpAuthenticating` is now provided with more information and you will need to update any implementation you may have.
  * If you copied the default authentication dialog, it's recommended to use `LcpDialogAuthentication` instead.
* Publications are now downloaded to a temporary location, to make sure disk storage can be recovered automatically by the system. After acquiring the publication, you need to move the downloaded file to another permanent location.
* The private `liblcp` dependency is now accessed through reflection, to allow switching LCP dynamically (contributed by [@qnga](https://github.com/readium/r2-lcp-kotlin/pull/87)).
  * You need to add `implementation "readium:liblcp:1.0.0@aar"` to your `build.gradle`.
  * `LcpService::create()` returns `null` if `lcplib` is not found.

### Fixed

* Decrypting resources in some edge cases (contributed by [@qnga](https://github.com/readium/r2-lcp-kotlin/pull/84))
* Issues with LSD interactions:
  * Exceptions handling with `renew` and `return` interactions.
  * Presentation of the `renew` interaction through an HTML page.
* The timeout of fetching the License Status Document is reduced to 5 seconds, to avoid blocking a publication opening in low Internet conditions.


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
[2.0.0-alpha.2]: https://github.com/readium/r2-lcp-kotlin/compare/2.0.0-alpha.1...2.0.0-alpha.2
[2.0.0-beta.1]: https://github.com/readium/r2-lcp-kotlin/compare/2.0.0-alpha.2...2.0.0-beta.1
[2.0.0-beta.2]: https://github.com/readium/r2-lcp-kotlin/compare/2.0.0-beta.1...2.0.0-beta.2
[2.0.0]: https://github.com/readium/r2-lcp-kotlin/compare/2.0.0-beta.2...2.0.0

