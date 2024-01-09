# Opening a publication

:warning: The described components are is still experimental.

Readium requires you to instantiate a few components before you can actually open a publication. 

## Constructing an `AssetOpener`

First, you need to instantiate an `HttpClient` to provide the toolkit the ability to do HTTP requests.
You can use the Readium `DefaultHttpClient` or a custom implementation. In the former case, its callback will
enable you to perform authentication when required.
Then, you can create an `AssetOpener` which will enable you to read content through different schemes and guessing its format.
In addition to an `HttpClient`, the `AssetOpener` constructor takes a `ContentResolver` to support data access through the `content` scheme.

```kotlin
val httpClient = DefaultHttpClient()

val assetOpener = AssetOpener(context.contentResolver, httpClient)
```

## Constructing a `PublicationOpener`

The component which can parse an `Asset` giving access to a publication to build a proper `Publication`
object is the `PublicationOpener`. Its constructor requires you to pass in:

* a `PublicationParser` it delegates the parsing work to.
* a list of `ContentProtection`s which will deal with DRMs.

The easiest way to get a `PublicationParser` is to use the `DefaultPublicationParser` class. As to
`ContentProtection`s, you can get one to support LCP publications through your `LcpService` if you have one.

```kotlin
val contentProtections = listOf(lcpService.contentProtection(authentication))

val publicationParser = DefaultPublicationParser(context, httpClient, assetOpener, pdfFactory)

val publicationOpener = PublicationOpener(publicationParser, contentProtections)
```

## Bringing the pieces together

Once you have got an `AssetOpener` and a `PublicationOpener`, you can eventually open a publication as follows:
```kotlin
val asset = assetOpener.open(url, mediaType)
  .getOrElse {  return error  }

val publication = publicationOpener.open(asset)
  .getOrElse { return error }
```

Persisting the asset media type on the device can significantly improve performance as it is valuable hint
for the content format, especially in case of remote publications.

## Extensibility`

`DefaultPublicationParser` accepts additional parsers. You can also use your own parser list
with `CompositePublicationParser` or implement [PublicationParser] in the way you like.

`AssetOpener` offers an alternative constructor providing better extensibility in a similar way.
This constructor takes several parameters with different responsibilities.

* `ResourceFactory` determines which schemes you will be able to access content through.
* `ArchiveOpener` which kinds of archives your `AssetOpener` will be able to open.
* `FormatSniffer` which file formats your `AssetOpener` will be able to identify.

For each of these components, you can either use the default implementations or implement yours
with the composite pattern. `CompositeResourceFactory`, `CompositeArchiveOpener` and `CompositeFormatSniffer`
provide simple implementations trying every item of a list in turns.

