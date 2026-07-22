# New OPDS catalog module (`org.readium.opds`)

## Context

The current OPDS support (parsers in `org.readium.r2.opds`, models in `org.readium.r2.shared.opds`) doesn't match the actual specifications and is poorly modeled. This plan replaces it with a brand new, KMP-ready implementation inside the existing `readium/opds` module, under a new package `org.readium.opds`. The old code is left untouched for now (deprecated/removed later).

Design decisions settled during grilling:

- **Unified, OPDS 2-shaped model**; OPDS 1 (Atom/XML, 1.0–1.2) is mapped onto it at parse time. Feed exposes which `OpdsVersion` it came from.
- **Reuse shared model types** (`Metadata`, `Link`, `Contributor`, `Subject`, `Properties`, and the OPDS `Properties` extensions in `readium/shared/.../publication/opds/Properties.kt`) for publication entries — but **not** their org.json `fromJSON`. New parsers use **kotlinx-serialization `JsonElement` tree API**.
- **KMP framing**: this module ships **Android-only now**, built against the current Android-backed shared types. "KMP-ready" means written in the target style — JsonElement tree-API parsers, no org.json, no direct Android imports — so its move to commonMain is a mechanical conversion deferred to **KMP phase 08** (`docs/kmp/PLAN.md` on `origin/feature-kmp`), after the shared core lands in phases 01–03. The JsonElement parsing helpers seed the Phase 03 shared-model flip.
- **Three layers**, each usable standalone:
  1. Pure parsers: `Opds1Parser`, `Opds2Parser`, `AuthenticationDocumentParser` — bytes/string + base `Url` → `Try<_, OpdsParseError>`.
  2. Stateless `OpdsClient` (shared `HttpClient`): fetch + sniff version + parse, unified `search(feed, query)` (OpenSearch doc for OPDS 1, URI template for OPDS 2), `nextPage(feed)`, `publication(link)` for full entries, 401 → parsed Authentication Document.
  3. Per-feed stateful `FeedController`: `StateFlow<State>`, `refresh()`, `loadNextPage()` with accumulation. **No back stack** — the app's NavigationStack owns history.
