# Supporting Readium LCP

You can use the Readium Kotlin toolkit to download and read publications that are protected with the [Readium LCP](https://www.edrlab.org/readium-lcp/) DRM.

:point_up: To use LCP with the Readium toolkit, you must first obtain the `liblcp` private library by contacting [EDRLab](https://www.edrlab.org/contact/).

## Overview

An LCP publication is protected with a *user passphrase* and distributed using an LCP License Document (`.lcpl`) .

The user flow typically goes as follows:

1. The user imports a `.lcpl` file into your application.
2. The application uses the Readium toolkit to download the protected publication from the `.lcpl` file to the user's bookshelf. The downloaded file can be a `.epub`, `.lcpdf` (PDF), or `.lcpa` (audiobook) package.
3. The user opens the protected publication from the bookshelf.
4. If the passphrase isn't already recorded in the `readium-lcp` internal database, the user will be asked to enter it to unlock the contents.
5. The publication is decrypted and rendered on the screen.

## Setup

To support LCP in your application, you require two components:

* The `readium-lcp` module from the toolkit provides APIs for downloading and decrypting protected publications. Import it as you would any other Readium module, such as `readium-navigator`.
* The private `liblcp` library customized for your application [is available from EDRLab](https://www.edrlab.org/contact/). They will provide instructions for integrating the `liblcp` library into your application.

### File formats

Readium LCP specifies new file formats.

| Name                                                                                              | File extension | Media type                                      |
|---------------------------------------------------------------------------------------------------|----------------|-------------------------------------------------|
| [License Document](https://readium.org/lcp-specs/releases/lcp/latest.html#32-content-conformance) | `.lcpl`        | `application/vnd.readium.lcp.license.v1.0+json` |
| [LCP for PDF package](https://readium.org/lcp-specs/notes/lcp-for-pdf.html)                       | `.lcpdf`       | `application/pdf+lcp`                           |
| [LCP for Audiobooks package](https://readium.org/lcp-specs/notes/lcp-for-audiobooks.html)         | `.lcpa`        | `application/audiobook+lcp`                     |

:point_up: EPUB files protected by LCP are supported without a special file extension or media type because EPUB accommodates any DRM scheme in its specification.

You may want to register these new file extensions and media types in the [intent filters](https://developer.android.com/guide/components/intents-filters) of your `AndroidManifest.xml`.

## Initializing the `LcpService`

`readium-lcp` offers an `LcpService` object that exposes its API. If `liblcp` is not configured correctly in your application, the constructor will return `null`. This is helpful if your application has build variants without LCP.

```kotlin
val lcpService = LcpService(
    context = context,
    assetRetriever = AssetRetriever(
        contentResolver = context.contentResolver,
        httpClient = DefaultHttpClient()
    )
) ?: error("liblcp is missing on the classpath")))
```

## Acquiring a publication from a License Document (LCPL)

Users need to import a License Document into your application to download the protected publication (`.epub`, `.lcpdf`, or `.lcpa`).

The `LcpService` offers an API to retrieve the full publication from an LCPL on the filesystem.

```kotlin
let acquisition = lcpService.acquirePublication(
    lcpl = File("path/to/license.lcpl"),
    onProgress = { progress ->
        print(String.format("Downloaded %.1f%%", progress * 100)
    }
).getOrElse { /* Failed to download the protected publication */ }

print("The publication was downloaded at ${acquisition.localFile}, its type is ${acquisition.format.mediaType}.")
```

After the download is completed, import the `acquisition.localFile` file into the bookshelf like any other publication file.

### Using a custom download manager

If you want more control over the acquisition process, you can download the publication manually instead.

The acquisition is done in three steps:

1. Parse the License Document (LCPL) file.
2. Download the protected publication.
3. Inject the LCPL into the downloaded package.

```kotlin
val lcplBytes: ByteArray = ...

val licenseDocument = LicenseDocument.fromBytes(lcplBytes)
    .getOrElse { /* The LCPL appears to be invalid */ }


val publicationLink = licenseDocument.publicationLink

val downloadedFile = yourDownloadService.download(publicationLink.url())
    .getOrElse { /* Failed to download the protected publication */ }

lcpService.injectLicenseDocument(licenseDocument, downloadedFile)
    .getOrElse { /* Failed to inject the LCPL in the downloaded package */ }

// The downloaded file is now ready to be imported in your bookshelf as usual.
```

## Opening a publication protected with LCP

### Initializing the `PublicationOpener`

A publication protected with LCP can be opened using the `PublicationOpener` component, just like a non-protected publication. However, you must provide a [`ContentProtection`](https://readium.org/architecture/proposals/006-content-protection.html) implementation when initializing the `PublicationOpener` to enable LCP. Luckily, `LcpService` has you covered.

```kotlin
val authentication = LcpDialogAuthentication()

val publicationOpener = PublicationOpener(
    publicationParser = DefaultPublicationParser(),
    contentProtections = listOf(
        lcpService.contentProtection(authentication)
    )
)
```

An LCP package is secured with a *user passphrase* for decrypting the content. The `LcpAuthenticating` interface expected by `LcpService.contentProtection()` provides the passphrase when needed. You can use the default `LcpDialogAuthentication` which displays a pop-up to enter the passphrase, or implement your own method for passphrase retrieval. If you already fetched the passphrase from a backend server, you can also use `LcpPassphraseAuthentication(passphrase)`.

:point_up: The user will be prompted once per passphrase since `readium-lcp` stores known passphrases on the device. 

### Setting up the `LcpDialogAuthentication`

For `LcpDialogAuthentication` to function correctly, it needs to identify the host view displaying the dialog. You must indicate the host view, for example using a `View.OnAttachStateChangeListener` in your bookshelf fragment.

```kotlin
class MyFragment : Fragment {

    private inner class OnViewAttachedListener(
        private val authentication: LcpDialogAuthentication
    ) : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) {
            authentication.onParentViewAttachedToWindow(view)
        }

        override fun onViewDetachedFromWindow(view: View) {
            authentication.onParentViewDetachedFromWindow()
        }
    }

    private val onViewAttachedListener: OnViewAttachedListener = OnViewAttachedListener(
        // Use your shared instance here.
        lcpDialogAuthentication
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.addOnAttachStateChangeListener(onViewAttachedListener)
    }
}
```

### Opening the publication

You are now ready to open the publication file with your `PublicationOpener` instance.

```kotlin
// Retrieve an `Asset` to access the file content.
val url = File("/path/to/lcp-protected-book.epub").toUrl()
val asset = assetRetriever.retrieve(url)
    .getOrElse { /* Failed to retrieve the Asset */ }
 
// Open a `Publication` from the `Asset`.
val publication = publicationOpener.open(asset, allowUserInteraction = true)
    .getOrElse { /* Failed to access or parse the publication */ }
```

The `allowUserInteraction` argument is forwarded to the `LcpAuthenticating` implementation when the passphrase is unknown. `LcpDialogAuthentication` shows a pop-up only if `allowUserInteraction` is `true`.

When importing the publication to the bookshelf, set `allowUserInteraction` to `false` as you don't need the passphrase for accessing the publication metadata and cover. If you intend to present the publication using a Navigator, set `allowUserInteraction` to `true` as decryption will be required.

:point_up: To check if a publication is protected with LCP before opening it, you can use `asset.format.conformsTo(Specification.Lcp)` on the `Asset` returned by the `AssetRetriever`.

### Using the opened `Publication`

After obtaining a `Publication` instance, you can access the publication's metadata to import it into the user's bookshelf. The user passphrase is not needed for reading the metadata or cover.

However, if you want to display the publication with a Navigator, verify it is not restricted. It could be restricted if the user passphrase is unknown or if the license is no longer valid (e.g., expired loan, revoked purchase, etc.).

```kotlin
if (publication.isRestricted) {
    val error = publication.protectionError
    if (error != null) {
        // The user is not allowed to open the publication. You should display the error.
        // In the case of LCP, `error` will be an `LcpError`.
    } else {
        // We don't have the user passphrase.
        // You may use `publication` to access its metadata, but not to render its content.
    }
} else {
    // The publication is not restricted, you may render it with a Navigator component.
}
```

## Streaming an LCP protected package

If the server hosting the LCP protected package supports [HTTP Range requests](https://httpwg.org/specs/rfc7233.html), it is possible to stream directly an LCP protected publication from a License Document (`.lcpl`) file, without downloading the whole publication first.

Simply open the License Document directly using the `PublicationOpener`. Make sure you provide an `HttpClient` (or an `HttpResourceFactory` for additional customization) to the `AssetRetriever`.

```kotlin
// Instantiate the required components.
val httpClient = DefaultHttpClient()

val assetRetriever = AssetRetriever(
    contentResolver = context.contentResolver,
    httpClient = httpClient
)

val lcpService = LcpService(
    context = context,
    assetRetriever = assetRetriever
) ?: error("liblcp is missing on the classpath")))

val authentication = LcpDialogAuthentication()

val publicationOpener = PublicationOpener(
    publicationParser = DefaultPublicationParser(
        context,
        httpClient = httpClient,
        assetRetriever = assetRetriever
    ),
    contentProtections = listOf(
        lcpService.contentProtection(authentication)
    )
)

// Retrieve an `Asset` to access the LCPL content.
val url = File("/path/to/license.lcpl").toUrl()
val asset = assetRetriever.retrieve(url)
    .getOrElse { /* Failed to retrieve the Asset */ }
 
// Open a `Publication` from the LCPL `Asset`.
val publication = publicationOpener.open(asset, allowUserInteraction = true)
    .getOrElse { /* Failed to access or parse the publication */ }
    
print("Opened ${publication.metadata.title}")
```

## Obtaining information on an LCP license

An LCP License Document contains metadata such as its expiration date, the remaining number of characters to copy and the user name. You can access this information using an `LcpLicense` object.

Use the `LcpService` to retrieve the `LcpLicense` instance for a publication.

```kotlin
// Retrieve an `Asset` to access the file content.
val url = File("/path/to/lcp-protected-book.epub").toUrl()
val asset = assetRetriever.retrieve(url)
    .getOrElse { /* Failed to retrieve the Asset */ }

if (!asset.format.conformsTo(Specification.Lcp)) {
    // Not protected with LCP.
}

val lcpLicense = lcpService.retrieveLicense(
    asset = asset,
    authentication = authenticaton,
    allowUserInteraction = true
).getOrElse { /* Failed to retrieve the LCP License from the publication */ }

lcpLicense.license.user.name?.let { name ->
    print("The publication was acquired by $user")
}

lcpLicense.license.rights.end?.let { endDate ->
    print("The loan expires on $endDate")
}

lcpLicense.charactersToCopyLeft?.let { copyLeft ->
    print("You can copy up to $copyLeft characters remaining.")
}
```

:point_up: If you have already opened a `Publication` with the `PublicationOpener`, you can directly obtain the `LcpLicense` using `publication.lcpLicense`.

## Managing a loan

Readium LCP allows borrowing publications for a specific period. Use the `LcpLicense` object to manage a loan and retrieve its end date using `lcpLicense.license.rights.end`.

### Returning a loan

Some loans can be returned before the end date. You can confirm this by using `lcpLicense.canReturnPublication`. To return the publication, execute:

```kotlin
lcpLicense.returnPublication()
    .onFailure { /* Failed to return the publication */ }
```

### Renewing a loan

The loan end date may also be extended. You can confirm this by using `lcpLicense.canRenewLoan`.

Readium LCP supports [two types of renewal interactions](https://readium.org/lcp-specs/releases/lsd/latest#35-renewing-a-license):

* Programmatic: You show your own user interface.
* Interactive: You display a web view, and the Readium LSD server manages the renewal for you.

You need to support both interactions by implementing the `LcpLicense.RenewListener` interface. A default Material Design implementation is available with `MaterialRenewListener`.

```kotlin
val renewListener = MaterialRenewListener(
    license = lcpLicense,
    caller = hostFragment,
    fragmentManager = hostFragment.childFragmentManager
)

lcpLicense.renewLoan(renewListener)
    .onFailure { /* Failed to extend the loan end date */ }
```

## Handling `LcpError`

The APIs may fail with an `LcpError`. These errors **must** be displayed to the user with a suitable message.

For an example, take a look at [`LcpUserError.kt`](../../test-app/src/main/java/org/readium/r2/testapp/domain/LcpUserError.kt) in the Test App.
