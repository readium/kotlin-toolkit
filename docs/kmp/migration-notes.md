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