- **Auth: parse + model only** (Authentication for OPDS 1.0). No credential management or flows.
- **Leniency**: warn-and-continue via shared `WarningLogger`. Hard failure only for unparseable XML/JSON (`MalformedDocument`) or not-recognizably-OPDS (`NotAnOpdsDocument`). Fallbacks: missing feed title → `null`; missing self link → request URL; empty feed → empty lists; invalid publication entry → dropped + warning.
- **Names**: `Feed`, `Publication` (package disambiguates from shared's), `Group`, `FacetGroup` (a facet group has a title + facet links; active facet marked with rel `self`), `OpdsClient`, `FeedController`.
- All new public API `@ExperimentalReadiumApi`.

## Build change

`readium/opds/build.gradle.kts`: add `implementation(libs.kotlinx.serialization.json)` (tree API only, no compiler plugin) and `testImplementation(libs.kotlinx.coroutines.test)`.

## File layout

```
readium/opds/src/main/java/org/readium/opds/
├── Feed.kt              // Feed, FeedMetadata, OpdsVersion, Group, FacetGroup
├── Publication.kt       // metadata: shared Metadata, links, images
├── OpdsError.kt         // OpdsParseError, OpdsError, OpdsWarning
├── Opds1Parser.kt
├── Opds2Parser.kt
├── OpdsClient.kt
├── FeedController.kt
├── auth/AuthenticationDocument.kt, auth/AuthenticationDocumentParser.kt
└── internal/
    ├── JsonExtensions.kt     // JsonElement opt* helpers + toPlainKotlin() (KMP Phase 03 seed)
    ├── JsonModelParsers.kt   // JsonElement parsers for shared Metadata/Link/Contributor/Subject/LocalizedString/Properties
    ├── AtomNamespaces.kt
    └── OpenSearchParser.kt
```

## Key signatures

```kotlin
public enum class OpdsVersion { OPDS1, OPDS2 }

public data class FeedMetadata(
    val version: OpdsVersion, val title: String? = null, val identifier: String? = null,
    val modified: Instant? = null,   // kotlin.time.Instant — same type shared Metadata uses; no conversion seam
    val description: String? = null,
    val numberOfItems: Int? = null, val itemsPerPage: Int? = null,
    // OPDS 2: native metadata.currentPage. OPDS 1: derived as startIndex/itemsPerPage + 1
    // when os:startIndex and os:itemsPerPage are both present; otherwise null.
    val currentPage: Int? = null,
)

public data class Feed(
    val metadata: FeedMetadata,
    val links: List<Link> = emptyList(),          // hrefs resolved absolute; self synthesized from request URL if missing
    val navigation: List<Link> = emptyList(),
    val publications: List<Publication> = emptyList(),
    val groups: List<Group> = emptyList(),
    val facets: List<FacetGroup> = emptyList(),
) { val self/next/search: Link? }

public data class Publication(val metadata: Metadata, val links: List<Link>, val images: List<Link>)
public data class Group(val title: String?, val links: List<Link>, val navigation: List<Link>, val publications: List<Publication>)
public data class FacetGroup(val title: String?, val links: List<Link>)  // active facet: rel "self", count: properties.numberOfItems

public sealed class OpdsParseError : Error { MalformedDocument; NotAnOpdsDocument }
public sealed class OpdsError : Error {
    Http(cause: HttpError); AuthenticationRequired(document, cause); Parsing(cause: OpdsParseError)
    SearchNotSupported; NoNextPage
}

public class Opds1Parser(warnings: WarningLogger? = null) {
    fun parseFeed(input: ByteArray/String, url: Url): Try<Feed, OpdsParseError>
    fun parsePublication(input: ByteArray, url: Url): Try<Publication, OpdsParseError>  // standalone entry
}
public class Opds2Parser(warnings: WarningLogger? = null) { // same + JsonObject overloads }

public class OpdsClient(httpClient: HttpClient, warnings: WarningLogger? = null) {
    suspend fun feed(url: AbsoluteUrl): Try<Feed, OpdsError>
    suspend fun feed(link: Link, base: Url? = null): Try<Feed, OpdsError>
    suspend fun publication(link: Link, base: Url? = null): Try<Publication, OpdsError>
    suspend fun search(feed: Feed, query: String): Try<Feed, OpdsError>
    suspend fun nextPage(feed: Feed): Try<Feed, OpdsError>
}

public class FeedController(url: AbsoluteUrl, client: OpdsClient) {
    data class State(feed: Feed?, isLoading: Boolean, error: OpdsError?, hasNextPage: Boolean)
    val state: StateFlow<State>
    suspend fun refresh()        // replaces accumulated content
    suspend fun loadNextPage()   // no-op if no next page or already loading
}
```

`loadNextPage()` merge rule: `publications`/`navigation`/`groups` are **appended**; `metadata` and `links` are **replaced** by the latest page (so `currentPage`/`next` stay truthful, `hasNextPage = latest.next != null`); `facets` are replaced only when the new page has any, otherwise kept (some catalogs send facets on page 1 only).

Ordering safety: the `Mutex` serializes only the *state* critical sections — network fetches happen **outside** the lock (otherwise a slow page fetch would block `refresh()` and the token would be redundant). Exact boundaries, both operations following the same pattern:

1. **Under lock**: if `isLoading` → return (prevents double-fetches); capture the current generation and (for `loadNextPage`) the `next` link from current state; `refresh()` additionally increments the generation; set `isLoading = true`.
2. **Unlocked**: perform the fetch.
3. **Re-acquire lock**: if the captured generation no longer matches, discard the result silently (a refresh superseded this load); else merge/replace state and clear `isLoading`.

Tests cover the refresh-supersedes-load interleaving and the double-`loadNextPage()` guard.

```kotlin

public data class AuthenticationDocument(id, title, authentication: List<Flow>, description, links) {
    data class Flow(type: String, links: List<Link>, labels: Labels)
}
```

`FeedController` is lifecycle-agnostic (suspend ops, no internal scope), serialized with a `Mutex`.

## Version sniffing (OpdsClient)

1. Response media type: `MediaType.OPDS2`/`OPDS2_PUBLICATION`/`OPDS1`*/`OPDS1_ENTRY`/`OPDS_AUTHENTICATION` (all constants already exist in shared).
2. Fallback content sniff: skip a UTF-8 BOM and leading whitespace first, then dispatch on the first significant byte: `{` → JsonObject shape (`authentication` → auth doc; `navigation|publications|groups|facets` → OPDS 2 feed; bare `metadata`+`links` is ambiguous between an *empty* feed and a publication document — tie-break: any link whose rel starts with `http://opds-spec.org/acquisition` (a publication document must have one) or a `self` link typed `application/opds-publication+json` → publication; otherwise treat as an empty feed, the common case); `<` → root element (`feed` → OPDS 1 feed, `entry` → entry). This ambiguity only exists on the content-fallback path; a proper Content-Type always wins.
3. Base for href resolution = final response URL; a valid absolute `rel=self` in-document wins; otherwise self is synthesized from the response URL.
4. Accept header on all requests: `application/opds+json, application/atom+xml;profile=opds-catalog;q=0.9, …`.

## OPDS 1 → unified model mapping (highlights)

- Feed: `atom:title/id/updated` → FeedMetadata; `os:totalResults/itemsPerPage` → numberOfItems/itemsPerPage; all `atom:link` → `Feed.links` (rels verbatim: self/next/search/shelf/…).
- Facets: `link[@rel=".../facet"]` grouped by `@opds:facetGroup` → `FacetGroup`; `@opds:activeFacet="true"` → rel `self`; `@thr:count` → `properties.numberOfItems`.
- Entry classification: has a rel starting `http://opds-spec.org/acquisition` → publication; else navigation link (title from `atom:title`, count from `@thr:count`); no usable link → dropped + warning.
- Groups: entries sharing a `rel="collection"` link are grouped by href (`Group.title` = link title (nullable), group's `self` link kept). Partition rule: a grouped entry appears **only** in its `Group.publications`/`Group.navigation`, never duplicated in the top-level lists; entries without a collection link go to top-level `Feed.publications`/`Feed.navigation` (no synthetic titleless group). A mixed feed (tagged + untagged entries) is an explicit test fixture.
- Publication metadata: Atom preferred over Dublin Core (`atom:title` → localizedTitle, `atom:author` → authors, `atom:category` → subjects, `atom:summary`/`content` → description, `dcterms:issued` → published, `dc:identifier`/`language`/`publisher` mapped; `rights`/`extent`/`source` → `otherMetadata`).
- Images: `.../image`, `.../image/thumbnail` (+ legacy Stanza/cover rels) → `Publication.images` with rels kept.
- Acquisition properties stored as **plain Kotlin values** in `Link.properties.otherProperties` so the existing shared getters (`price`, `indirectAcquisitions`, `availability`, `holds`, `copies`, `numberOfItems`) work unchanged: `opds:price` → map(currency, value), nested `opds:indirectAcquisition` preserved recursively, `opds:availability/holds/copies` → maps.

### Critical invariant: `toPlainKotlin()` numeric materialization

The plain-values strategy hinges on JSON numbers materializing as the **exact runtime type each shared getter casts to** (`Properties.numberOfItems` does `as? Int` — a `Long` or `Double` silently returns null; prices need `Double`). `toPlainKotlin()` must decide per value: try `intOrNull` first, then `longOrNull`, then `doubleOrNull`. This helper feeds **all** `otherProperties`/`otherMetadata` in both parsers; a wrong branch corrupts counts/holds/copies/prices silently, surfacing only when a consumer reads them. Treat this as a dedicated, heavily-tested invariant: a test suite asserting every OPDS getter round-trips through `toPlainKotlin()` output.

Second risk: `indirectAcquisitions` rebuilds via `JSONObject(Map)`, whose deep wrapping of nested `List`/`Map` values is API-level-dependent. The round-trip test must exercise a **multi-level nested** `indirectAcquisition` chain (≥2 levels of `child`), not just one level; if deep wrapping fails under Robolectric, fall back to storing children in a shape the shared getter demonstrably navigates.
- `link[@rel="alternate"][type~="type=entry;profile=opds-catalog"]` kept on `Publication.links` — used by `OpdsClient.publication()`.

## OPDS 2 parsing outline

`Json.parseToJsonElement`; non-object → `MalformedDocument`; no recognizable collection → `NotAnOpdsDocument`. `internal/JsonModelParsers.kt` mirrors shared `fromJSON` logic on `JsonElement` (link href required else drop + warning; `templated` → `Href(str, templated = true)`; `properties`/`otherMetadata` via `toPlainKotlin()`). A group or facet group missing `metadata.title` keeps its content with `title = null` + warning (dropping would lose publications; consistent with the nullable feed title).

## Search

- OPDS 2: `search` link `templated` → expand with the template's **actual** variable names via `Href.parameters` (the spec doesn't standardize the name — catalogs use `{?query}`, `{?q}`, `{?searchTerms}`, …): bind the query to the single parameter if there's exactly one, else to the first of `query`/`q`/`searchTerms` present, then `Href.resolve(base, params)`. Warn if no known parameter matches. Multi-variable templates (e.g. `{?query,page}`): the shared `URITemplate.expandFormStyle` emits **empty pairs for unbound variables** (`?query=foo&page=`) — it never leaves a literal `{var}`, but it does not omit unbound vars as strict RFC 6570 would. Accept the empty pair (the URL is well-formed and catalogs generally tolerate it); the template-expansion test asserts this exact output so the behavior is documented, not accidental. `URITemplate.expand` already percent-encodes values (`percentEncodedQuery()`), so no extra encoding is needed on the OPDS 2 path.
- OPDS 1: fetch `rel=search` OpenSearch description, pick best-matching `<Url>` template (opds profile > atom > any), substitute `{searchTerms}` manually (not RFC 6570) with the query **percent-encoded for the query component** (never a naive `replace()`); optional namespaced variables like `{atom:author?}` replaced with `""`.

## Known constraints found during exploration

- `XmlParser`/`ElementNode` (`shared/util/xml/XmlParser.kt`) are `@InternalReadiumApi` → opt in; instantiate per parse (stateful).
- `WarningLogger` interface is clean but `JsonWarning` is org.json-bound → new `OpdsWarning` type.
- Shared `Url`/`Href`/`Metadata` are still Android-backed → new code avoids direct Android imports ("KMP-ready" = structure + JsonElement parsers), tests use Robolectric until KMP phases swap backings.
- Never wildcard-import `org.readium.r2.shared.publication` in files using `org.readium.opds.Publication`.

## Implementation sequence

1. Build change + models + errors + auth models.
2. `internal/JsonExtensions.kt` + `JsonModelParsers.kt` (+ tests) — everything depends on these.
3. `Opds2Parser` + `AuthenticationDocumentParser` (+ fixtures/tests).
4. `Opds1Parser` + `OpenSearchParser` (+ fixtures/tests).
5. `OpdsClient` (sniffing, search, pagination, auth detection) + fake-`HttpClient` tests.
6. `FeedController` + tests.
7. Docs: create `CONTEXT.md` (glossary: Catalog, Feed, Publication, Navigation, Group, FacetGroup/facet, Acquisition Link, Complete Entry, Authentication Document) and `docs/adr/0001-unified-opds2-shaped-model.md` (unified OPDS 2-shaped model + JsonElement parsers seeding KMP Phase 03). Update `CHANGELOG.md`.

## Verification

- `./gradlew :readium:readium-opds:test` — parser tests assert full model equality on fixtures (reuse existing `.atom` fixtures + new real-catalog captures under `src/test/resources/org/readium/opds/{opds1,opds2,auth}/`), warning assertions via `ListWarningLogger` (dropped entries, missing title/self), sniffing matrix (incl. BOM/whitespace-prefixed payloads) + search round-trips (OPDS 2 with non-`query` template variables; OPDS 1 with queries needing percent-encoding) + 401/auth with a fake `HttpClient`, `FeedController` state sequences and page-merge rules with `kotlinx-coroutines-test`. The fake `HttpClient` must reproduce the real contract on 401: `HttpError.ErrorResponse` carrying both the auth-document `body` and its `mediaType`. `OpdsClient` issues **GET** requests, and `DefaultHttpClient` reads a GET's error body inline from `errorStream` (the re-fetch path only applies to HEAD) — the fake must mirror the GET path exactly.
- Property round-trip test: OPDS 1 & 2 prices/holds/copies/indirect acquisitions readable through the existing shared `Properties` extensions.
- End-to-end smoke (**manual only, not in the automated suite**): point `OpdsClient` at a real catalog (e.g. `https://test.opds.io/2.0/home.json` and a Feedbooks OPDS 1 feed) from a scratch script — never part of `:readium:readium-opds:test`, so network/catalog drift can't red the build.
- `./gradlew :readium:readium-opds:build` — explicitApi + allWarningsAsErrors clean.
