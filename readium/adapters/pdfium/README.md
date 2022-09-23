# Readium Adapter for [PdfiumAndroid](https://github.com/barteksc/PdfiumAndroid) and [AndroidPdfViewer](https://github.com/barteksc/AndroidPdfViewer)

[PdfiumAndroid](https://github.com/barteksc/PdfiumAndroid) is an open source library for rendering PDF documents. This adapter provides an implementation of Readium's `PdfDocument` for parsing a PDF publication, and of `PdfDocumentFragment` to render a PDF with `PdfNavigatorFragment`. LCP protected PDFs are supported, although bound to available device memory.

PdfiumAndroid is free and performant, but has a number of downsides:

* It is currently unmaintained.
* LCP protected PDFs are limited by the device memory. In practice, this library seems to fail to render a `.lcpdf` of ~40 MB.

A better alternative when possible is to use the commercial library [PSPDFKit](../pspdfkit).

## Setup

Follow the same setup instructions as the core Readium toolkit, then add this new dependency in your app's `build.gradle`.

```groovy
dependencies {
    implementation "com.github.readium.kotlin-toolkit:readium-adapter-pdfium:$readium_version"
    // Or, if you need only the parser but not the navigator:
    implementation "com.github.readium.kotlin-toolkit:readium-adapter-pdfium-document:$readium_version"
}
```

## Parse a PDF into a Readium `Publication`.

To open a PDF publication with PdfiumAndroid, initialize the `Streamer` with the adapter factory: 

```kotlin
val streamer = Streamer(context,
    pdfFactory = PdfiumDocumentFactory(context)
)

val publication = streamer.open(FileAsset(pdfFile)).getOrThrow()
```

## Render a PDF with Readium's `PdfNavigatorFragment`.

To render the PDF using Readium's `PdfNavigatorFragment`, use:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    childFragmentManager.fragmentFactory =
        PdfNavigatorFragment.createFactory(
            publication = publication,
            ...
            documentFragmentFactory = PdfiumDocumentFragment.createFactory()
        )

    super.onCreate(savedInstanceState)
}
```

You can also configure `AndroidPdfViewer`'s view manually by providing a listener:

```kotlin
documentFragmentFactory = PdfiumDocumentFragment.createFactory(
    listener = object : PdfiumDocumentFragment.Listener {
        override fun onConfigurePdfView(configurator: PDFView.Configurator) {
            configurator.spacing(30)
        }
    }
)
```

