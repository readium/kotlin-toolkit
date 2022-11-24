# Supporting PDF documents

The Readium toolkit relies on third-party PDF engines to parse and render PDF documents.

If you want to support PDF in your application, you need to explicitly enable one of the provided PDF adapters. Instructions for each adapter are available under the `readium/adapters` directory.

* `pdfium` - An adapter for the free and open source libraries [PdfiumAndroid](https://github.com/barteksc/PdfiumAndroid) and [AndroidPdfViewer](https://github.com/barteksc/AndroidPdfViewer).
* `pspdfkit` - An adapter for the commercial library [PSPDFKit](https://pspdfkit.com/).

The Test App currently showcases the `pdfium` adapter.

## Creating an adapter for a custom PDF engine

If you wish to integrate another third-party engine with Readium, you can create your own adapter. Each PDF adapter is split in two independent components:

* `document` which is required by the `Streamer` for parsing a PDF publication and extracting its metadata.
* `navigator` which can be used with the navigator's `PdfNavigatorFragment` to render a PDF document.

Take a look at the existing adapters for an example implementation. The `pspdfkit` one is the best example.

### `document` adapter

You will need to implement two interfaces from `readium-shared`:

* `PdfDocument` which represents a single PDF document and provides access to its metadata and cover.
* `PdfDocumentFactory` which is responsible for parsing a PDF document and creating instances of your `PdfDocument` implementation.

Then, you can provide your implementation to the `Streamer` during initialization:

```kotlin
val streamer = Streamer(context,
    pdfFactory = CustomDocumentFactory()
)

val publication = streamer.open(FileAsset(pdfFile)).getOrThrow()
```

### `navigator` adapter

If you want to render a PDF document with `readium-navigator`, you can use `PdfNavigatorFragment` which handles most of the grunt work.
You will still need to implement a few interfaces :

* `Configurable.Preferences` and `Configurable.Settings` which will deal with the settings supported by your PDF engine.
* `PdfDocumentFragment` which renders a single PDF document.
* `PreferencesEditor` which enables you to easily build a user interface for your preferences.
* `VisualNavigator.Presentation` which enables the navigator to get the information it needs about the current presentation, on the basis of the current settings or not.
* `PdfEngineProvider` which ties everything together for the use of the `PdfNavigatorFactory`.

Take care of calling the  `Listener` callbacks of `PdfDocumentFragment` when the user scrolls or interacts with the document and `PdfNavigatorFragment` will automatically send `Locator` progression events to the application.
The `input` parameter will contain:

* `publication` and `link`, to get access to the PDF resource using `publication.get(link)`.
* `initialPageIndex` which is the page index (from 0) that should be restored when creating the PDF view.
* `listener` which holds the callbacks.

Finally, provide an instance of your implementation of `PdfEngineProvider` when initializing the `PdfNavigatorFactory`:

```kotlin
val navigatorFactory = PdfNavigatorFactory(
    publication = publication,
    pdfEngineProvider = CustomPdfEngineProvider()
)
```
