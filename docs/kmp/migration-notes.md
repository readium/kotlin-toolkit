# KMP migration — breaking changes log

Raw log of public-API breaking changes, appended phase by phase as the migration progresses. Phase 09 assembles the final migration guide from this file.

## Phase 02 — Url & MediaType

### `Url` is now backed by uri-kmp and lives in `commonMain`

- `org.readium.r2.shared.util.Url` (and `AbsoluteUrl`, `RelativeUrl`) no longer wraps `android.net.Uri`; it is backed by [uri-kmp](https://github.com/eygraber/uri-kmp) (`com.eygraber.uri.Uri`) and is available from common multiplatform code. Package and API surface are unchanged, except for the conversion helpers listed below.
- URL string validation no longer relies on `java.net.URI`. It now accepts any string made of the RFC 3986 character set (plus well-formed percent escapes) and still rejects whitespace and non-ASCII characters. Some malformed URLs that `java.net.URI` used to reject (e.g. structurally invalid authorities) may now parse successfully; this is a leniency increase only.
- `Url.query` no longer uses `android.net.UrlQuerySanitizer`; query parameters are split on `&`/`=` and percent-decoded. Two deliberate divergences, aligned with the WHATWG URL specification: `+` is no longer decoded as a space, and `;` is no longer treated as a parameter separator (`?a=1;b=2` is a single parameter). Locked by `UrlTest.queryParametersAreOnlySeparatedByAmpersands`.
- `Url.resolve`/`Url.relativize`/`Url.normalize` no longer delegate to `java.net.URI` and `java.io.File.normalize()`; they use a pure-Kotlin implementation. Behavior is identical for every case covered by the `UrlTest`/`HrefTest` parity suites (the phase-02 acceptance criterion), with the following **deliberate divergences** in corners the old implementation handled per known JDK quirks. The new behavior follows RFC 3986 §5.2 and the WHATWG URL specification, and is locked by characterization tests in `UrlTest`:
  - Resolving a query-only reference keeps the whole base path: `http://e/foo/bar` + `?x=1` → `http://e/foo/bar?x=1` (JDK resolved against the parent directory: `http://e/foo/?x=1`, dropping the filename — JDK-6791060). The RFC behavior is also what publications expect from a `?page=2` HREF.
  - Dot segments of an absolute-path (or authority-carrying) reference are removed: `http://e/foo/bar` + `/quz/../baz` → `http://e/baz` (JDK kept them literally).
  - `Url.relativize` now behaves like the JRE `java.net.URI.relativize` **on device too**. Previously unit tests exercised the JRE variant while devices ran Android libcore's variant (which does not append `/` to a base missing it) — a divergence the old code acknowledged in a comment. There is now a single implementation, matching the behavior the test suite always specified.

### JVM/Android conversion helpers moved to `androidMain` (`util/UrlAndroid.kt`)

Still in `org.readium.r2.shared.util`, but now Android-only extension functions. Android/JVM callers only need to add an import for:

- `AbsoluteUrl.toFile(): File?` — **was a member of `AbsoluteUrl`**, now an extension; call sites need `import org.readium.r2.shared.util.toFile`.
- `File.toUrl(isDirectory: Boolean): AbsoluteUrl`
- `android.net.Uri.toUrl()`, `Uri.toAbsoluteUrl()`, `Uri.toRelativeUrl()`
- `Url.toUri(): android.net.Uri` — now re-parses the URL string instead of returning the wrapped `android.net.Uri`.
- `Url.toURI(): java.net.URI` (`@InternalReadiumApi`)
- `java.net.URL.toUrl()`, `URL.toAbsoluteUrl()`, `URL.toRelativeUrl()`
- `java.net.URI.toUrl()`

### Other relocations to `commonMain` (no API change)

- `org.readium.r2.shared.publication.Href`
- `org.readium.r2.shared.util.URITemplate` (internal)
- `org.readium.r2.shared.extensions`: `addPrefix`, `addSuffix`, `toInstant`, `tryOrNull`, `tryOr`, `tryOrLog` (the `Throwable.findInstance` helpers stay Android-only)

### `MediaType` moved to `commonMain`

- `org.readium.r2.shared.util.mediatype.MediaType` is now common code with an unchanged API, except:
  - `MediaType.charset: java.nio.charset.Charset?` — **was a member**, now an Android-only extension in `androidMain` (`util/mediatype/MediaTypeAndroid.kt`); call sites need `import org.readium.r2.shared.util.mediatype.charset`. It also no longer throws for an unknown charset name: it returns null instead.
  - The canonicalization of the `charset` parameter goes through an expect/actual helper: on Android it still uses `java.nio.charset.Charset.forName()`; on iOS a fixed table of common IANA names and aliases is used, so exotic aliases may stay un-canonicalized (uppercased as-is) there.
- `org.readium.r2.shared.util.format.Format` (with `FormatSpecification` and `Specification`) moved to `commonMain`, unchanged.
- The sniffing infrastructure (`Sniffing.kt`, `Sniffers.kt`) stays in `androidMain` for now; it moves to `commonMain` in later phases together with `util/data`, JSON and XML.

## Phase 03 — JSON: org.json → kotlinx.serialization JsonElement

### The `JSONable` contract now uses kotlinx.serialization types

- `org.readium.r2.shared.JSONable.toJSON()` now returns `kotlinx.serialization.json.JsonObject` instead of `org.json.JSONObject`, and `List<JSONable>.toJSON()` returns `JsonArray`. This is a repo-wide, non-bridgeable break: every `toJSON()`/`fromJSON()` surface in `readium-shared`, `readium-opds`, `readium-lcp`, `readium-navigator` and the test app was flipped in the same change.
- All `fromJSON(...)` companion parsers now take `kotlinx.serialization.json.JsonObject?`/`JsonArray?`/`JsonElement?` instead of `org.json.JSONObject?`/`JSONArray?`/`Any?`. Parsers that used to accept `Any?` (e.g. `Contributor.fromJSON`, `Subject.fromJSON`, `LocalizedString.fromJSON`, `PublicationCollection.fromJSON`, `Accessibility.fromJSON`, `Tdm.fromJSON`) now accept `JsonElement?`; pass a `JsonPrimitive` where you used to pass a `String`.
- `grep "org.json" readium/shared/src` returns nothing: org.json is gone from the toolkit.

### JSON helper mapping (old `extensions/JSON.kt` → new `util/json/Json.kt`, commonMain)

The `org.json` helper extensions (`@InternalReadiumApi`) were ported to `org.readium.r2.shared.util.json` with equivalents operating on `Map<String, JsonElement>` (which both `JsonObject` and the `MutableMap` copies used by parsers implement):

| Old (`org.readium.r2.shared.extensions`, on `JSONObject`) | New (`org.readium.r2.shared.util.json`) |
|---|---|
| `optString` / `optBoolean` / `optInt` / `optLong` / `optDouble` | same names, on `Map<String, JsonElement>` |
| `optNullableString/Boolean/Int/Long/Double` | same names |
| `optPositiveInt` / `optPositiveDouble` | same names |
| `optStringsFromArrayOrSingle` | same name |
| `optJSONObject` / `optJSONArray` (org.json members) | `optJsonObject` / `optJsonArray` |
| `remove = true` variants | overloads on `MutableMap<String, JsonElement>`; call `json.toMutableMap()` first |
| `toMap()` / `toList()` | `JsonObject.toMap()` / `JsonArray.toList()` |
| `putIfNotEmpty(name, …)` (mutation on `JSONObject`) | `putIfNotEmpty(name, …)` on `kotlinx.serialization.json.JsonObjectBuilder` (use `buildJsonObject { … }`) |
| `put(name, nullableValue)` (org.json dropped nulls) | `putIfNotNull(name, value)` on `JsonObjectBuilder` |
| `JSONObject(map)` | `putAll(map)` on `JsonObjectBuilder`, or `wrapJson(value)` |
| `mapNotNull`, `filterIsInstance`, `parseObjects` | stdlib `mapNotNull`/`filterIsInstance` on `JsonArray` (it is a `List<JsonElement>`); `parseObjects` kept |
| `String.toJsonOrNull()` (internal) | `String.toJsonObjectOrNull()` (+ `toJsonArrayOrNull`, `toJsonElementOrNull`) |

Parsing behavior notes:

- Parsing goes through a lenient `Json` configuration (`LenientJson`: `isLenient`, `allowTrailingComma`, `allowComments`). Unlike org.json, single-quoted strings are **not** supported, and trailing garbage after the top-level value is rejected. Unquoted literals are accepted and coerced back to strings by the string accessors (unless they are booleans or numbers), like org.json.
- `toMap()`/`toList()` unwrap numbers to `Int` when they fit, then `Long`, then `Double` (org.json used `Int`/`Long`/`Double` depending on the accessor). Null values are skipped.
- `putAll`/`Map.toJsonObject()` keep empty nested objects and arrays (like the legacy `JSONObject(Map)` conversion), while `putIfNotEmpty` drops them (like the legacy `wrapJSON`).
- In the RWPM serializers backed by an extension map (`Metadata.otherMetadata`, `Locator.Locations.otherLocations`, `LocatorCollection.Metadata.otherMetadata`), the keys handled by the serializer always shadow — or remove, when the corresponding property is null — a value with the same key in the extension map, like with org.json's `put(name, null)`.

### `WarningLogger` / `JsonWarning` moved to commonMain

- `JsonWarning.modelClass` is now a `kotlin.reflect.KClass<*>` (was `java.lang.Class<*>`), and `JsonWarning.json` a `kotlinx.serialization.json.JsonObject?`. The `WarningLogger.log(modelClass, reason, json, severity)` helper takes a `KClass<*>`: call it with `Foo::class` instead of `Foo::class.java`.

### Parcelers

- `org.readium.r2.shared.extensions.JSONParceler` was replaced with `org.readium.r2.shared.util.json.JsonMapParceler`, a common expect/actual `Parceler<Map<String, Any>>` backed by kotlinx serialization on Android and a no-op on iOS.
- `org.readium.r2.shared.util.InstantParceler` is now a common expect/actual object (moved out of `util/Instant.kt`).
- `org.readium.r2.shared.util.Parceler`, `TypeParceler` and `WriteWith` are now common expect declarations (actual typealiases to kotlinx.parcelize on Android; no-op declarations on iOS).

### HTTP helpers

- `HttpClient.fetchJSONObject()` now returns `HttpTry<kotlinx.serialization.json.JsonObject>`.
- `ByteArray.decodeJson()` returns `Try<JsonObject, DecodeError>`; `JSONObject.decodeRwpm()` is now `JsonObject.decodeRwpm()`.
- `HttpError.ErrorResponse.problemDetails` parses with kotlinx; `ProblemDetails.fromJSON` takes a `JsonObject`.
- `Manifest.toString()` no longer escapes forward slashes (org.json serialized `/` as `\/`).

### LCP models

- `LicenseDocument.json`, `StatusDocument.json`, and the `json` properties of the LCP components (`Link`s, `Rights`, `User`, `Signature`, `Encryption`, `ContentKey`, `UserKey`, `Event`, `PotentialRights`) are now `kotlinx.serialization.json.JsonObject`/`JsonArray`. `Rights.extensions` and `User.extensions` are `JsonObject`.
- Garbage-in edge case: a JSON `null` value for a required string field (e.g. `"algorithm": null`) used to be coerced by Android's org.json `getString` to the literal string `"null"`; it now yields an empty string `""`. Both are invalid inputs; no behavioral guarantee is attached to either.

### Relocations to `commonMain` (no API change unless noted)

Moved with unchanged package names: `JSONable`, `util/logging/WarningLogger.kt`, and in `publication/`: `ReadingProgression` (uppercase keys still accepted, now via invariant lowercasing), `LocalizedString` (default-locale fallback now uses a multiplatform `defaultLanguageTag()`), `Layout`, `Page`, `epub/EpubLayout`, `epub/Properties`, `encryption/Encryption`, `encryption/Properties`, `presentation/Presentation`, `presentation/Properties`, `Accessibility`, `Tdm`, `Properties`, `Link`, `Contributor`, `Subject`, `Collection`, `PublicationCollection`, `Locator` (+ `LocatorCollection`), `html/DomRange`, `html/Locator`; in `opds/`: `Acquisition`, `Availability`, `Copies`, `Holds`, `Price`, `Facet`, and `publication/opds/Properties`.

Deferred to later phases (still `androidMain`, marked `// TODO(kmp)`): `Publication` (needs `Container`/`Resource` from phase 04 and services from phase 07), and therefore `Metadata`, `Manifest`, `presentation/Metadata`, `opds/Feed`, `opds/Group` which reference `Publication`.

Their test suites moved to `commonTest` (kotlin-test) and now also run on the iOS simulator. Test names lost characters not allowed in Kotlin/Native identifiers (`{}`, `[]`, …).

## Phase 04 — I/O core onto Okio

The I/O core (`util/data`, `util/file`, `util/resource`, `util/asset`, `util/cache`, `util/archive`, `util/format/Sniffing.kt`) moved to `commonMain`, backed internally by Okio (`FileSystem` + `FileHandle` for positional reads). Readium's `Readable`/`Resource`/`Container` abstractions remain the public API; Okio types are not exposed.

### File references are now `file://` URLs

`AbsoluteUrl` (file scheme) is the canonical cross-platform file reference; `java.io.File` entry points remain as Android-only conveniences.

- `FileResource(file: File)` — **was a constructor**, now an `androidMain` factory function with the same call syntax (`util/file/FileAndroid.kt`). The class constructor is now `FileResource(url: AbsoluteUrl)` and requires a `file://` URL.
- `DirectoryContainer(root: File)` — the companion factory is now `DirectoryContainer(root: AbsoluteUrl)` in `commonMain`; a suspend `DirectoryContainer(root: File)` function keeps the Android call syntax. The public constructor changed from `DirectoryContainer(root: File, entries: Set<Url>)` to `DirectoryContainer(root: AbsoluteUrl, entries: Set<Url>)`.
- New common API: `AbsoluteUrl.fromFilePath(path: String, isDirectory: Boolean = false): AbsoluteUrl?` creates a `file://` URL from a percent-decoded absolute path (this is what KMP consumers use instead of `File.toUrl`).
- `FileResource` error mapping no longer distinguishes `SecurityException` (previously `FileSystemError.Forbidden`): Okio surfaces access errors as `IOException` → `FileSystemError.IO`.
- `DirectoryContainer` error mapping changed deliberately: a missing (or unlistable) root directory now fails with `FileSystemError.FileNotFound` instead of succeeding with an empty container (`File.walk` used to swallow the error); other listing I/O failures map to `FileSystemError.IO`. The `SecurityException` → `FileSystemError.Forbidden` mapping is gone (JVM-only type; Okio surfaces access errors as `IOException`). Cancellation and unexpected exceptions now propagate instead of being wrapped.

### `AssetRetriever` Android conveniences became extensions (`util/asset/AssetRetrieverAndroid.kt`)

Call syntax is unchanged, but Android callers may need new imports:

- `AssetRetriever(contentResolver, httpClient)` — **was a constructor**, now an `androidMain` factory function. No import change (imported together with the class name).
- `retrieve(file: File, formatHints)` / `retrieve(file: File, mediaType)` — **were members**, now extensions; add `import org.readium.r2.shared.util.asset.retrieve`.
- `sniffFormat(file: File, hints)` — **was a member**, now an extension; add `import org.readium.r2.shared.util.asset.sniffFormat`.
- The primary common constructor `AssetRetriever(resourceFactory, archiveOpener, formatSniffer)` is unchanged. `DefaultResourceFactory`, `DefaultArchiveOpener` and `DefaultFormatSniffer` stay `androidMain` until the sniffers and ZIP stack move (phases 05–07).

### `ReadException` extends `okio.IOException`

`ReadException` now subclasses `okio.IOException`, which on Android/JVM **is** `java.io.IOException` (typealias) — no change for Android consumers; on iOS it is Okio's own `IOException` class.

### `OutOfMemoryError` expect/actual

`ReadError.OutOfMemory` / `DecodeError.OutOfMemory` reference `org.readium.r2.shared.OutOfMemoryError`, an expect class actual-typealiased to `java.lang.OutOfMemoryError` on Android (binary/source compatible) and `kotlin.OutOfMemoryError` on iOS.

### Decoding

- `ByteArray.decodeString()` (UTF-8) and `decodeJson()` are `commonMain`. The charset-parametrized overload `decodeString(charset: Charset)` is `androidMain` (no default value anymore — calling `decodeString()` resolves to the common UTF-8 version, same behavior).
- `decodeXml()` (needs `XmlParser`, phase 06), `decodeRwpm()` (needs `Manifest`, phase 07) and `decodeBitmap()` (needs the KMP image type, phase 07) stay `androidMain` in `util/data/DecodingAndroid.kt`.
- `Readable.asInputStream()` and the other `Readable` ↔ `java.io.InputStream` adapters stay `androidMain` permanently; common code uses `Readable` directly.

### `FormatHints.charset` became an Android extension

`FormatHints.charset: Charset?` — **was a member** of `FormatHints`, now an `androidMain` extension (`util/format/SniffingAndroid.kt`); add `import org.readium.r2.shared.util.format.charset` if you used it.

### `MemoryObserver` Android helpers became extensions (`util/MemoryObserverAndroid.kt`)

`MemoryObserver` (interface + `Level`, both `@InternalReadiumApi`) is `commonMain`. `Level.fromLevel(Int)` and `MemoryObserver.asComponentCallbacks2(...)` — **were companion members**, now `androidMain` companion extensions; call syntax unchanged, add imports `org.readium.r2.shared.util.fromLevel` / `org.readium.r2.shared.util.asComponentCallbacks2`.

### `toString()` of in-memory resources no longer reads content

`InMemoryResource.toString()` and `StringResource.toString()` used `runBlocking` to read their content; `runBlocking` is banned from common code, so they now print the byte count (when already loaded) or a placeholder. `AssetSniffer` (internal) lost its default constructor arguments (`DefaultFormatSniffer()`/`DefaultArchiveOpener()` are Android-only).

### Relocations to `commonMain` (no API change unless noted)

Moved with unchanged package names: `util/data` (`Reading`, `Container`, `Buffering`, `Caching`, `Decoding` minus the Android-only parts above), `util/file` (`FileResource`, `DirectoryContainer`, `FileResourceFactory`, `FileSystemError`), `util/resource` (all 12 files except `content/ResourceContentExtractor.kt`, which waits for Ksoup in phase 07), `util/asset` (`Asset`, `AssetRetriever`, `AssetSniffer`; `Defaults.kt` stays `androidMain`), `util/cache/Cache.kt`, `util/archive` (`ArchiveOpener`, `ArchiveProperties`), `util/format/Sniffing.kt` (`FormatHints`, `FormatSniffer` & co; `Sniffers.kt` implementations stay `androidMain` until phases 06–07), `util/MemoryObserver.kt`. `DEFAULT_BUFFER_SIZE` used in `buffered()` signatures is now Readium's own `@InternalReadiumApi` constant in `util/data` (same value, 8 KiB, as `kotlin.io.DEFAULT_BUFFER_SIZE`).

Their test suites (`DirectoryContainerTest`, `BufferingResourceTest`, resource `PropertiesTest`) moved to `commonTest` (kotlin-test + `Fixtures`) and also run on the iOS simulator. `ZipContainerTest`, `ReadableInputStreamAdapterTest`, `AssetSnifferTest` and `DefaultSniffersTest` stay in `androidHostTest` until their subjects move (phases 05–07).

## Phase 05a — Zip channel abstractions

### Vendored `java.nio` channel mirror renamed (temporary)

The classes of the vendored channel package `org.readium.r2.shared.util.zip.jvm` and the class `org.readium.r2.shared.util.zip.FileChannelAdapter` — public on Android only because they live in the temporary `:readium:readium-shared-zip-legacy` `java-library` — moved to `org.readium.r2.shared.util.zip.legacyjvm`. They were never meant as public API; the whole legacy project (and package) is deleted at the end of phase 05c, so do not depend on them.

### New common channel layer (internal, no public API change)

`org.readium.r2.shared.util.zip.jvm` is now a Kotlin `commonMain` package containing **internal** suspend-first translations of the channel interfaces (`Channel`, `ReadableByteChannel`, `WritableByteChannel`, `ByteChannel`, `SeekableByteChannel`), their exceptions (based on `okio.IOException`), and `ZipBuffer`, a minimal replacement for `java.nio.ByteBuffer`. The adapters `ReadableChannelAdapter`, `CachingReadableChannel`, `BufferedReadableChannel` (all internal) moved to `commonMain` on these interfaces, and a new internal `FileChannelAdapter` adapts an `okio.FileHandle`. The Android zip containers still run on the legacy blocking stack (via internal `Legacy*` copies of the adapters) until phase 05c swaps them.
