# Opening a publication

:warning: The described components are is still experimental.

In order to offer the best extensibility possible, Readium requires you to build different components before
you can actually open a publication. 

## Constructing an `AssetOpener`

First, you need to build an `AssetOpener`. It will enable you to read content through different schemes and guessing its format.
The constructor takes several components with different responsibilities.

* `ResourceFactory` determines which schemes you will be able to access content through.
* `ArchiveOpener` which kinds of archives your `AssetOpener` will be able to open.
* `FormatSniffer` which file formats your `AssetOpener` will be able to identify.

For each of these components, you can either use a single Readium-provided implementation, or implement yours
with the composite pattern. `CompositeResourceFactory`, `CompositeArchiveOpener` and `CompositeFormatSniffer`
provide simple implementations trying every item of a list in turns.

Though the default `ResourceFactory` is a `FileResourceFactory` supporting only the file `scheme` with,
Readium provides you with `ContentResourceFactory` and `HttpResourceFactory` to support the `content` and `http`
schemes respectively.

The only archive format supported by Readium is ZIP, with the default `ZipArchiveOpener`,
while a couple of formats including EPUB, PDF, CBZ and RPF can be identified by the `DefaultFormatSniffer`.

## Constructing a `PublicationOpener`

The component which can parse an `Asset` representing a publication to build a proper `Publication`
object is the `PublicationOpener`. It is also highly customizable through the parameters you pass
to its constructor.

* `parsers` and `ignoreDefaultParsers` allow you to override the default list of [PublicationParser] that will be used.
* `contentProtections` enables you to add support for different DRM schemes.


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