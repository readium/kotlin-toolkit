# Migration Guide

All migration steps necessary in reading apps to upgrade to major versions of the Kotlin Readium toolkit will be documented in this file.

## 2.3.0

### PDF support

The PDF navigator got refactored to support arbitrary third-party PDF engines. As a consequence, [PdfiumAndroid](https://github.com/barteksc/PdfiumAndroid) (the open source PDF renderer we previously used) was extracted into its own adapter package. **This is a breaking change** if you were supporting PDF in your application.

This new version ships with an adapter for the commercial PDF engine [PSPDFKit](https://pspdfkit.com/), see the instructions under `readium/adapter/pspdfkit` to set it up.

If you wish to keep using the open source library [PdfiumAndroid](https://github.com/barteksc/PdfiumAndroid), you need to migrate your app.

#### Migrating to the PdfiumAndroid adapter

First, add the new dependency in your app's `build.gradle`.

```groovy
dependencies {
    implementation "com.github.readium.kotlin-toolkit:readium-adapter-pdfium:$readium_version"
    // Or, if you need only the parser but not the navigator:
    implementation "com.github.readium.kotlin-toolkit:readium-adapter-pdfium-document:$readium_version"
}
```

Then, setup the `Streamer` with the adapter factory: 

```kotlin
Streamer(...,
    pdfFactory = PdfiumDocumentFactory(context)
)
```

Finally, provide the new `PdfDocumentFragmentFactory` to `PdfNavigatorFragment`:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    childFragmentManager.fragmentFactory =
        PdfNavigatorFragment.createFactory(...,
            documentFragmentFactory = PdfiumDocumentFragment.createFactory()
        )

    super.onCreate(savedInstanceState)
}
```


## 2.1.0

With this new release, we migrated all the [`r2-*-kotlin`](https://github.com/readium/?q=r2-kotlin) repositories to [a single `kotlin-toolkit` repository](https://github.com/readium/r2-testapp-kotlin/issues/461).

### Using JitPack

If you are integrating Readium with the JitPack Maven repository, the same Readium modules are available as before. Just replace the former dependency notations with the new ones, [per the README](../README.md).

```
dependencies {
    implementation "com.github.readium.kotlin-toolkit:readium-shared:$readium_version"
    implementation "com.github.readium.kotlin-toolkit:readium-streamer:$readium_version"
    implementation "com.github.readium.kotlin-toolkit:readium-navigator:$readium_version"
    implementation "com.github.readium.kotlin-toolkit:readium-opds:$readium_version"
    implementation "com.github.readium.kotlin-toolkit:readium-lcp:$readium_version"
}
```

### Using a fork

If you are integrating your own forks of the Readium modules, you will need to migrate them to a single fork and port your changes. Follow strictly the given steps and it should go painlessly.

1. Upgrade your forks to the latest Readium 2.1.0 version from the legacy repositories, as you would with any update. The 2.1.0 version is available on both the legacy repositories and the new `kotlin-toolkit` one. It will be used to port your changes over to the single repository.
2. [Fork the new `kotlin-toolkit` repository](https://github.com/readium/kotlin-toolkit/fork) on your own GitHub space.
3. In a new local directory, clone your legacy forks as well as the new single fork:
    ```sh
    mkdir readium-migration
    cd readium-migration
   
    # Clone the legacy forks
    git clone https://github.com/USERNAME/r2-shared-kotlin.git
    git clone https://github.com/USERNAME/r2-streamer-kotlin.git
    git clone https://github.com/USERNAME/r2-navigator-kotlin.git
    git clone https://github.com/USERNAME/r2-opds-kotlin.git
    git clone https://github.com/USERNAME/r2-lcp-kotlin.git
   
    # Clone the new single fork
    git clone https://github.com/USERNAME/kotlin-toolkit.git
    ```
4. Reset the new fork to be in the same state as the 2.1.0 release.
    ```sh
    cd kotlin-toolkit
    git reset --hard 2.1.0
    ```
5. For each Readium module, port your changes over to the new fork.
    ```sh
    rm -rf readium/*/src
   
    cp -r ../r2-shared-kotlin/r2-shared/src readium/shared
    cp -r ../r2-streamer-kotlin/r2-streamer/src readium/streamer
    cp -r ../r2-navigator-kotlin/r2-navigator/src readium/navigator
    cp -r ../r2-opds-kotlin/r2-opds/src readium/opds
    cp -r ../r2-lcp-kotlin/r2-lcp/src readium/lcp
    ```
6. Review your changes, then commit.
    ```sh
    git add readium
    git commit -m "Apply local changes to Readium"
    ```
7. Finally, pull the changes to upgrade to the latest version of the fork. You might need to fix some conflicts.
    ```sh
    git pull --rebase
    git push
    ```
   
Your fork is now ready! To integrate it in your app as a local Git clone or submodule, follow the instructions from the [README](../README.md).


## [2.0.0](https://github.com/readium/r2-testapp-kotlin/compare/2.2.0-beta.2...2.2.0)

Nothing to change in your app to upgrade from 2.0.0-beta.2 to the final 2.0.0 release! Please follow the relevant sections if you are upgrading from an older version.

## [2.0.0-beta.2](https://github.com/readium/r2-testapp-kotlin/compare/2.2.0-beta.1...2.2.0-beta.2)

This new beta is the last one before the final 2.0.0 release. It is mostly focused on bug fixes but we also adjusted the LCP and HTTP server APIs before setting it in stone for the 2.x versions.

### Serving publications with the HTTP server

The API used to serve `Publication` resources with the Streamer's HTTP server was simplified. See the test app changes [in PR #387](https://github.com/readium/r2-testapp-kotlin/pull/387/files).

Replace `addEpub()` with `addPublication()`, which does not expect the publication filename anymore. If the `Publication` is servable, `addPublication()` will return its base URL. This means that you do not need to:

* Call `Publication.localBaseUrlOf()` to get the base URL. Use the one returned by `addPublication()` instead.
* Set the server port in the `$key-publicationPort` `SharedPreferences` property.
    * If you copied the `R2ScreenReader` from the test app, you will need to update it to use directly the base URL instead of the `$key-publicationPort` property. [See this commit](https://github.com/readium/r2-testapp-kotlin/pull/388/commits/a911967094bf6699b0ae3596002716414b2795f6).

`R2EpubActivity` and `R2AudiobookActivity` are expecting an additional `Intent` extra: `baseUrl`. Use the base URL returned by `addPublication()`.

### LCP changes

Find all the changes made in the test app related to LCP [in PR #379](https://github.com/readium/r2-testapp-kotlin/pull/379/files).

#### Replacing `org.joda.time.DateTime` with `java.util.Date`

We replaced all occurrences of Joda's `DateTime` with `java.util.Date` in `r2-lcp-kotlin`, to reduce the dependency on third-party libraries. You will need to update any code using `LcpLicense`. The easiest way would be to keep using Joda in your own app and create `DateTime` object from the `Date` ones. For example:

```kotlin
lcpLicense?.license?.issued?.let { DateTime(it) }
```

#### Revamped loan renew API

The API to renew an LCP loan got revamped to better support renewal through a web page. You will need to implement `LcpLicense.RenewListener` to coordinate the UX interaction.

##### For Material Design apps

If your application fits Material Design guidelines, you may use the provided `MaterialRenewListener` implementation directly. This will only work if your theme extends a `MaterialComponents` one, for example:

```xml
<style name="AppTheme" parent="Theme.MaterialComponents.Light.DarkActionBar">
```

`MaterialRenewListener` expects an `ActivityResultCaller` instance for argument. Any `ComponentActivity` or `Fragment` object can be used as `ActivityResultCaller`.

```kotlin
val activity: FragmentActivity

license.renewLoan(MaterialRenewListener(
    license = lcpLicense,
    caller = activity,
    fragmentManager = activity.supportFragmentManager
))
```

## [2.0.0-beta.1](https://github.com/readium/r2-testapp-kotlin/compare/2.2.0-alpha.2...2.2.0-beta.1)

The version 2.0.0-beta.1 is mostly stabilizing the new APIs and fixing existing bugs. We also upgraded the libraries to be compatible with Kotlin 1.4 and Gradle 4.1.

### Replacing `Format` by `MediaType`

To simplify the new format API, [we merged `Format` into `MediaType`](https://github.com/readium/architecture/pull/145) to offer a single interface. If you were using `Format`, you should be able to replace it by `MediaType` seamlessly.

### Replacing `File` by `FileAsset`

[`Streamer.open()` is now expecting an implementation of `PublicationAsset`](https://github.com/readium/architecture/pull/147) instead of an instance of `File`. This allows to open publications which are not represented as files on the device. For example a stream, an URL or any other custom structure.

Readium ships with a default implementation named `FileAsset` replacing the previous `File` type. The API is the same so you can just replace `File` by `FileAsset` in your project.

### Support for display cutouts

This new version is now compatible with [display cutouts](https://developer.android.com/guide/topics/display-cutout). However, [this is an opt-in feature](https://github.com/readium/r2-navigator-kotlin/pull/184). To support display cutouts, follow these instructions:

* **IMPORTANT**: You need to remove any `setPadding()` statement from your app in `UserSettings.kt`, if you copied it from the test app.
* If you embed a navigator fragment (e.g. `EpubNavigatorFragment`) yourself, you need to opt-in by [specifying the `layoutInDisplayCutoutMode`](https://developer.android.com/guide/topics/display-cutout#choose_how_your_app_handles_cutout_areas) of the host `Activity`.
* `R2EpubActivity` and `R2CbzActivity` automatically apply `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` to their window's `layoutInDisplayCutoutMode`.
* `PdfNavigatorFragment` is not yet compatible with display cutouts, because of limitations from the underlying PDF viewer.


## 2.0.0-alpha.2

The 2.0.0 introduces numerous new APIs in the Shared Models, Streamer and LCP libraries, which are detailed in the following proposals. We highly recommend skimming over the "Developer Guide" section of each proposal before upgrading to this new major version.

* [Format API](https://readium.org/architecture/proposals/001-format-api)
* [Composite Fetcher API](https://readium.org/architecture/proposals/002-composite-fetcher-api)
* [Publication Encapsulation](https://readium.org/architecture/proposals/003-publication-encapsulation)
* [Publication Helpers and Services](https://readium.org/architecture/proposals/004-publication-helpers-services)
* [Streamer API](https://readium.org/architecture/proposals/005-streamer-api)
* [Content Protection](https://readium.org/architecture/proposals/006-content-protection)

[This `r2-testapp-kotlin` commit](https://github.com/readium/r2-testapp-kotlin/commit/03c73bb67a1f3f72026d35675babec43e623c8d7) showcases all the changes required to upgrade the Test App.

[Please reach out on Slack](http://readium-slack.herokuapp.com/) if you have any issue migrating your app to Readium 2.0.0, after checking the [troubleshooting section](#troubleshooting).

### Replacing the Parsers with `Streamer`

A new `Streamer` class deprecates the use of individual `PublicationParser` implementations, which you will need to replace in your app.

#### Opening a Publication

Call `Streamer::open()` to parse a publication. It will return a self-contained `Publication` model which handles metadata, resource access and DRM decryption. This means that `Container`, `PubBox` and `DRM` are not needed anymore, you can remove any reference from your app.

The `allowUserInteraction` parameter should be set to `true` if you intend to render the parsed publication to the user. It will allow the Streamer to display interactive dialogs, for example to enter DRM credentials. You can set it to `false` if you're parsing a publication in a background process, for example during bulk import.

```kotlin
val streamer = Streamer(context)

val publication = streamer.open(File(path), allowUserInteraction = true)
    .getOrElse { error ->
        alert(error.getUserMessage(context))
        return
    }
```

#### Parsing a Readium Web Publication Manifest

You can't use `Publication.fromJSON()` to parse directly a manifest anymore. Instead, you can use `Manifest.fromJSON()`, which gives you access to the metadata embedded in the manifest.

Then, if you really need a `Publication` model, you can build one yourself from the `Manifest` and optionally a `Fetcher` and Publication Services.

```diff
-val publication = Publication.fromJSON(json)
+val publication = Manifest.fromJSON(json)?.let { Publication(it) }
```

However, the best way to parse a RWPM is to use the `Streamer`, like with any other publication format. This way the `Publication` model will be initialized with appropriate `Fetcher` and Publication Services.

#### Error Feedback

In case of failure, a `Publication.OpeningException` is returned. It implements `UserException` and can be used directly to present an error message to the user with `getUserMessage(Context)`.

If you wish to customize the error messages or add translations, you can override the strings declared in `r2-shared-kotlin/r2-shared/src/main/res/values/strings.xml` in your own app module. This goes for LCP errors as well, which are declared in `r2-lcp-kotlin/r2-lcp/src/main/res/values/strings.xml`.

#### Advanced Usage

`Streamer` offers other useful APIs to extend the capabilities of the Readium toolkit. Take a look at its documentation for more details, but here's an overview:

* Add new custom parsers.
* Integrated DRM support, such as LCP.
* Provide different implementations for third-party tools, e.g. ZIP, PDF and XML.
* Customize the `Publication`'s metadata or `Fetcher` upon creation.
* Collect authoring warnings from parsers.

### Accessing a Publication's Resources

#### `Container` is Deprecated

Since the new `Publication` model is self-contained, you can replace any use of the `Container` API by `publication.get(Link)`. This works for any publication format supported by the `Streamer`'s parsers.

The test app used to have special cases for DiViNa and Audiobooks, by unpacking manually the ZIP archives. You should remove this code and streamline any resource access using `publication.get()`.

#### Extracting Publication Covers

Extracting the cover of a publication for caching purposes can be done with a single call to `publication.cover()`, instead of reaching for a `Link` with `cover` relation. You can use `publication.coverFitting(Size)` to select the best resolution without exceeding a given size. It can be useful to avoid saving very large cover images.

```diff
-val cover =
-    try {
-        publication.coverLink
-            ?.let { container.data(it.href) }
-            ?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
-    } catch (e: Exception) {
-        null
-    }

+val cover = publication.coverFitting(Size(width = 100, height = 100))
```

### Observing a Navigator's Current Locator

`Navigator::currentLocator` is now a `StateFlow` instead of `LiveData`, to better support chromeless navigators such as an audiobook navigator in the future.

If you were observing `currentLocator` from an `Activity` or `Fragment`, you can continue to do so with `currentLocator.asLiveData()`.

```diff
- navigator.currentLocator.observe(this, Observer { locator -> })
+ navigator.currentLocator.asLiveData().observe(this, Observer { locator -> })
```

If you access directly the value through `navigator.currentLocator.value`, you might need to add the following annotation to the enclosing class:

```
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
```

Despite being still experimental, `StateFlow` [is deemed stable for use](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/#not-stable-for-inheritance).

### LCP and Other DRMs

#### Opening an LCP Protected Publication

Support for LCP is now fully integrated with the `Streamer`, which means that you don't need to retrieve the LCP license and fill `container.drm` yourself after opening a `Publication` anymore.

To enable the support for LCP in the `Streamer`, you need to initialize it with a `ContentProtection` implementation provided by `r2-lcp-kotlin`.

```kotlin
val lcpService = LcpService(context)
val streamer = Streamer(
    context = context,
    contentProtections = listOfNotNull(
        lcpService?.contentProtection()
    )
)
```

Then, to prompt the user for their passphrase, you need to set `allowUserInteraction` to `true` and provide the instance of the hosting `Activity`, `Fragment` or `View` with the `sender` parameter when opening the publication.

```kotlin
streamer.open(File(path), allowUserInteraction = true, sender = activity)
```

Alternatively, if you already have the passphrase, you can pass it directly to the `credentials` parameter. If it's valid, the user won't be prompted.

#### Customizing the Passphrase Dialog

The LCP Service now ships with a default passphrase dialog. You can remove the former implementation from your app if you copied it from the test app. But if you still want to use a custom implementation of `LcpAuthenticating`, for example to have a different layout, you can pass it when creating the `ContentProtection`.

```kotlin
lcpService.contentProtection(CustomLCPAuthentication())
```

#### Presenting a Protected Publication with a Navigator

In case the credentials were incorrect or missing, the `Streamer` will still return a `Publication`, but in a "restricted" state. This allows reading apps to import publications by accessing their metadata without having the passphrase.

But if you need to present the publication with a Navigator, you will need to first check if the `Publication` is not restricted.

Besides missing credentials, a publication can be restricted if the Content Protection returned an error, for example when the publication is expired. In which case, you must display the error to the user by checking the presence of a `publication.protectionError`.

```kotlin
if (publication.isRestricted) {
    publication.protectionError?.let { error ->
        // A status error occurred, for example the publication expired
        alert(error.getUserMessage(context))
    }
} else {
    presentNavigator(publication)
}
```

#### Accessing an LCP License Information

To check if a publication is protected with a known DRM, you can use `publication.isProtected`.

If you need to access an LCP license's information, you can use the helper `publication.lcpLicense`, which will return the `LcpLicense` if the publication is protected with LCP and the passphrase was known. Alternatively, you can use `LcpService::retrieveLicense()` as before.

#### Acquiring a Publication from an LCPL

`LcpService.importPublication()` was replaced with `acquirePublication()`, which is a cancellable suspending function. It doesn't require the user to enter its passphrase anymore to download the publication.

#### Supporting Other DRMs

You can integrate additional DRMs, such as Adobe ACS, by implementing the `ContentProtection` protocol. This will provide first-class support for this DRM in the Streamer and Navigator.

Take a look at the [Content Protection](https://readium.org/architecture/proposals/006-content-protection) proposal for more details. [An example implementation can be found in `r2-lcp-kotlin`](https://github.com/readium/r2-lcp-kotlin/blob/develop/r2-lcp/src/main/java/org/readium/r2/lcp/LcpContentProtection.kt).

### Introducing `Try`

A few of the new APIs are returning a `Try` object, which is similar to the [native `Result` type](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-result/). We decided to go for this opiniated approach for error handling instead of throwing `Exception` because of the type-safety it brings and the constraint on reading apps to properly handle error cases.

You can revert to traditional exceptions by calling `getOrThrow()` on the `Try` instance, but the most convenient way to handle the error would be to use `getOrElse()`.

```kotlin
val publication = streamer.open(File(path), allowUserInteraction = true)
    .getOrElse { error ->
        alert(error.getUserMessage(context))
        return
    }
```

`Try` also supports `map()` and `flatMap()` which are useful to transform the result while forwarding any error handling to upper layers.

```kotlin
fun cover(): Try<Bitmap, ResourceException> =
    publication.get(coverLink)
        .use { resource -> resource.read() } // <- returns a Try<ByteArray, ResourceException>
        .map { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
```

### Troubleshooting

#### Attempt to invoke virtual method '`android.content.SharedPreferences android.content.Context.getSharedPreferences(java.lang.String, int)`' on a null object reference

Make sure you create the `LcpService` *after* `onCreate()` has been called on an `Activity`.

#### LCP publications are blank or LCPL are not imported

Make sure you added the following to your app's `build.gradle`:

```
implementation "readium:liblcp:1.0.0@aar"
```

#### LCP publications are opening but not decrypted

Make sure you added the content protection to the Streamer, [following these instructions](#opening-an-lcp-protected-publication).

#### E/LcpDialogAuthentication: No valid [sender] was passed to `LcpDialogAuthentication::retrievePassphrase()`. Make sure it is an Activity, a Fragment or a View.

To be able to present the LCP passphrase dialog, the default `LcpDialogAuthentication` needs a hosting view as context. You must provide it to the `sender` parameter of `Streamer::open()`.

```kotlin
streamer.open(File(path), allowUserInteraction = true, sender = activity)
```

#### IllegalArgumentException: The provided publication is restricted. Check that any DRM was properly unlocked using a Content Protection.

Navigators will refuse to be opened if a publication is protected and not unlocked. You must check if a publication is not restricted by [following these instructions](#presenting-a-protected-publication-with-a-navigator).

## 2.0.0-alpha.1

With this new release, we started a process of modernization of the Readium Kotlin toolkit to:

* better follow Android best practices and Kotlin conventions,
* reduce coupling between reading apps and Readium, to ease future migrations and allow refactoring of private core implementations,
* increase code safety,
* unify Readium APIs across platforms [through public specifications](https://github.com/readium/architecture/tree/master/proposals).

As such, **this release will break existing codebases**. While most changes are facilitated thanks to deprecation warnings with automatic fixes, there are a few changes listed below that you will need to operate manually.

### Imports

* The `Publication` shared models were moved to their own package. While there are deprecated aliases helping with migration, it doesn't work for `Publication.EXTENSION`. Therefore, you need to replace all occurrences of `org.readium.r2.shared.Publication` by `org.readium.r2.shared.publication.Publication` in your codebase.
    * Or better, don't use `Publication.EXTENSION` anymore. [We have a new `Format` API which handles these needs in a more systematic way](https://github.com/readium/r2-testapp-kotlin/pull/314/files). And instead of file extensions, we recommend to store media types in your database.
* A few `Publication` and `Link` properties, such as `images`, `pageList` and `numberOfItems` were moved to a different package. Simply trigger the "Import" feature of your IDE to resolve them.

### Immutability of Shared Models

The `Publication` shared models are now immutable to increase code safety. This should not impact reading apps much unless you were creating `Publication` or other models yourself.

However, there are a few places in the Test App that needs to be updated:

* `Publication`'s `readingOrder`, `links`, and `tableOfContents` are not `MutableList` anymore, but read-only `List`. [Therefore, you need to update any code expecting mutable lists](https://github.com/readium/r2-testapp-kotlin/commit/b6dca6d2ac9c1d63493744d6c45f03cd5b915cc1#diff-20bbc2d6644af240cffe39c8f2a1e1d8L65).
* `Locator` can't be modified directly anymore. Instead, [use the `copy()` or `copyWithLocations()` `Locator` APIs](https://github.com/readium/r2-testapp-kotlin/commit/177f12533410d8334a3f3d187dc9acef194fa2ee#diff-a6a819ecd51536fea64e4ab0a85b58eeL835-L839).

### Last Read Location

Best practices on observing and restoring the last location were updated in the Test App, and it is **highly recommended** that you update your codebase as well, to avoid any issues.

#### Restoring the Last Location

You need to make these changes in your implementations of `EpubActivity`, `ComicActivity` and `AudiobookActivity`:

* [Remove any overrides of `currentLocation`](https://github.com/readium/r2-testapp-kotlin/commit/e79e9aa31a1f1b6516cd733ec9c5be7772923e00#diff-a6a819ecd51536fea64e4ab0a85b58eeL76-L87).
* [Restore the last location from your database in `onCreate()`](https://github.com/readium/r2-testapp-kotlin/commit/0f6137ff42d37f094c8de53abb982bdf1f2a0248#diff-330266947fef09416118fea00ffbfdf9R72-R75), for example with something similar to:

```kotlin
// Restores the last read location
bookRepository.lastLocatorOfBook(bookId)?.let { locator ->
    go(locator, animated = false)
}
```

#### Observing the Current Location

`NavigatorDelegate.locationDidChange()` is now deprecated in favor of the more idiomatic `Navigator.currentLocator: LiveData<Locator?>`.

```kotlin
currentLocator.observe(this, Observer { locator ->
    if (locator != null) {
        bookRepository.saveLastLocatorOfBook(bookId, locator)
    }
})
```

### `Publication`

* A new [Positions List](https://github.com/readium/architecture/tree/master/models/locators/positions) feature was added to provide a list of discrete locations in a publication. It can be used to implement an approximation of page numbers. This replaces the existing `R2SyntheticPageList`, which should be removed from your codebase.
* [Publications parsed from large manifests could crash the application](https://github.com/readium/r2-testapp-kotlin/issues/286) when starting a reading activity. To fix this, **`Publication` must not be put in an `Intent` extra anymore**. Instead, [use the new `Intent` extensions provided by Readium](https://github.com/readium/r2-testapp-kotlin/pull/303). This solution is a crutch until [we move away from `Activity` in the Navigator](https://github.com/readium/r2-navigator-kotlin/issues/115) and let reading apps handle the lifecycle of `Publication` themselves.
    * Replace all occurrences of `putExtra("publication", publication)` or similar by `putPublication(publication)`.
    * Replace all occurrences of `getSerializableExtra("publication")` or similar by `getPublication(this)`.

### `Locator`

* `Locator` is now `Parcelable` instead of `Serializable`, you must replace all occurrences of `getSerializableExtra("locator")` by `getParcelableExtra("locator")`.
* `Locations.fragment` was renamed to `fragments`, and is now a `List`. You need to update your code if you were creating `Locations` yourself.
* `locations` and `text` are not nullable anymore. `Locator`'s constructor has a default value, [so you don't need to pass `null` for them anymore](https://github.com/readium/r2-testapp-kotlin/commit/177f12533410d8334a3f3d187dc9acef194fa2ee#diff-20bbc2d6644af240cffe39c8f2a1e1d8L140).
* `Locator` is not meant to be subclassed, and extending it is not possible anymore. If your project is based on the Test App, [you need to do the following changes in your codebase](https://github.com/readium/r2-testapp-kotlin/commit/177f12533410d8334a3f3d187dc9acef194fa2ee#diff-354a03fd7ee2e678e865f6fff63e1963):
    * Don't extend `Locator` in `Bookmark` and `Highlight`. Instead, add a `locator` property which will create a `Locator` object from their properties. Then, [in places where you were creating a `Locator` from a database model](https://github.com/readium/r2-testapp-kotlin/commit/177f12533410d8334a3f3d187dc9acef194fa2ee#diff-a6a819ecd51536fea64e4ab0a85b58eeR858-R861), you can use this property directly.
    * For `SearchLocator`, you have two choices:
        * (Recommended) Replace all occurrences of `SearchLocator` by `Locator`. These two models are interchangeable.
        * Use the same strategy described above for `Bookmark`.

```kotlin
class Bookmark(...) {

    val locator get() = Locator(
        href = resourceHref,
        type = resourceType,
        title = resourceTitle,
        locations = location,
        text = locatorText
    )

}
```

### `Server`

The CSS, JavaScript and fonts injection in the `Server` was refactored to reduce the risk of collisions and simplify your codebase. **This is a breaking change**, [to upgrade your app you need to](https://github.com/readium/r2-testapp-kotlin/pull/321/files#diff-9bb6ad21df8b48f171ba6266616662ac):

* Provide the application's `Context` when creating a `Server`.
* Remove the following injection statements, which are now handled directly by the Streamer:

```kotlin
server.loadCustomResource(assets.open("scripts/crypto-sha256.js"), "crypto-sha256.js", Injectable.Script)   
server.loadCustomResource(assets.open("scripts/highlight.js"), "highlight.js", Injectable.Script)
```
