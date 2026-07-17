# Phase 06 — XML & HTTP

Read `docs/kmp/PLAN.md` first. Depends on phases 02 (Url) and 04 (I/O). Breaking phase: append to `docs/kmp/migration-notes.md`.

## Task 6.1 — `XmlParser` onto xmlutil

**File:** `util/xml/XmlParser.kt` — wraps `XmlPullParser` and produces Readium's own `ElementNode` tree; only the tokenizer changes.

- Add `implementation(libs.xmlutil.core)` to commonMain.
- Rewrite the pull loop on `nl.adaptivity.xmlutil.XmlReader` (`xmlStreaming.newReader(...)`), mapping events: START_TAG/END_TAG/TEXT/CDSECT → xmlutil `EventType.START_ELEMENT/END_ELEMENT/TEXT/CDSECT`. Namespace handling: the current parser is namespace-aware (`FEATURE_PROCESS_NAMESPACES`) — xmlutil is namespace-aware by default; preserve `ElementNode`'s namespace fields exactly.
- Input: the current API takes `InputStream`. Change to accept `String`/`ByteArray`/`Readable` (KMP-safe); keep an `InputStream` overload as androidMain extension. Record in migration notes.
- Move `XmlParser.kt` + `ElementNode` to commonMain; port `XmlParserTest` to commonTest (fixtures via `Fixtures`), asserting identical trees for: namespaces, CDATA, entities (`&amp;` etc. — xmlutil handles predefined entities; the current parser's `DOCDECL` behavior should be characterized first), mixed content, encoding declarations (UTF-8/UTF-16 fixtures).

## Task 6.2 — `DefaultHttpClient` onto Ktor

**Files:** `util/http/DefaultHttpClient.kt` (HttpURLConnection), `HttpRequest.kt` (android.os.Bundle extras), plus `HttpClient.kt`, `HttpResponse.kt`, `HttpError.kt`, `HttpStatus.kt`, `HttpHeaders.kt`, `HttpResource.kt`, `HttpContainer.kt`, `ProblemDetails.kt`.

> Phase-01 runtime-parcelization audit (task 1.3) outcome: the only `Bundle`/`Parcel` usages left to handle are `HttpRequest.extras`/`DefaultHttpClient` (this task) and the `Parceler` implementations in `util/Instant.kt` and `extensions/JSON.kt` (androidMain-only, fine as is). The grep hit in `util/format/Sniffing.kt` is a false positive: the word "Bundle" appears only in a KDoc sentence ("Bundle of media type and file extension hints"); no `android.os.Bundle` involved, nothing to do.

1. **Interface layer first:** `HttpClient`, `HttpRequest`, `HttpResponse`, `HttpError`, `HttpStatus`, `HttpHeaders`, `ProblemDetails` → commonMain. `HttpRequest.extras: Bundle` becomes `Map<String, String>` (or drop if unused — grep consumers; record in migration notes). `java.net` exception references in `HttpError` mapping move behind the implementation.
2. **DefaultHttpClient on Ktor:** commonMain implementation using `ktor-client-core`; engines injected per platform (androidMain: `ktor-client-okhttp`; iosMain: `ktor-client-darwin`) via an internal expect/actual `defaultHttpEngine()`. Preserve: redirect handling (current callback `onFollowUnsafeRedirect` semantics), user-agent, timeouts, response streaming into Readium `Readable`/`Resource` (Ktor `bodyAsChannel` → adapt to `Readable`), range requests (used by zip-over-HTTP — keep `Range` header pass-through intact).
3. **Error mapping table** (implement + unit test): UnknownHostException/NoRouteToHost/ConnectException → `HttpError.Unreachable`-equivalent; SocketTimeout → timeout error; SSLHandshake → SSL error. On Ktor these surface as `UnresolvedAddressException`, `HttpRequestTimeoutException`, engine-specific IO exceptions — map by type where possible, fall back to a generic IO error. iOS Darwin engine raises `DarwinHttpRequestException` wrappers; assert mapping in iOS-specific tests if feasible, otherwise keep the mapping engine-agnostic.
4. **Threading:** per PLAN.md concurrency rules — Darwin callbacks land on worker threads; do not touch main-thread-only APIs in the client.
5. `HttpResource`/`HttpContainer` → commonMain once the client compiles.

Tests: port existing HTTP tests; add a commonTest against a scripted fake `HttpClient` for the retry/redirect logic; if a real-socket test exists (MockWebServer), keep it androidHostTest-only.

**Phase acceptance:** OPDS fetching works in test-app (manual smoke); EPUB streaming over HTTP path compiles on iOS; canonical commands pass.
