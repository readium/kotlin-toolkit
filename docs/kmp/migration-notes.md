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

## Phase 05b — Commons Compress port to Kotlin common

### Vendored `compress` package renamed in the legacy project (temporary)

Same treatment as the 05a `legacyjvm` rename: the vendored Java package `org.readium.r2.shared.util.zip.compress` in `:readium:readium-shared-zip-legacy` moved to `org.readium.r2.shared.util.zip.legacycompress` (mechanical find-replace, plus the two androidMain call sites `StreamingZipArchiveProvider`/`StreamingZipContainer`), freeing the original FQNs for the Kotlin port. These classes were never meant as public API; the whole legacy project is deleted at the end of phase 05c.

### New common zip stack (internal, no public API change)

The read path of the vendored Commons Compress subset is now Kotlin in shared `commonMain`, same packages (`…util.zip.compress.archivers[.zip]`, `…utils`), everything **internal**:

- `ZipFile`, `ZipArchiveEntry`, `ZipUtil`, extra-field machinery (`ZipExtraField`, `ExtraFieldUtils`, zip64/unicode/resource-alignment fields), encodings, value types (`ZipShort`, `ZipLong`, `ZipEightByteInteger`, `GeneralPurposeBit`), and the bounded/counting/buffered/inflater stream helpers.
- All I/O is **suspend-first** on the 05a channels; `java.io.InputStream` was replaced by an internal suspending `ZipInputStream` (`compress/utils`), deliberately minimal (`read`/`skip`/`close`). The `ZipFile` constructor became a `suspend operator fun invoke` factory on the companion (same call syntax).
- `java.util.zip.Inflater` is now `internal expect class Inflater` (`compress/archivers/zip`): the Android actual delegates to `java.util.zip.Inflater`, the iOS actual wraps `platform.zlib` (raw deflate via `inflateInit2` with negative window bits). Covered by a commonTest fixture-inflation suite and an androidHostTest JVM-deflate/common-inflate round trip.
- `java.util.zip.ZipException` → internal `compress.archivers.zip.ZipException : okio.IOException`. `java.util.zip.CRC32` → internal common `Crc32`. `ZipEightByteInteger` is backed by a plain `Long` instead of `BigInteger` (lossless round-trip; only the textual form of values ≥ 2^63 differs).
- Encodings: `NioZipEncoding`/`CharsetAccessor` (built on `java.nio.charset`) were replaced by a common encoding table per the 05a decision — zip names are CP437 (validated 256-char table) or UTF-8 (hand-rolled decoder replacing malformed input with `?`, like the original `CodingErrorAction.REPLACE` configuration). The `ZipEncoding` interface kept only `decode`; `encode`/`canEncode` were writer-only.

### Not ported (writer-only, unreachable from the read path)

`ZipArchiveOutputStream`, `StreamCompressor`, `ScatterZipOutputStream`, `ScatterStatistics`, `ZipSplitOutputStream`, `Zip64Mode`, `Zip64RequiredException`, `ZipArchiveEntryRequest`, `ZipArchiveEntryPredicate`, `parallel/*` (3 files), `ArchiveInputStream`, `ArchiveOutputStream`, `BoundedInputStream`, `FileNameUtils`, `CountingInputStream`'s unused overloads, plus `ZipFile.copyRawEntries`/`getUnixSymlink` and the `java.io.File`/`Path` factories of the split channels. The central-directory signature constants formerly on `ZipArchiveOutputStream` moved into `ZipFile`. The Java files remain in the legacy project (they are reachable from the legacy `ZipFile`) and die with it in 05c.

### Behavior restored relative to the vendored Java copy

The vendored `ExtraFieldUtils` registry had been stripped empty, so the legacy `ZipFile` parsed every extra field as `UnrecognizedExtraField` — meaning it **could not read zip64 archives** (it threw "archive contains unparseable zip64 extra field") and never honored InfoZIP Unicode path/comment extra fields. The port hardcodes the kept typed fields (zip64, unicode path/comment, resource alignment) in `ExtraFieldUtils.createExtraField`, restoring upstream Commons Compress behavior; commonTest covers zip64 and unicode-name fixtures. The androidHostTest differential suite (`ZipFileDifferentialTest`) asserts byte-identical output against the legacy implementation on the fixtures the legacy code can read (`epub.epub`, stored/deflated, data descriptors).

