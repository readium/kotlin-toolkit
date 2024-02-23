# Getting started

The Readium Kotlin toolkit enables you to develop reading apps for Android and ChromeOS. It provides built-in support for multiple publication formats such as EPUB, PDF, audiobooks, and comics.

:warning: Readium offers only low-level tools. You are responsible for creating a user interface for reading and managing books, as well as a data layer to store the user's publications. The Test App is an example of such integration.

The toolkit is divided into separate modules that can be used independently.

### Main modules

* `readium-shared` contains shared `Publication` models and utilities.
* `readium-streamer` parses publication files (e.g. an EPUB) into a `Publication` object.
* [`readium-navigator` renders the content of a publication](navigator/navigator.md).
    * [`readium-navigator-media-audio` renders audiobooks](navigator/media-navigator.md)
    * [`readium-navigator-media-tts` renders publication with a text-to-speech engine](tts.md)

### Specialized packages

* `readium-opds` parses [OPDS catalog feeds](https://opds.io) (both OPDS 1 and 2).
* [`readium-lcp` downloads and decrypts LCP-protected publications](lcp.md).

### Adapters to third-party dependencies

* `readium-adapter-exoplayer` provides an [ExoPlayer](https://exoplayer.dev) adapter for the [`AudioNavigator`](navigator/media-navigator.md).
* [`readium-adapter-pdfium`](../../readium/adapters/pdfium/README.md) provides a [Pdfium](https://github.com/barteksc/AndroidPdfViewer) adapter for the [PDF Navigator](pdf.md).
* [`readium-adapter-pspdfkit`](../../readium/adapters/pspdfkit/README.md) provides a [PSPDFKit](https://pspdfkit.com) adapter for the [PDF Navigator](pdf.md).

## Overview of the shared models (`readium-shared`)

The Readium toolkit provides models used as exchange types between packages.

### Publication

`Publication` and its sub-components represent a single publication – ebook, audiobook or comic. It is loosely based on the [Readium Web Publication Manifest](https://readium.org/webpub-manifest/).

A `Publication` instance:

* holds the metadata of a publication, such as its author or table of contents,
* allows to read the contents of a publication, e.g. XHTML or audio resources,
* provides additional services, for example content extraction or text search

### Link

A [`Link` object](https://readium.org/webpub-manifest/#24-the-link-object) holds a pointer (URL) to a `Publication` resource along with additional metadata, such as its media type or title.

The `Publication` contains several `Link` collections, for example:

* `readingOrder` lists the publication resources arranged in the order they should be read.
* `resources` contains secondary resources necessary for rendering the `readingOrder`, such as an image or a font file.
* `tableOfContents` represents the table of contents as a tree of `Link` objects.

### Locator

A [`Locator` object](https://readium.org/architecture/models/locators/) represents a precise location in a publication resource in a format that can be stored and shared across reading systems. It is more accurate than a `Link` and contains additional information about the location, e.g. progression percentage, position or textual context.

`Locator` objects are used for various features, including:

* reporting the current progression in the publication
* saving bookmarks, highlights and annotations
* navigating search results

## Opening a publication (`readium-streamer`)

To retrieve a `Publication` object from a publication file like an EPUB or audiobook, you can use an `AssetRetriever` and `PublicationOpener`.

```kotlin
// Instantiate the required components.
val httpClient = DefaultHttpClient()
val assetRetriever = AssetRetriever(
    contentResolver = context.contentResolver,
    httpClient = httpClient
)
val publicationOpener = PublicationOpener(
    publicationParser = DefaultPublicationParser(
        context,
        httpClient = httpClient,
        assetRetriever = assetRetriever,
        pdfFactory = PdfiumDocumentFactory(context)
    )
)

// Retrieve an `Asset` to access the file content.
val url = File("/path/to/book.epub").toUrl()
val asset = assetRetriever.retrieve(url)
    .getOrElse { /* Failed to retrieve the Asset */ }
 
// Open a `Publication` from the `Asset`.
val publication = publicationOpener.open(asset, allowUserInteraction = true)
    .getOrElse { /* Failed to access or parse the publication */ }
    
print("Opened ${publication.metadata.title}")
```

The `allowUserInteraction` parameter is useful when supporting a DRM like Readium LCP. It indicates if the toolkit can prompt the user for credentials when the publication is protected.

[See the dedicated user guide for more information](open-publication.md).

## Accessing the metadata of a publication

After opening a publication, you may want to read its metadata to insert a new entity into your bookshelf database, for instance. The `publication.metadata` object contains everything you need, including `title`, `authors` and the `published` date.

You can retrieve the publication cover using `publication.cover()`. Avoid calling this from the main thread to prevent blocking the user interface.

## Rendering the publication on the screen (`readium-navigator`)

You can use a Readium navigator to present the publication to the user. The `Navigator` renders resources on the screen and offers APIs and user interactions for navigating the contents.

Please refer to the [Navigator guide](navigator/navigator.md) for more information.