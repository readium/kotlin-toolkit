# Opening a publication

:warning: The APIs described here may still undergo changes before the stable 3.0 release.

To open a publication with Readium, you need to instantiate a couple of components: an `AssetRetriever` and a `PublicationOpener`.

## `AssetRetriever`

The `AssetRetriever` grants access to the content of an asset located at a given URL, such as a publication package, manifest, or LCP license.

### Constructing an `AssetRetriever`

You can create an instance of `AssetRetriever` with:

* A `ContentResolver` to support data access through the `content` URL scheme.
* An `HttpClient` to enable the toolkit to perform HTTP requests and support the `http` and `https` URL schemes. You can use `DefaultHttpClient` which provides callbacks for handling authentication when needed.

```kotlin
val httpClient = DefaultHttpClient()
val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
```

### Retrieving an `Asset`

With your fresh instance of `AssetRetriever`, you can open an `Asset` from an `AbsoluteUrl`.

```kotlin
// From a `File`
val url = File("...").toUrl()
// or from a content:// `Uri`
val url = contentUri.toAbsoluteUrl()
// or from a raw URL string
val url = AbsoluteUrl("https://domain/book.epub")

val asset = assetRetriever.retrieve(url)
    .getOrElse { /* Failed to retrieve the Asset */ }
```

The `AssetRetriever` will sniff the media type of the asset, which you can store in your bookshelf database to speed up the process next time you retrieve the `Asset`. This will improve performance, especially with HTTP URL schemes.

```kotlin
val mediaType = asset.format.mediaType

// Speed up the retrieval with a known media type.
val asset = assetRetriever.retrieve(url, mediaType)
```

## `PublicationOpener`

`PublicationOpener` builds a `Publication` object from an `Asset` using:

* A `PublicationParser` to parse the asset structure and publication metadata.
    * The `DefaultPublicationParser` handles all the formats supported by Readium out of the box.
* An optional list of `ContentProtection` to decrypt DRM-protected publications.
    * If you support Readium LCP, you can get one from the `LcpService`.

```kotlin
val contentProtections = listOf(lcpService.contentProtection(authentication))

val publicationParser = DefaultPublicationParser(context, httpClient, assetRetriever, pdfFactory)

val publicationOpener = PublicationOpener(publicationParser, contentProtections)
```

### Opening a `Publication`

Now that you have a `PublicationOpener` ready, you can use it to create a `Publication` from an `Asset` that was previously obtained using the `AssetRetriever`.

The `allowUserInteraction` parameter is useful when supporting Readium LCP. When enabled and using a `LcpDialogAuthentication`, the toolkit will prompt the user if the passphrase is missing.

```kotlin
val publication = publicationOpener.open(asset, allowUserInteraction = true)
    .getOrElse { /* Failed to access or parse the publication */ }
```

## Supporting additional formats or URL schemes

`DefaultPublicationParser` accepts additional parsers. You also have the option to use your own parser list by using `CompositePublicationParser` or create your own `PublicationParser` for a fully customized parsing resolution strategy.

The `AssetRetriever` offers an additional constructor that provides greater extensibility options, using:

* `ResourceFactory` which handles the URL schemes through which you can access content.
* `ArchiveOpener` which determines the types of archives (ZIP, RAR, etc.) that can be opened by the `AssetRetriever`.
* `FormatSniffer` which identifies the file formats that `AssetRetriever` can recognize.

You can use either the default implementations or implement your own for each of these components using the composite pattern. The toolkit's `CompositeResourceFactory`, `CompositeArchiveOpener`, and `CompositeFormatSniffer` provide a simple resolution strategy.
