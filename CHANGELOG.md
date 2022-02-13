# Changelog

All notable changes to this project will be documented in this file. Take a look at [the migration guide](docs/migration-guide.md) to upgrade between two major versions.

**Warning:** Features marked as *experimental* may change or be removed in a future release without notice. Use with caution.

## [Unreleased]

### Added

#### Shared

* A new `Publication.conformsTo()` API to identify the profile of a publication.
* Support for the [`conformsTo` RWPM metadata](https://github.com/readium/webpub-manifest/issues/65), to identify the profile of a `Publication`.

#### Navigator

* The PDF navigator now honors the publication reading progression with support for right-to-left and horizontal scrolling.
    * The default (auto) reading progression for PDF is top-to-bottom, which is vertical scrolling.
* A new convenience utility `EdgeTapNavigation` to trigger page turns while tapping the screen edges.
    * It takes into account the navigator reading progression to move into the right direction.
    * Call it from the `VisualNavigator.Listener.onTap()` callback as demonstrated below:
    ```kotlin
    override fun onTap(point: PointF): Boolean {
        val navigated = edgeTapNavigation.onTap(point, requireView())
        if (!navigated) {
            // Fallback action, for example toggling the app bar.
        }
        return true
    }
    ```
* The new `Navigator.Listener.onJumpToLocator()` API is called every time the navigator jumps to an explicit location, which might break the linear reading progression.
    * For example, it is called when clicking on internal links or programmatically calling `Navigator.go()`, but not when turning pages.
    * You can use this callback to implement a navigation history by differentiating between continuous and discontinuous moves.
* (*experimental*) A new audiobook navigator based on Jetpack `media2`.
    * See the [pull request #80](https://github.com/readium/kotlin-toolkit/pull/80) for the differences with the previous audiobook navigator.
    * This navigator is located in its own module `readium-navigator-media2`. You will need to add it to your dependencies to use it.
    * The Test App demonstrates how to use the new audiobook navigator, see `MediaService` and `AudioReaderFragment`.

### Deprecated

#### Shared

* `Publication.type` is now deprecated in favor of the new `Publication.conformsTo()` API which is more accurate.
    * For example, replace `publication.type == Publication.TYPE.EPUB` with `publication.conformsTo(Publication.Profile.EPUB)` before opening a publication with the `EpubNavigatorFragment`.
* `Link.toLocator()` is deprecated as it may create an incorrect `Locator` if the link `type` is missing.
    * Use `publication.locatorFromLink()` instead.

### Fixed

* Fix building with Kotlin 1.6.

#### Streamer

* Fixed the rendering of PDF covers in some edge cases.
* Fixed reading ranges of obfuscated EPUB resources.

#### Navigator

* Fixed turning pages of an EPUB reflowable resource with an odd number of columns. A virtual blank trailing column is appended to the resource when displayed as two columns.
* EPUB: Fallback on `reflowable` if the `presentation.layout` hint is missing from a manifest.
* EPUB: Offset of the current selection's `rect` to take into account the vertical padding.
* Improve backward compatibility of JavaScript files using Babel.
* [#193](https://github.com/readium/r2-navigator-kotlin/issues/193) Fixed invisible `<audio>` elements.


## [2.1.1]

### Changed

#### Navigator

* Improve loading of EPUB reflowable resources.
    * Resources are hidden until fully loaded and positioned.
    * Intermediary locators are not broadcasted as `currentLocator` anymore while loading a resource.
    * Improved accuracy when jumping to the middle of a large resource.
    * `EpubNavigatorFragment.PaginationListener.onPageLoaded()` is now called only a single time, for the currently visible page.
    * `VisualNavigator.Listener.onTap()` is called even when a resource is not fully loaded.

### Fixed

#### Navigator

* `EpubNavigatorFragment`'s `goForward()` and `goBackward()` are now jumping to the previous or next pages instead of resources.
* [#20](https://github.com/readium/kotlin-toolkit/issues/20) EPUB navigator stuck between two pages with vertical swipes.
* [#27](https://github.com/readium/kotlin-toolkit/issues/27) Internal links break the EPUB navigator (contributed by [@mihai-wolfpack](https://github.com/readium/kotlin-toolkit/pull/28)).


## [2.1.0]

### Added

#### Shared

* (*experimental*) A new Publication `SearchService` to search through the resources' content, with a default implementation `StringSearchService`.
* `ContentProtection.Scheme` can be used to identify protection technologies using unique URI identifiers.
* `Link` objects from archive-based publication assets (e.g. an EPUB/ZIP) have additional properties for entry metadata.
    ```json
    "properties" {
        "archive": {
            "entryLength": 8273,
            "isEntryCompressed": true
        }
    }
    ```

#### Streamer

* EPUB publications implement a `SearchService` to search through the content.
* Known DRM schemes (LCP and Adobe ADEPT) are now sniffed by the `Streamer`, when no registered `ContentProtection` supports them.
    * This is helpful to present an error message when the user attempts to open a protected publication not supported by the app.

#### Navigator

* The EPUB navigator is now able to navigate to a `Locator` using its `text` context. This is useful for search results or highlights missing precise locations.
* Get or clear the current user selection of the navigators implementing `SelectableNavigator`.
* (*experimental*) Support for the [Decorator API](https://github.com/readium/architecture/pull/160) to draw user interface elements over a publication's content.
    * This can be used to render highlights over a text selection, for example.
    * For now, only the EPUB navigator implements `DecorableNavigator`, for reflowable publications. You can implement custom decoration styles with `HtmlDecorationTemplate`.
* Customize the EPUB selection context menu by providing a custom `ActionMode.Callback` implementation with `EpubNavigatorFragment.Configuration.selectionActionModeCallback`.
    * This is an alternative to overriding `Activity.onActionModeStarted()` which does not seem to work anymore with Android 12.
* (*experimental*) A new audiobook navigator based on Android's [`MediaSession`](https://developer.android.com/guide/topics/media-apps/working-with-a-media-session).
    * It supports out-of-the-box media style notifications and background playback.
    * ExoPlayer is used by default for the actual playback, but you can use a custom player by implementing `MediaPlayer`.

#### OPDS

* New APIs using coroutines and R2's `HttpClient` instead of Fuel and kovenant (contributed by [@stevenzeck](https://github.com/readium/r2-opds-kotlin/pull/55)).

### Changed

* Upgraded to Kotlin 1.5.31 and Gradle 7.1.1

#### Streamer

* The default EPUB positions service now uses the archive entry length when available. [This is similar to how Adobe RMSDK generates page numbers](https://github.com/readium/architecture/issues/123).
    * To use the former strategy, create the `Streamer` with: `Streamer(parsers = listOf(EpubParser(reflowablePositionsStrategy = OriginalLength(pageLength = 1024))))`

#### Navigator

* The order of precedence of `Locator` locations in the reflowable EPUB navigator is: `text`, HTML ID, then `progression`. The navigator will now fallback on less precise locations in case of failure.

#### LCP

* Migrated to Jetpack Room for the SQLite database storing rights and passphrases (contributed by [@stevenzeck](https://github.com/readium/r2-lcp-kotlin/pull/116)).
    * Note that the internal SQL schema changed. You will need to update your app if you were querying the database manually.

### Fixed

#### Shared

* Crash with `HttpRequest.setPostForm()` on Android 6.
* HREF normalization when a resource path contains special characters.

#### Streamer

* EPUB style injection when a resource has a `<head>` tag with attributes.

#### Navigator

* When restoring a `Locator`, The PDF navigator now falls back on `locations.position` if the `page=` fragment identifier is missing.

#### OPDS

* Links in an OPDS 2 feed are normalized to the feed base URL.


## 2.0.0

### Added

#### Shared

* `HttpFetcher` is a new publication fetcher able to serve remote resources through HTTP.
    * The actual HTTP requests are performed with an instance of `HttpClient`.
* `HttpClient` is a new protocol exposing a high level API to perform HTTP requests.
    * `DefaultHttpClient` is an implementation of `HttpClient` using standard `HttpURLConnection` APIs. You can use `DefaultHttpClient.Callback` to customize how requests are created and even recover from errors, e.g. to implement Authentication for OPDS.
    * You can provide your own implementation of `HttpClient` to Readium APIs if you prefer to use a third-party networking library.

#### Streamer

* `Streamer` takes a new optional `HttpClient` dependency to handle HTTP requests.

### Fixed

#### Navigator

* Scrolling to an EPUB ID (e.g. from the table of contents) when the target spans several screens.

#### LCP

* [#267](https://github.com/readium/r2-testapp-kotlin/issues/267) Prints and copy characters left are now properly reported (contributed by [@qnga](https://github.com/readium/r2-lcp-kotlin/pull/104)).


## 2.0.0-beta.2

### Added

#### Shared

* `Publication.Service.Context` now holds a reference to the parent `Publication`. This can be used to access other services from a given `Publication.Service` implementation.
* The default `LocatorService` implementation can be used to get a `Locator` from a global progression in the publication.
  * `publication.locateProgression(0.5)`

#### Streamer

* `Server.addPublication()` is a new API which replaces `addEpub()` and is easier to use.
  * If the publication can be served, it will return the base URL which you need to provide to the Navigator `Activity` or `Fragment`.
  * You do not need to give the publication filename nor add the server port in the `$key-publicationPort` `SharedPreference` value anymore.

#### LCP

* You can observe the progress of an acquisition by providing an `onProgress` closure to `LcpService.acquirePublication()`.
* Extensibility in licenses' `Rights` model.

### Changed

#### Streamer

* The HTTP server now requests that publication resources are not cached by browsers.
  * Caching poses a security risk for protected publications.

#### Navigator

* `R2EpubActivity` and `R2AudiobookActivity` require a new `baseUrl` `Intent` extra. You need to set it to the base URL returned by `Server.addPublication()` from the Streamer.

#### LCP

* The Renew Loan API got revamped to better support renewal through a web page.
    * You will need to implement `LcpLicense.RenewListener` to coordinate the UX interaction.
    * If your application fits Material Design guidelines, take a look at `MaterialRenewListener` for a default implementation.
* Removed dependency on Joda's `DateTime` in public APIs.
    * You can always create a `DateTime` from the standard `Date` objects if you relied on Joda's features in the callers.

### Fixed

#### Shared

* [#129](https://github.com/readium/r2-shared-kotlin/issues/129) Improve performances when reading deflated ZIP resources.
  * For example, it helps with large image-based FXL EPUB which used to be slow to render.
* [#136](https://github.com/readium/r2-shared-kotlin/issues/136) `null` values in JSON string properties are now properly parsed as nullable types, instead of the string `"null"`

#### Navigator

* [#217](https://github.com/readium/r2-testapp-kotlin/issues/217) Interactive HTML elements are not bypassed anymore when handling touch gestures.
  * Scripts using `preventDefault()` are now taken into account and do not trigger a tap event anymore.
* [#150](https://github.com/readium/r2-navigator-kotlin/issues/150) External links are opened in a Chrome Custom Tab instead of the navigator's web view.
* [#52](https://github.com/readium/r2-navigator-kotlin/issues/52) Memory leak in EPUB web views. This fixes ongoing media playback when closing an EPUB.

#### LCP

* [#287](https://github.com/readium/r2-testapp-kotlin/issues/287) Make sure the passphrase input is visible on smaller devices in the authentication dialog.


## 2.0.0-beta.1

### Added

#### Shared

* `PublicationAsset` is a new interface which can be used to open a publication from various medium, such as a file, a remote URL or a custom source.
  * `File` was replaced by `FileAsset`, which implements `PublicationAsset`.

#### Navigator

* Support for [display cutouts](https://developer.android.com/guide/topics/display-cutout) (screen notches).
    * **IMPORTANT**: You need to remove any `setPadding()` statement from your app in `UserSettings.kt`, if you copied it from the test app.
    * If you embed a navigator fragment (e.g. `EpubNavigatorFragment`) yourself, you need to opt-in by [specifying the `layoutInDisplayCutoutMode`](https://developer.android.com/guide/topics/display-cutout#choose_how_your_app_handles_cutout_areas) of the host `Activity`.
    * `R2EpubActivity` and `R2CbzActivity` automatically apply `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` to their window's `layoutInDisplayCutoutMode`.
    * `PdfNavigatorFragment` is not yet compatible with display cutouts, because of limitations from the underlying PDF viewer.
* Customize EPUB vertical padding by overriding the `r2.navigator.epub.vertical_padding` dimension.
    * Follow [Android's convention for alternative resources](https://developer.android.com/guide/topics/resources/providing-resources#AlternativeResources) to specify different paddings for landscape (`values-land`) or large screens.

### Changed

* Upgraded to Kotlin 1.4.10.

#### Shared

* `Format` got merged into `MediaType`, to simplify the media type APIs.
  * You can use `MediaType.of()` to sniff the type of a file or bytes.
      * All the `MediaType.of()` functions are now suspending to prevent deadlocks with `runBlocking`.
  * `MediaType` has now optional `name` and `fileExtension` properties.
  * Some publication formats can be represented by several media type aliases. Using `mediaType.canonicalMediaType()` will give you the canonical media type to use, for example when persisting the file type in a database. All Readium APIs are already returning canonical media types, so it only matters if you create a `MediaType` yourself from its string representation.
* `ContentLayout` is deprecated, use `publication.metadata.effectiveReadingProgression` to determine the reading progression of a publication instead.

#### Streamer

* `Streamer` is now expecting a `PublicationAsset` instead of a `File`. You can create custom implementations of `PublicationAsset` to open a publication from different medium, such as a file, a remote URL, in-memory bytes, etc.
  * `FileAsset` can be used to replace `File` and provides the same behavior.

#### Navigator

* All `utils.js` functions were moved under a `readium.` namespace. You will need to update your code if you were calling them manually.

### Fixed

#### LCP

* When acquiring a publication, falls back on the media type declared in the license link if the server returns an unknown media type.

#### Navigator

* EPUBs declaring multiple languages were laid out from right to left if the first language had an RTL reading
progression. Now if no reading progression is set, the `effectiveReadingProgression` will be LTR.
* [#152](https://github.com/readium/r2-navigator-kotlin/issues/152) Panning through a zoomed-in fixed layout EPUB (contributed by [@johanpoirier](https://github.com/readium/r2-navigator-kotlin/pull/172)).
* [#146](https://github.com/readium/r2-navigator-kotlin/issues/146) Various reflowable EPUB columns shift issues.
* Restoring the last EPUB location after configuration changes (e.g. screen rotation).
* Edge taps to turn pages when the app runs in a multi-windows environment.


## 2.0.0-alpha.2

### Added

#### Shared

* The [Publication Services API](https://readium.org/architecture/proposals/004-publication-helpers-services) allows to extend a `Publication` with custom implementations of known services. This version ships with a few predefined services:
  * `PositionsService` provides a list of discrete locations in the publication, no matter what the original format is.
  * `CoverService` provides an easy access to a bitmap version of the publication cover.
* The [Composite Fetcher API](https://readium.org/architecture/proposals/002-composite-fetcher-api) can be used to extend the way publication resources are accessed.
* Support for exploded directories for any archive-based publication format.
* [Content Protection](https://readium.org/architecture/proposals/006-content-protection) handles DRM and other format-specific protections in a more systematic way.
  * LCP now ships an `LcpContentProtection` implementation to be plugged into the `Streamer`.
  * You can add custom `ContentProtection` implementations to support other DRMs by providing an instance to the `Streamer`.

#### Streamer

* [Streamer API](https://readium.org/architecture/proposals/005-streamer-api) offers a simple interface to parse a publication and replace standalone parsers.
* A generic `ImageParser` for bitmap-based archives (CBZ or exploded directories) and single image files.
* A generic `AudioParser` for audio-based archives (Zipped Audio Book or exploded directories) and single audio files.

#### Navigator

* Support for the new `Publication` model using the [Content Protection](https://readium.org/architecture/proposals/006-content-protection) for DRM rights and the [Fetcher](https://readium.org/architecture/proposals/002-composite-fetcher-api) for resource access.
* (*experimental*) New `Fragment` implementations as an alternative to the legacy `Activity` ones (contributed by [@johanpoirier](https://github.com/readium/r2-navigator-kotlin/pull/148)).
  * The fragments are chromeless, to let you customize the reading UX.
  * To create the fragments use the matching factory such as `EpubNavigatorFragment.createFactory()`, as showcased in `R2EpubActivity`.
  * At the moment, highlights and TTS are not yet supported in the new EPUB navigator `Fragment`.
  * [This is now the recommended way to integrate Readium](https://github.com/readium/r2-navigator-kotlin/issues/115) in your applications.

#### LCP

* LCP implementation of the [Content Protection API](https://readium.org/architecture/proposals/006-content-protection) to work with the new [Streamer API](https://readium.org/architecture/proposals/005-streamer-api) (contributed by [@qnga](https://github.com/readium/r2-lcp-kotlin/pull/79)).
  * It is highly recommended that you upgrade to the new `Streamer` API to open publications, which will simplify DRM unlocking.
* Two default implementations of `LcpAuthenticating`:
  * `LcpDialogAuthentication` to prompt the user for its passphrase with the official LCP dialog.
  * `LcpPassphraseAuthentication` to provide directly a passphrase, pulled for example from a database or a web service.
* `LcpService::isLcpProtected()` provides a way to check if a file is protected with LCP.
* All the `LcpException` errors are now implementing `UserException` and are suitable for user display. Use `getUserMessage()` to get the localized message.

### Changed

#### Shared

* [The `Publication` and `Container` types were merged together](https://readium.org/architecture/proposals/003-publication-encapsulation) to offer a single interface to a publication's resources.
  * Use `publication.get()` to read the content of a resource, such as the cover. It will automatically be decrypted if a `ContentProtection` was attached to the `Publication`.

#### Streamer

* `Container` and `ContentFilters` were replaced by a shared implementation of a [`Fetcher`](https://readium.org/architecture/proposals/002-composite-fetcher-api).

#### Navigator

* `currentLocator` is now a `StateFlow` instead of `LiveData`, to better support chromeless navigators such as an audiobook navigator.
  * If you were observing `currentLocator` in a UI context, you can continue to do so with `currentLocator.asLiveData()`.
* Improvements to the PDF navigator:
  * The navigator doesn't require PDF publications to be served from an HTTP server anymore. A side effect is that the navigator is now able to open larger PDF files.
  * `PdfNavigatorFragment.Listener::onResourceLoadFailed()` can be used to report fatal errors to the user, such as when trying to open a PDF document that is too large for the available memory.
  * A dedicated `PdfNavigatorFragment.createFactory()` was added, which deprecates the use of `NavigatorFragmentFactory`.

#### LCP

* The public API got modernized to be more Kotlin idiomatic (contributed by [@qnga](https://github.com/readium/r2-lcp-kotlin/pull/84)).
  * All asynchronous APIs are now suspending to take advantage of Kotlin's coroutines.
* `LcpAuthenticating` is now provided with more information and you will need to update any implementation you may have.
  * If you copied the default authentication dialog, it's recommended to use `LcpDialogAuthentication` instead.
* Publications are now downloaded to a temporary location, to make sure disk storage can be recovered automatically by the system. After acquiring the publication, you need to move the downloaded file to another permanent location.
* The private `liblcp` dependency is now accessed through reflection, to allow switching LCP dynamically (contributed by [@qnga](https://github.com/readium/r2-lcp-kotlin/pull/87)).
  * You need to add `implementation "readium:liblcp:1.0.0@aar"` to your `build.gradle`.
  * `LcpService::create()` returns `null` if `lcplib` is not found.

### Fixed

#### Shared

* `OutOfMemoryError` occuring while opening large publications are now caught to prevent crashes. They are reported as `Resource.Exception.OutOfMemory`.
* Readium can now open PDF documents of any size without crashing. However, LCP protected PDFs are still limited by the available memory.

#### Streamer

* Readium can now open PDF documents of any size without crashing. However, LCP protected PDFs are still limited by the available memory.
* Various HTTP server fixes and optimizations.

#### Navigator

* Prevent switching to the next resource by mistake when scrolling through an EPUB resource in scroll mode.

#### LCP

* Decrypting resources in some edge cases (contributed by [@qnga](https://github.com/readium/r2-lcp-kotlin/pull/84))
* Issues with LSD interactions:
  * Exceptions handling with `renew` and `return` interactions.
  * Presentation of the `renew` interaction through an HTML page.
* The timeout of fetching the License Status Document is reduced to 5 seconds, to avoid blocking a publication opening in low Internet conditions.


## 2.0.0-alpha.1

### Added

#### Shared

* Support for [Positions List](https://github.com/readium/architecture/tree/master/models/locators/positions), which provides a list of discrete locations in a publication and can be used to implement an approximation of page numbers.
  * Get the visible position from the current `Locator` with `locations.position`.
  * The total number of positions can be retrieved with `publication.positions().size`. It is a suspending function because computing positions the first time can be expensive. 
* The new [Format API](https://github.com/readium/architecture/blob/master/proposals/001-format-api.md) simplifies the detection of file formats, including known publication formats such as EPUB and PDF.
  * [A format can be "sniffed"](https://github.com/readium/architecture/blob/master/proposals/001-format-api.md#sniffing-the-format-of-raw-bytes) from files, raw bytes or even HTTP responses.
  * Reading apps are welcome to [extend the API with custom formats](https://github.com/readium/architecture/blob/master/proposals/001-format-api.md#supporting-a-custom-format).
  * Using `Link.mediaType.matches()` is now recommended [to safely check the type of a resource](https://github.com/readium/architecture/blob/master/proposals/001-format-api.md#mediatype-class).
  * [More details about the Kotlin implementation can be found in the pull request.](https://github.com/readium/r2-shared-kotlin/pull/100)
* In `Publication` shared models:
  * Support for the [Presentation Hints](https://readium.org/webpub-manifest/extensions/presentation.html) extension.
  * Support for OPDS holds, copies and availability in `Link`, for library-specific features.
  * Readium Web Publication Manifest extensibility is now supported for `Publication`, `Metadata`, link's `Properties` and locator's `Locations`, which means that you are now able to access custom JSON properties in a manifest [by creating Kotlin extensions on the shared models](https://github.com/readium/r2-shared-kotlin/blob/a4e5b4461d6ce9f989a79c8f912f3cbdaff4667e/r2-shared/src/main/java/org/readium/r2/shared/publication/opds/Properties.kt#L16).

#### Streamer

* Support for [Positions List](https://github.com/readium/architecture/tree/master/models/locators/positions) with EPUB, CBZ and PDF. Positions provide a list of discrete locations in a publication and can be used to implement an approximation of page numbers.
  * Get the visible position from the current `Locator` with `locations.position`.
  * The total number of positions can be retrieved with `publication.positions().size`. It is a suspending function because computing positions the first time can be expensive. 
* `ReadiumWebPubParser` to parse all Readium Web Publication profiles, including [Audiobooks](https://readium.org/webpub-manifest/extensions/audiobook.html), [LCP for Audiobooks](https://readium.org/lcp-specs/notes/lcp-for-audiobooks.html) and [LCP for PDF](https://readium.org/lcp-specs/notes/lcp-for-pdf.html). It parses both manifests and packages.
* (*experimental*) `PDFParser` to parse single PDF documents.
  * The PDF parser is based on [PdfiumAndroid](https://github.com/barteksc/PdfiumAndroid/), which may increase the size of your apps. Please open an issue if this is a problem for you, as we are considering different solutions to fix this in a future release.

#### Navigator

* The [position](https://github.com/readium/architecture/tree/master/models/locators/positions) is now reported in the locators for EPUB, CBZ and PDF.
* (*experimental*) [PDF navigator](https://github.com/readium/r2-navigator-kotlin/pull/130).
  * Supports both single PDF and LCP protected PDF.
  * As a proof of concept, [it is implemented using `Fragment` instead of `Activity`](https://github.com/readium/r2-navigator-kotlin/issues/115). `R2PdfActivity` showcases how to use the `PdfNavigatorFragment` with the new `NavigatorFragmentFactory`.
  * The navigator is based on [AndroidPdfViewer](https://github.com/barteksc/AndroidPdfViewer), which may increase the size of your apps. Please open an issue if this is a problem for you, as we are considering different solutions to fix this in a future release.

#### LCP

* Support for [PDF](https://readium.org/lcp-specs/notes/lcp-for-pdf.html) and [Readium Audiobooks](https://readium.org/lcp-specs/notes/lcp-for-audiobooks.html) protected with LCP.

### Changed

#### Shared

* [The `Publication` shared models underwent an important refactoring](https://github.com/readium/r2-shared-kotlin/pull/88) and some of these changes are breaking. [Please refer to the migration guide to update your codebase](https://github.com/readium/r2-testapp-kotlin/blob/develop/MIGRATION-GUIDE.md).
  * All the models are now immutable data classes, to improve code safety. This should not impact reading apps unless you created `Publication` or other models yourself.
  * A few types and enums were renamed to follow the Google Android Style coding convention better. Just follow deprecation warnings to update your codebase.

#### Streamer

* The CSS, JavaScript and fonts injection in the `Server` was refactored to reduce the risk of collisions and simplify your codebase.
  * **This is a breaking change**, [to upgrade your app you need to](https://github.com/readium/r2-testapp-kotlin/pull/321/files#diff-9bb6ad21df8b48f171ba6266616662ac):
    * Provide the application's `Context` when creating a `Server`.
    * Remove the following injection statements, which are now handled directly by the Streamer:
```kotlin
server.loadCustomResource(assets.open("scripts/crypto-sha256.js"), "crypto-sha256.js", Injectable.Script)   
server.loadCustomResource(assets.open("scripts/highlight.js"), "highlight.js", Injectable.Script)
```

#### Navigator

* [Upgraded to Readium CSS 1.0.0-beta.1.](https://github.com/readium/r2-navigator-kotlin/pull/134)
  * Two new fonts are available: AccessibleDfa and IA Writer Duospace.
  * The file structure now follows strictly the one from [ReadiumCSS's `dist/`](https://github.com/readium/readium-css/tree/master/css/dist), for easy upgrades and custom builds replacement.

#### LCP

* `LCPAuthenticating` can now return hashed passphrases in addition to clear ones. [This can be used by reading apps](https://github.com/readium/r2-lcp-kotlin/pull/64) fetching hashed passphrases from a web service or [Authentication for OPDS](https://readium.org/lcp-specs/notes/lcp-key-retrieval.html), for example.

### Deprecated

#### Shared

* `R2SyntheticPageList` was replaced by the aforementioned Positions List and can be safely removed from your codebase.

#### Navigator

* `Navigator.currentLocation` and `NavigatorDelegate.locationDidChange()` are deprecated in favor of a unified `Navigator.currentLocator`, which is observable thanks to `LiveData`.

### Fixed

#### Shared

* **Important:** [Publications parsed from large manifests could crash the application](https://github.com/readium/r2-testapp-kotlin/issues/286) when starting a reading activity. To fix this, **`Publication` must not be put in an `Intent` extra anymore**. Instead, [use the new `Intent` extensions provided by Readium](https://github.com/readium/r2-testapp-kotlin/pull/303). This solution is a crutch until [we move away from `Activity` in the Navigator](https://github.com/readium/r2-navigator-kotlin/issues/115) and let reading apps handle the lifecycle of `Publication` themselves.
* [The local HTTP server was broken](https://github.com/readium/r2-testapp-kotlin/pull/306) when provided with publication filenames containing invalid characters.
* XML namespace prefixes are now properly supported when an author chooses unusual ones (contributed by [@qnga](https://github.com/readium/r2-shared-kotlin/pull/85)).
* The `AndroidManifest.xml` is not forcing anymore `allowBackup` and `supportsRtl`, to let reading apps manage these features themselves (contributed by [@twaddington](https://github.com/readium/r2-shared-kotlin/pull/93)).

#### Streamer

* The EPUB parser underwent a significant refactoring to fix a number of issues (contributed by [@qnga](https://github.com/readium/r2-streamer-kotlin/pull/89))
  * [Metadata parsing was updated to follow our up-to-date specifications](https://github.com/readium/r2-streamer-kotlin/pull/102).
  * XML namespace prefixes are now properly supported, when an author chooses unusual ones.
  * Similarly, default vocabularies and prefixes for EPUB 3 Property Data Types are now properly handled.
* [`Server` was broken](https://github.com/readium/r2-testapp-kotlin/pull/306) when provided with publication filenames containing invalid characters.
* [EPUB publishers' default styles are not overriden by Readium CSS anymore](https://github.com/readium/r2-navigator-kotlin/issues/132).
* The `AndroidManifest.xml` is not forcing anymore `allowBackup` and `supportsRtl`, to let reading apps manage these features themselves (contributed by [@twaddington](https://github.com/readium/r2-streamer-kotlin/pull/93)).

#### Navigator

* **Important:** [Publications parsed from large manifests could crash the application](https://github.com/readium/r2-testapp-kotlin/issues/286) when starting a reading activity. To fix this, **`Publication` must not be put in an `Intent` extra anymore**. Instead, [use the new `Intent` extensions provided by Readium](https://github.com/readium/r2-testapp-kotlin/pull/303). This solution is a crutch until [we move away from `Activity` in the Navigator](https://github.com/readium/r2-navigator-kotlin/issues/115) and let reading apps handle the lifecycle of `Publication` themselves.
* [Crash when opening a publication with a space in its filename](https://github.com/readium/r2-navigator-kotlin/pull/136).
* [Jumping to an EPUB location from the search](https://github.com/readium/r2-navigator-kotlin/pull/111).
* The `AndroidManifest.xml` is not forcing anymore `allowBackup` and `supportsRtl`, to let reading apps manage these features themselves (contributed by [@twaddington](https://github.com/readium/r2-navigator-kotlin/pull/118)).

#### OPDS

* XML namespace prefixes are now properly supported when an author chooses unusual ones (contributed by [@qnga](https://github.com/readium/r2-shared-kotlin/pull/85)).
* The `AndroidManifest.xml` is not forcing anymore `allowBackup` and `supportsRtl`, to let reading apps manage these features themselves (contributed by [@twaddington](https://github.com/readium/r2-opds-kotlin/pull/41)).

#### LCP

* [`OutOfMemoryError` when downloading a large publication](https://github.com/readium/r2-lcp-kotlin/issues/70). This fix removed the dependency to [Fuel](https://github.com/kittinunf/fuel).
* The `AndroidManifest.xml` is not forcing anymore `allowBackup` and `supportsRtl`, to let reading apps manage these features themselves (contributed by [@twaddington](https://github.com/readium/r2-lcp-kotlin/pull/63)).


[unreleased]: https://github.com/readium/kotlin-toolkit/compare/main...HEAD
[2.1.0]: https://github.com/readium/kotlin-kotlin/compare/2.0.0...2.1.0
[2.1.1]: https://github.com/readium/kotlin-kotlin/compare/2.1.0...2.1.1
