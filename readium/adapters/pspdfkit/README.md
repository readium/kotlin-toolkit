# Readium Adapter for [PSPDFKit](https://pspdfkit.com/)

[PSPDFKit](https://pspdfkit.com/) is a commercial library for rendering PDF documents. This adapter provides an implementation of Readium's `PdfDocument` for parsing a PDF publication, and of `PdfDocumentFragment` to render a PDF with `PdfNavigatorFragment`. LCP protected PDFs are fully supported.

If you are looking for a free, open source PDF adapter, take a look at [PdfiumAndroid](../pdfium).

## Setup

Follow the same setup instructions as the core Readium toolkit, then add this new dependency in your app's `build.gradle`.

```groovy
dependencies {
    implementation "com.github.readium.kotlin-toolkit:readium-adapter-pspdfkit:$readium_version"
    // Or, if you need only the parser but not the navigator:
    implementation "com.github.readium.kotlin-toolkit:readium-adapter-pspdfkit-document:$readium_version"
}
```

This adapter requires PSPDFKit's maven repository in your root `build.gradle`:

```groovy
allprojects {
    repositories {
        maven { url "https://customers.pspdfkit.com/maven" }
    }
}
```

Finally, follow PSPDFKit's instructions to add your license key in your app's `AndroidManifest.xml`.

## Parse a PDF into a Readium `Publication`.

To open a PDF publication with PSPDFKit, initialize the `Streamer` with the adapter factory: 

```kotlin
val streamer = Streamer(context,
    pdfFactory = PsPdfKitDocumentFactory(context)
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
            documentFragmentFactory = PsPdfKitDocumentFragment.createFactory(requireContext())
        )

    super.onCreate(savedInstanceState)
}
```

