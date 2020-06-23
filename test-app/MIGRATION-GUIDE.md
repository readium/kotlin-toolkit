# Migration Guide

[unreleased]: https://github.com/readium/r2-testapp-kotlin/compare/master...HEAD
[x.x.x]: https://github.com/readium/r2-testapp-kotlin/compare/V2.1.0-beta.4...x.x.x

All migration steps necessary in reading apps to upgrade to major versions of the Readium toolkit will be documented in this file.

## [Unreleased]

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