Other knowingly accepted deltas: `ZipUtil.dosToJavaTime` is on kotlinx-datetime (lenient rollover like `java.util.Calendar`, current system time zone; DST edge cases may differ by an hour — entry times are unused by Readium), and malformed UTF-8 names decode with one `?` per invalid byte rather than per invalid sequence.

### Test fixtures

`src/commonTest/fixtures/zip/` now holds the zip fixture set used through the `Fixtures` helper: `epub.epub` (copied from `fixtures/resource`), generated `basic.zip` (stored + deflated), `datadescriptor.zip` (streamed, general purpose bit 3), `unicode.zip` (EFS-flagged UTF-8 name + InfoZIP unicode path extra field), a hand-crafted minimal `zip64.zip` (zip64 EOCD + locator + extended information extra field), and a raw-deflate pair (`lorem.deflate`/`lorem.txt`) for the `Inflater` tests.

## Phase 05c — Zip containers on the ported stack

### `:readium:readium-shared-zip-legacy` deleted

The temporary `java-library` subproject that hosted the vendored Java zip stack (packages `…util.zip.legacyjvm` and `…util.zip.legacycompress`, plus the `Legacy*` adapter copies in shared androidMain) is gone, together with `readium-shared`'s `api` dependency on it. These classes were public on Android for build reasons only and were documented as doomed since 05a/05b; any consumer that depended on them must move to the Readium `Resource`/`Container` APIs. `readium-shared` no longer ships any vendored Java code.

### Zip containers are common and single-stack (internal, behavioral notes)

`StreamingZipContainer`, `StreamingZipArchiveProvider`, `FileZipArchiveProvider` and `ZipArchiveOpener` moved to `commonMain`. Option (a) of the phase doc was taken: `FileZipContainer` (the `java.util.zip.ZipFile`-based file path) was **deleted** and local files now open through the same ported stack, via an internal `FileChannelAdapter` on `okio.FileHandle` (`FileZipArchiveProvider` only differs from the streaming provider by its file-specific error mapping). Consequences:

- File-based zip containers now behave exactly like streaming ones: both open with `ignoreLocalFileHeader = true` (central directory only, like `java.util.zip`), so entry names decode per the zip spec (CP437 or UTF-8; InfoZIP unicode extra fields are not applied by the containers) instead of `java.util.zip`'s UTF-8-only decoding, zip64 archives work, and `Container.get` no longer returns directory entries (the legacy `FileZipContainer` did).
- **Concurrency**: entry reads of one file-based container are now serialized on a container-wide mutex (the shape `StreamingZipContainer` always had), whereas the legacy `FileZipContainer` allowed concurrent entry streams — e.g. an internal HTTP server serving CSS/images/audio of one publication in parallel now reads them sequentially. Known limitation; a possible follow-up is per-entry channels on `okio.FileHandle` (thread-safe positional reads). Closing mirrors the legacy shape: `Resource.close()` closes the entry's cached stream without taking that mutex, and `Container.close()` closes the archive from a `GlobalScope` job, so closing while a read is in flight is (as before) not synchronized.
- Error mapping: `SecurityException` is no longer mapped to `FileSystemError.Forbidden` when opening a zip file (common code cannot catch the JVM-only type); such failures surface as `FileSystemError.IO`. `FileZipArchiveProvider.sniffOpen` on a missing file now fails with `FileSystemError.FileNotFound` instead of `FileSystemError.IO` (the legacy code only special-cased `FileNotFoundException` in `open`).
- Benchmarks (androidHostTest, 55 MB / 220-entry zip, Robolectric host JVM), `java.util.zip` baseline → ported stack: open 1 → 3 ms, full sequential read of all entries 42 → 54 ms, chunked ranged read of a 1 MB deflated entry 1 → 2 ms. (An earlier draft measured 15/86/14 ms; the gap was mostly eager local-file-header parsing, a missing dispatcher hoist around open, a 512-byte inflater fill buffer and temporary-array copies in the channel adapters, all fixed during review.) The residual delta is the suspend stream plumbing vs. `java.util.zip`'s native path; judged marginal, so the expect/actual JVM fast path (option b) was not needed.

### Tests

`ZipContainerTest` moved to commonTest (file, streaming and exploded-directory containers over the `resource/epub.epub` and `resource/epub` fixtures — the exploded EPUB moved from androidHostTest resources to `commonTest/fixtures/resource/epub`). A new commonTest `StreamingZipContainerTest` locks in the zip-over-HTTP contract of ADR 0002 against an in-memory ranged-read fake: opening a > 5 MB archive fetches exactly one 65 557-byte tail read, entry reads stay bounded (no full download), and < 5 MB archives are cached with a single full read. The 05b differential suite was extended to the containers, run green one last time, and deleted with the legacy project.

## Phase 06 — XML & HTTP

### `XmlParser` is common and backed by xmlutil

`org.readium.r2.shared.util.xml` (`XmlParser`, `ElementNode`, `TextNode`, `Attribute`, `AttributeMap`) moved to `commonMain`. The tokenizer is now [xmlutil](https://github.com/pdvrieze/xmlutil)'s generic `XmlReader` (pinned to 0.91.3: the 1.0.x klibs are built with Kotlin 2.4 and cannot be consumed by Kotlin 2.3) instead of `XmlPullParser`. API changes:

- `XmlParser.parse(InputStream)` — **was the only input**, now Android-only extension in `androidMain` (`util/xml/XmlParserAndroid.kt`); add `import org.readium.r2.shared.util.xml.parse`. The common entry points are `parse(String)`, `parse(ByteArray)` and `suspend parse(Readable)`.
- Parse failures throw `org.readium.r2.shared.util.xml.XmlParserException` instead of `org.xmlpull.v1.XmlPullParserException` (and `parse(Readable)` throws `ReadException` for read errors). No caller in the toolkit caught the old type.
- `parse(ByteArray)` detects the encoding from the BOM or the XML declaration and supports UTF-8 (default and fallback), UTF-16 (BE/LE, with or without BOM) and ISO-8859-1/US-ASCII. `XmlPullParser` supported any charset of the JVM; other declared encodings now fall back to a lenient UTF-8 decoding.
- Predefined entities (`&amp;` …) and character references are decoded as before. Undeclared entities (e.g. `&nbsp;` in an XHTML document parsed without its DTD) already made the Android parser throw; xmlutil behaves the same. DOCTYPE declarations are still skipped without processing.
- `ByteArray.decodeXml()` moved from `androidMain` to `commonMain` (same package `util.data`, no API change).

### The HTTP stack is common and `DefaultHttpClient` runs on Ktor

`org.readium.r2.shared.util.http` moved to `commonMain` in its entirety (`HttpClient`, `HttpRequest`, `HttpResponse`, `HttpError`, `HttpStatus`, `HttpHeaders`, `ProblemDetails`, `DefaultHttpClient`, `HttpResource`, `HttpContainer`, `HttpResourceFactory`). `DefaultHttpClient` is now implemented with the [Ktor client](https://ktor.io) — OkHttp engine on Android, Darwin (NSURLSession) engine on iOS — instead of `HttpURLConnection`.

Public API changes:

- `HttpStreamResponse.body` is now a Readium `Readable` instead of a `java.io.InputStream`. It supports **forward reads only** (any range starting at or after the current position; a backward range fails with `ReadError.UnsupportedOperation`), and you must still `close()` it to terminate the connection. Android callers needing an `InputStream` can wrap it with the existing `Readable.asInputStream()` adapter (`util/data`).
- `HttpRequest.extras` is now a `Map<String, String>` instead of an `android.os.Bundle` (it was the last runtime `Bundle` usage flagged by the phase-01 parcelization audit). `HttpRequest`, `HttpRequest.Method` and `HttpRequest.Body` no longer implement `java.io.Serializable`.
- `HttpRequest.Body.File` now takes a `file://` `AbsoluteUrl` instead of a `java.io.File` (no known consumer; the body is streamed by the client through Okio).
- `HttpRequest.Builder.appendQueryParameter(s)` now actually appends the parameters to the built request URL. Before the migration the parameters were collected into an `android.net.Uri.Builder` that `build()` never read — appending was silently a no-op. No caller in the toolkit relied on it.
- `HttpClient.fetchString` lost its `charset` parameter and always decodes as UTF-8 (no caller used another charset).
- `HttpClient.download(request, destination)` takes a `file://` `AbsoluteUrl` destination in common code; an `androidMain` extension keeps the `java.io.File` call syntax. `onProgress` is no longer dispatched on the main thread — hop to your UI thread yourself if needed (per the KMP concurrency ground rules, the toolkit makes no thread-affinity assumptions).
- `DefaultHttpClient.Callback` is unchanged, including `onFollowUnsafeRedirect` semantics: redirections to the same scheme are followed automatically (up to 5), while cross-scheme redirections (e.g. HTTP → HTTPS) require explicit confirmation.
- `DefaultHttpClient` now implements Readium's `Closeable`. It holds a network engine and a coroutine scope for the in-flight requests; call `close()` on short-lived instances to release them and cancel any response body still being streamed. A closed client fails new requests with an `HttpError` (long-lived app-wide instances can ignore this, like before).
- `HttpRequest.allowUserInteraction` is kept but now ignored by `DefaultHttpClient` (it mapped to `HttpURLConnection.allowUserInteraction`, which has no Ktor equivalent).

Behavioral notes:

- Redirections are now handled entirely by Readium (Ktor's `followRedirects` is off), with the following rules:
  - Only 301, 302, 303, 307 and 308 trigger a redirection. The other 3xx statuses (e.g. 304 Not Modified) are returned as regular responses — the old client failed on them with `MalformedResponse` because they carry no `Location`.
  - The original request headers are preserved across redirections (like `HttpURLConnection` re-sent them), so a `Range` request redirected to a CDN stays a range request. When the redirection changes the scheme or the host, the credential-bearing headers (`Authorization`, `Proxy-Authorization`, `Cookie`) are dropped, like OkHttp does.
  - Cookies set by the redirecting response are forwarded only to the **same host**, as cookie pairs (attributes such as `Path` or `HttpOnly` are stripped) merged into a single `Cookie` header.
  - A 303 See Other redirection of a POST/PUT/PATCH/DELETE is followed with a GET request without body, per RFC 9110. Divergence from `HttpURLConnection`: the JDK also converted 301/302 POSTs to GET, whereas Readium keeps the method and body for those (the unsafe cross-scheme path always did).
  - At most 5 redirections are followed; the count is stored in `HttpRequest.extras["redirectCount"]`.
- A failed HEAD request is still retried as GET to fetch the error body (used e.g. for OPDS Authentication Documents).
- Error mapping: timeouts map to `HttpError.Timeout` (Ktor timeout exceptions plus `java.net.SocketTimeoutException` on Android, `NSURLErrorTimedOut` on iOS); unreachable hosts to `HttpError.Unreachable` (`UnknownHostException`/`NoRouteToHostException`/`ConnectException` on Android, DNS/connect `NSURLError*` codes on iOS, `UnresolvedAddressException` in common); TLS failures to `HttpError.SslHandshake` (`SSLHandshakeException` on Android, certificate `NSURLError*` codes on iOS); anything else to `HttpError.IO`.
- `DefaultHttpClient` no longer logs request headers at Info level: requests are logged at Debug severity with `Authorization`, `Proxy-Authorization`, `Cookie` and `Set-Cookie` values masked (fixes the credential-leak concern recorded in phase 01).
- `HttpResource` keeps its skip-forward stream caching (`maxSkipBytes` = 8 KiB) and open-ended range requests on top of the new `Readable` body, so zip-over-HTTP and progressive download streaming behave as before. Its `close()` now closes the cached response body (it used to be a no-op). Two pre-KMP limitations are deliberately kept: 206 responses are trusted without validating the `Content-Range` offset, and concurrent `read()` calls are not synchronized.
- `Readable.asInputStream()` (androidMain) no longer throws when the underlying `Readable` cannot report its length (e.g. an HTTP response without `Content-Length`, streamed with chunked transfer encoding): `available()` returns 0 and reads stream until exhaustion.

Tests: `ProblemDetailsTest` moved to `commonTest`; new commonTest suites `DefaultHttpClientTest` (against Ktor's `MockEngine`: redirects, error bodies, HEAD fallback, range pass-through, user-agent, callback retry, exception mapping) and `HttpRequestTest` run on both Android and the iOS simulator.

## Phase 07 — Publication services & remaining models

### Multiplatform image type: `ReadiumImage`

`org.readium.r2.shared.util.ReadiumImage` (commonMain expect) is the new multiplatform image type: it wraps an `android.graphics.Bitmap` on Android (accessor: `readiumImage.bitmap`) and a `UIKit.UIImage` on iOS (accessor: `readiumImage.uiImage`), and exposes `width`/`height` in pixels. `org.readium.r2.shared.util.ImageSize(width, height)` replaces `android.util.Size` in the affected APIs.

- `CoverService.cover()` and `Publication.cover()` now return `ReadiumImage?` instead of `Bitmap?`; `coverFitting(maxSize:)` takes an `ImageSize` and returns `ReadiumImage?`. Android conveniences preserving the old call syntax: `Publication.coverAsBitmap()` and `Publication.coverFittingAsBitmap(android.util.Size)` (androidMain).
- `InMemoryCoverService.createFactory` takes a `ReadiumImage?` instead of a `Bitmap?`.
- `ByteArray.decodeBitmap(): Try<Bitmap, DecodeError>` (androidMain) is replaced by the common `ByteArray.decodeImage(maxSize: ImageSize? = null): Try<ReadiumImage, DecodeError>`. When `maxSize` is given, the image is decoded downscaled to fit it (subsampled `BitmapFactory` decode on Android, ImageIO thumbnailing on iOS) — `CoverService.coverFitting` uses this instead of decoding the cover at full size and scaling it down afterwards.
- EXIF orientation parity gap: on iOS, `decodeImage` applies the EXIF orientation of rotated JPEGs (both `UIImage(data:)` and the ImageIO thumbnail path honor it, and `ReadiumImage.width`/`height` report the oriented, visual dimensions). On Android, `BitmapFactory` ignores EXIF orientation, like the pre-KMP `decodeBitmap` did — a 90°-rotated photo decodes with swapped dimensions and no rotation. Fixing Android would require an `androidx.exifinterface` dependency and a rotation pass; left as-is to preserve the existing Android behavior. Publication covers are virtually never EXIF-rotated, so the impact is theoretical.
- `PdfDocument.cover(context: Context): Bitmap?` is now `cover(): ReadiumImage?` — implementations needing an Android `Context` (like the PSPDFKit adapter) must capture it at construction. `PsPdfKitDocument`'s constructor takes the `Context` as its first parameter.

### HTML parsing uses Ksoup instead of Jsoup in `readium-shared`

`HtmlResourceContentIterator` (content service) and `HtmlResourceContentExtractor` (search indexing) now parse HTML with [Ksoup](https://github.com/fleeksoft/ksoup) (`com.fleeksoft.ksoup`), a multiplatform port of Jsoup with the same parsing behavior and selector API. `readium-shared` no longer depends on `org.jsoup`; the Android-only navigator modules keep their own Jsoup dependency. No API change: the swap is internal (the existing content-iterator test suite passes unchanged).

### Text tokenizer: `DefaultTextContentTokenizer` is expect/actual

`org.readium.r2.shared.util.tokenizer` (`TextTokenizer`, `TextUnit`, `DefaultTextContentTokenizer`) moved to `commonMain`. `DefaultTextContentTokenizer` is now an expect/actual class: the Android actual keeps the previous behavior (ICU `BreakIterator` on API 24+, `java.text.BreakIterator` below), the iOS actual uses `CFStringTokenizer` (word-boundary and sentence units). `IcuTextTokenizer` and `NaiveTextTokenizer` remain Android-only, in `androidMain`. Word/sentence tokenization of plain English text is asserted identical across platforms in commonTest; language-specific edge cases may differ slightly between ICU and CFStringTokenizer.

### Search: `StringSearchService` is common, ICU stays as the Android algorithm

`publication/services/search` moved to `commonMain` in its entirety. API changes:

- `StringSearchService.Algorithm.findRanges` takes a `language: Language?` instead of a `locale: java.util.Locale`.
- `StringSearchService.IcuAlgorithm` became the top-level `IcuAlgorithm` (androidMain, same package). The new common `DefaultSearchAlgorithm` expect/actual class is the default used by `StringSearchService.createDefaultFactory()`: on Android it delegates to `IcuAlgorithm` (or `NaiveAlgorithm` below API 24), on iOS it runs an `NSString.rangeOfString(options:range:locale:)` loop with case/diacritic-insensitive options. Whole-word search (`Options.wholeWord`) is supported only by the Android ICU algorithm; the exact match and case-insensitive behaviors are asserted in commonTest on both platforms, locale-collation edge cases may differ.

### Publication, Manifest, Metadata and all core services are common

The remaining publication models and services moved from `androidMain` to `commonMain` (same packages): `Publication`, `PublicationServicesHolder`, `Manifest`, `Metadata`, `presentation/Metadata`, `epub/Presentation`, `epub/Publication`, `opds/Publication`, `EpubEncryptionParser`, `HrefNormalizer`, `ManifestTransformer`, `MediaOverlays`, `MediaOverlayNode`, `opds/Feed`, `opds/Group`, `protection/ContentProtection`, `protection/FallbackContentProtection`, the services (`CoverService`, `PositionsService`, `LocatorService`, `ContentService`, `ContentProtectionService`, `CacheService`, `SearchService`), the content iterators (`Content`, `ContentTokenizer`, `PublicationContentIterator`, `HtmlResourceContentIterator`), `ResourceContentExtractor`, `PdfDocument`/`PdfDocumentFactory`, the format `Sniffers` and `ByteArray.decodeRwpm()`/`JsonObject.decodeRwpm()`. Behavior is unchanged; the API breaks are:

- `Publication.Profile`, `Metadata` and `SearchService.Options` now implement the Readium multiplatform `Parcelable` stand-in (`org.readium.r2.shared.util.Parcelable`) instead of `android.os.Parcelable` directly — on Android this is a typealias, so Android consumers are unaffected.
- `MediaOverlays` and `MediaOverlayNode` no longer implement `java.io.Serializable`.
- `Content.Attributes`: `QuoteElement`'s `referenceUrl` is a Readium `Url?` instead of `java.net.URL?`.
- `InMemoryCacheService` (which requires an Android `Context`) stays in `androidMain`; the `CacheService` interface is common.
- The deprecated `org.readium.r2.shared.util.Instant` compatibility wrapper stays in `androidMain` (it exposes `java.util.Date` conversions); use `kotlin.time.Instant` in common code.

### Accessibility display guide is common; string localization is platform-specific

`accessibility/AccessibilityMetadataDisplayGuide` and `AccessibilityDisplayString` moved to `commonMain`. Because resolving Android string resources requires a `Context`, the localization entry points became platform extensions (decision recorded per the phase-07 note):

- `Field.localizedTitle(context)` and `Statement.localizedString(context, descriptive)` are now **androidMain extension functions** in the same package instead of interface members — the call syntax is unchanged, but Android callers must import `org.readium.r2.shared.accessibility.localizedTitle` / `localizedString`. They still resolve the W3C guide translations bundled as Android resources.
- iOS gets `Field.localizedTitle()` and `Statement.localizedString(descriptive)` extensions backed by a generated lookup table of the en-US strings (v2.0.c of the W3C JSON strings), bundled in the binary.
- `AccessibilityMetadataDisplayGuide.Statement` is now a **sealed** interface (its implementations were already internal).

## Phase 08 — Streamer & OPDS conversion

`readium-streamer` and `readium-opds` are now Kotlin Multiplatform modules (`androidTarget`, `iosArm64`, `iosSimulatorArm64`), published as KMP artifacts like `readium-shared`. Almost all of their code lives in `commonMain`; the API breaks are limited to the streamer entry points that used to take an Android `Context`.

### Streamer parsers: `Context` replaced by an optional cache-service factory

The `Context` parameter of the parsers was only used to build an `InMemoryCacheService` (which registers Android memory-pressure callbacks). The parser cores are now common and take an optional `cacheServiceFactory` instead; `androidMain` factory *functions* preserve the exact pre-KMP call syntax:

- `DefaultPublicationParser(context, httpClient, assetRetriever, pdfFactory, additionalParsers)` (androidMain function) still works unchanged and wires `InMemoryCacheService` as before. The common class is `DefaultPublicationParser(httpClient, assetRetriever, pdfFactory, additionalParsers, cacheServiceFactory)`.
- `PdfParser(context, pdfFactory)` (androidMain function) still works unchanged. The common class is `PdfParser(pdfFactory, cacheServiceFactory)`.
- `ReadiumWebPubParser(context, httpClient, pdfFactory, epubReflowablePositionsStrategy)` (androidMain function) still works unchanged. The common class is `ReadiumWebPubParser(httpClient, pdfFactory, epubReflowablePositionsStrategy, cacheServiceFactory)` — the `context: Context? = null` first parameter is gone from the common constructor.
- `cacheServiceFactory` defaults to a **platform default**: on Android, an `InMemoryCacheService` *without* memory-pressure callbacks — exactly the pre-KMP behavior of `ReadiumWebPubParser(context = null, …)`. So an Android caller using the common constructors (e.g. `ReadiumWebPubParser(httpClient = …, pdfFactory = …)`) keeps getting a cache service, it just doesn't release its content on memory pressure; use the `Context`-taking androidMain entry points to get the trim callbacks. On iOS, the default is **no cache service**: opened PDF documents are not cached between publication services unless the caller provides a factory. `InMemoryCacheService` itself stays in `androidMain` (it needs a `Context` for memory-pressure callbacks).

`PublicationOpener`, `PublicationParser`, `CompositePublicationParser`, the EPUB/audio/image parser stack and the positions services are common with unchanged APIs. `extensions/File.kt` (`File.firstComponent`, internal) stays in `androidMain`.

### Logging: streamer no longer depends on Timber

The three `Timber` call sites in `readium-streamer` (`PdfPositionsService`, `LcpdfPositionsService`, `ReadiumWebPubParser`) now log through the `ReadiumLog` facade, and the module no longer depends on Timber nor on `com.mcxiaoke.koi` (SHA-1 and hex decoding are done with Okio/pure Kotlin). To make this possible, `ReadiumLog.v/d/i/w/e` in `readium-shared` changed from `internal` to `public` annotated `@InternalReadiumApi` — they are meant for Readium modules only.

### OPDS

`OPDS1Parser` and `OPDS2Parser` moved to `commonMain` unchanged (they had no platform dependencies left after phases 03/06). `readium-opds` no longer declares a Timber dependency (it never used it).

### Tests

The streamer and OPDS test suites (Robolectric-free since the shared migration) run in `commonTest` on both the Android host and the iOS simulator, using per-module `Fixtures` helpers. A new end-to-end `EpubParserTest` opens a fixture EPUB (zip container → package document → `Publication`) on every test platform, fulfilling the phase-08 acceptance criterion. Only `FileTest` (java.io.File) remains Android-only.
