# Kotlin Multiplatform Migration — Master Plan

This document is the single source of truth for migrating the Readium Kotlin Toolkit core to Kotlin Multiplatform (KMP). It contains the ground rules every implementing agent must follow. **Read this document in full before starting any task.** Each phase has its own document in `docs/kmp/phases/` with concrete tasks; load only this document plus your phase document.

## Goals

- `readium-shared`, `readium-streamer` and `readium-opds` compile and pass tests for the targets: `androidTarget`, `iosArm64`, `iosSimulatorArm64`.
- KMP app developers can consume these modules from `commonMain`, published as regular KMP artifacts on Maven Central.
- The Android-only modules (`readium-navigator`, `readium-navigator-*`, `readium-lcp`, adapters, `test-app`, demos) keep compiling and working against the migrated core at every step.

## Non-goals

- Swift-export polish (SKIE, XCFramework, SPM packaging) — out of scope; iOS consumers are KMP apps, not the Swift toolkit.
- Migrating navigators, LCP or adapters to KMP.
- `iosX64` (Intel simulator): deliberately dropped, like most of the KMP ecosystem. Easy to add back on demand.

## Architectural decisions

| Concern | Decision |
|---|---|
| Targets | `androidTarget`, `iosArm64`, `iosSimulatorArm64` |
| API policy | Breaking major release. Temporary `androidMain` deprecation bridges allowed *within* a phase, deleted before the phase ends |
| JSON | kotlinx.serialization **JsonElement tree API** (not `@Serializable` data classes). Port the org.json `opt*` helper style; keep the lenient parse-with-warnings design |
| Zip | Port the vendored Commons Compress subset + `java.nio` channel shims to pure Kotlin in `commonMain` (preserves zip-over-HTTP streaming & random access). See ADR 0002 |
| I/O | **Okio** internally (`FileSystem` + `FileHandle` for positional reads). Readium's `Resource`/`Container` stay the public API. Okio `ByteString` for digests. See ADR 0003 |
| HTTP | `DefaultHttpClient` on **Ktor client** (OkHttp engine on Android, Darwin engine on iOS) |
| XML | **xmlutil** (pdvrieze) `XmlReader` under the existing `XmlParser`/`ElementNode` API |
| URI | **uri-kmp** (eygraber) backing the `Url` type |
| HTML | **Ksoup** (fleeksoft) replacing Jsoup |
| Parcelable | Readium-owned expect/actual `@Parcelize` annotations (actual typealias to kotlinx.parcelize on Android; no-op on iOS) |
| Images | Readium-owned expect/actual image type (Bitmap on Android; CGImage/UIImage on iOS) |
| ICU text | expect/actual behind the existing `TextTokenizer`/`SearchService` interfaces (android.icu on Android; CFStringTokenizer/NSString on iOS) |
| Logging | Readium-owned minimal logging facade replacing Timber (Logcat / os_log defaults, app-pluggable) |
| Dates | kotlinx.datetime (already the norm; only residual cleanup) |

## Concurrency ground rules

Standard kotlinx-coroutines on all targets (Kotlin 2.x memory model; `Dispatchers.IO` exists on Native).

- Suspend-first APIs, as today.
- No `runBlocking` in common code.
- No thread-affinity assumptions; never assume you are on the main thread.
- Ktor Darwin engine callbacks land on worker threads — always hop through dispatchers.

## Always-green protocol

**Every task ends with the full repository compiling and all tests passing.** Run the canonical verification commands (below) before considering a task done.

For API-breaking tasks, choose one of two mechanisms (the phase doc says which):

1. **Atomic task** — the task scope includes fixing *all* downstream call sites (navigator, navigators, lcp, opds, streamer, adapters, test-app, demos) in the same change. The phase doc contains a grep-generated inventory of those call sites.
2. **Bridged task** — introduce the new API alongside a bridge preserving the old signature, annotated with both `@Deprecated` and the marker annotation `@KmpMigrationBridge` (defined in phase 01), living in `androidMain`. Downstream migrates in follow-up tasks. The phase's final task deletes all bridges: `grep -rn "KmpMigrationBridge" readium/` must return only the annotation's own declaration.

**Bridging limits:** bridges only work for free functions and extension helpers. Kotlin cannot overload on return type, so interface contract flips — above all `JSONable.toJSON(): JSONObject → JsonObject` — cannot be bridged and must be atomic.

## Module conversion recipe

Applied to each module in order: shared (phase 00), streamer and opds (phase 08).

1. Flip the module to the KMP plugin with **all existing code moved unchanged into `src/androidMain`** (use `git mv` to preserve history). The module compiles exactly as before; iOS source sets are empty.
2. Move subsystems to `src/commonMain` one at a time, in the order given by the phase docs. Each move is a task, gated by the verification commands.
3. Platform code that is *deliberately* Android-only (ContentResolver, AssetManager, android.icu actuals…) stays in `androidMain` permanently.

**Intra-module move ordering:** a `commonMain` file cannot reference an `androidMain` sibling. Move leaves before aggregates (e.g. `Href`/`Properties` before `Link`, `Link` before `Locator`, everything before `Manifest`). Phase docs list the topological order.

## Coding conventions for agents

- Preserve package names exactly; only source-set directories change.
- Use `git mv` when relocating files, in a separate commit from content changes when practical. **Never commit — leave staging/committing to the maintainer.** Structure your work so each task is one reviewable unit.
- Keep the lenient-parsing architecture: parsers skip invalid data and log through `WarningLogger`; they do not throw.
- `explicitApi()` and `allWarningsAsErrors` are already on and stay on.
- Port tests alongside the code they cover (see test-porting rule).
- Migration-guide notes: every breaking phase from 02 onward appends its breaks to `docs/kmp/migration-notes.md` as it goes. Phase 09 assembles the final migration guide from it, never reconstructs.
- If a task turns out impossible as written (missing dependency, wrong assumption), stop and report; do not improvise around a phase-doc instruction.

## Test-porting rule

`readium-shared` has 58 test files: ~48 use Robolectric, ~10 are pure JVM. When a subsystem moves to `commonMain`, re-evaluate its tests:

- Most Robolectric usage exists only because of `android.net.Uri` / org.json. Once those become KMP types, the tests **must** move to `commonTest`, converted to kotlin-test assertions (AssertJ and JUnit-specific APIs dropped).
- Tests stay in `androidUnitTest` only if they exercise genuinely Android behavior (ContentResolver, Android actuals).
- Each phase doc has an inventory: test file → destination (`commonTest` / `androidUnitTest`) → reason.

**Test fixtures:** fixture files (EPUB/RWPM/OPDS samples, sniffing inputs, zip fixtures) are accessed through the `Fixtures` commonTest helper built in phase 00 (Okio `FileSystem` + per-target fixtures-directory injection). Never use `ClassLoader.getResource` in `commonTest`.

## Canonical verification commands

> Task names validated by the phase-00 spike (2026-07-07) on a real conversion of `readium-shared`. Authoritative.

```sh
# Android side of shared (KMP task name; there is no compileDebugKotlinAndroid)
./gradlew :readium:readium-shared:compileAndroidMain
# iOS side of shared
./gradlew :readium:readium-shared:compileKotlinIosSimulatorArm64
# Tests, both platforms (androidHostTest is the KMP name for unit tests, incl. Robolectric)
./gradlew :readium:readium-shared:testAndroidHostTest :readium:readium-shared:iosSimulatorArm64Test
# Whole repo, Android side only (navigator, lcp, adapters, test-app…)
./gradlew compileDebugSources
```

`compileDebugSources` never touches iOS. The iOS-side commands are per-module and this list **grows as modules convert**: phase 08 adds the `:readium:readium-streamer:` and `:readium:readium-opds:` equivalents. This section always holds the authoritative list.

## Publishing

Current coordinates: `org.readium.kotlin-toolkit:<artifactId>:<version>` (see `gradle.properties` `pom.*` and `buildSrc/src/main/kotlin/readium.library-conventions.gradle.kts`). KMP publishing switches to variant-aware Gradle Module Metadata: a root module plus `-android`, `-iosarm64`, `-iossimulatorarm64` artifacts. Two compatibility requirements, both verified in phase 09:

1. A scratch KMP project consumes the artifacts from `commonMain`.
2. An **existing pure-Android Gradle consumer** resolving `org.readium.kotlin-toolkit:readium-shared` keeps working unchanged through variant selection.

## Phase index (dependency order)

| Phase | Doc | Depends on |
|---|---|---|
| 00 Build infrastructure | `phases/00-build-infrastructure.md` | — |
| 01 Foundations | `phases/01-foundations.md` | 00 |
| 02 Url & MediaType | `phases/02-url-mediatype.md` | 01 |
| 03 JSON | `phases/03-json.md` | 02 |
| 04 I/O core | `phases/04-io-core.md` | 01 |
| 05a Zip channels | `phases/05a-zip-channels.md` | 04 |
| 05b Zip compress port | `phases/05b-zip-compress-port.md` | 05a |
| 05c Zip containers | `phases/05c-zip-containers.md` | 05b |
| 06 XML & HTTP | `phases/06-xml-http.md` | 02 (Url), 04 |
| 07 Publication services | `phases/07-publication-services.md` | 03, 06 |
| 08 Streamer & OPDS | `phases/08-streamer-opds.md` | 05c, 06, 07 |
| 09 Release | `phases/09-release.md` | all |

## Key codebase facts

- Gradle project paths are the renamed ones: `:readium:readium-shared`, `:readium:readium-streamer`, `:readium:readium-opds`.
- Versions: Kotlin 2.3.20, AGP 9.0.0, minSdk 23, compileSdk 36.
- **Already present — audit, don't re-add**: kotlinx-serialization-json 1.10.0 and kotlinx-datetime 0.7.1 are in `gradle/libs.versions.toml`; shared already applies the serialization plugin and depends on both. Dates are largely kotlinx.datetime already (~4 residual `java.util.Date` usages).
- Vendored zip stack: `readium/shared/src/main/java/org/readium/r2/shared/util/zip/` — **64 Java files in total**: `compress/` (Commons Compress subset, 53 files) plus `jvm/` channel shims and `FileChannelAdapter.java` (11 files). They are fully self-contained (imports: JDK + each other only). The KMP Android target compiles no Java, so phase 00 parks them in a temporary `java-library` subproject `:readium:readium-shared-zip-legacy`, deleted at the end of phase 05.

## `readium-shared`: deliberately androidMain (final list, phase 07)

Everything else in `readium-shared` lives in `commonMain`. The files below stay in `androidMain` permanently, because they are Android-bound by nature:

- **ContentResolver / content-scheme assets**: `util/content/` (`ContentResource`, `ContentResourceFactory`, `ContentResolverError`), `util/asset/AssetRetrieverAndroid.kt`, `util/asset/Defaults.kt`, `extensions/ContentResolver.kt`.
- **`java.io` / `InputStream` adapters**: `util/data/InputStream.kt`, `util/io/CountingInputStream.kt`, `extensions/InputStream.kt`, `extensions/File.kt`, `extensions/URL.kt`, `extensions/ByteArrayAndroid.kt`, `util/file/FileAndroid.kt`, `util/xml/XmlParserAndroid.kt` (`XmlParser.parse(InputStream)`), `util/data/DecodingAndroid.kt` (`decodeString(java Charset)`).
- **expect/actual Android actuals**: Parcelize (`util/Parcelize.android.kt`, `InstantParceler`, `JsonMapParceler`), Logcat logger (`util/logging/Log.android.kt`), ICU (`util/tokenizer/TextTokenizer.android.kt`, `publication/services/search/StringSearchServiceAndroid.kt`), Bitmap image type (`util/Image.android.kt`, `extensions/Bitmap.kt`, the bitmap half of `DecodingAndroid.kt`), `Language.android.kt`, `CharsetName.android.kt`, `MediaTypeAndroid.kt` (java Charset accessors), `UrlAndroid.kt` (`android.net.Uri`/`java.io.File`/`java.net` conversions), `Http.android.kt`, `IoDispatcher.android.kt`, `OutOfMemoryError.android.kt`, `SniffingAndroid.kt`, `Inflater.android.kt`, R-string accessibility localization (`accessibility/AccessibilityDisplayString.android.kt`, `AccessibilityMetadataDisplayGuideAndroid.kt`).
- **Android-flavored conveniences over common APIs**: `publication/services/CoverServiceAndroid.kt` (`coverAsBitmap()`), `publication/services/InMemoryCacheService.kt` (needs a `Context`), `util/MemoryObserverAndroid.kt` (`ComponentCallbacks2`), `extensions/ExceptionAndroid.kt`.
- **Deprecated JVM compatibility shims**: `util/Instant.kt` (java.util.Date conversions), `util/Lazy.kt` (kotlin.reflect.jvm), `util/Benchmarking.kt` (JVM `String.format`), `KmpMigrationBridge.kt` (the marker annotation declaration).

## Phase-00 spike findings (2026-07-07, binding)

A throwaway-worktree spike converted `readium-shared` for real and reached: whole repo compiling, 569 Robolectric tests passing, iOS klib compiling, one commonTest test running on the iOS simulator. Binding constraints discovered:

- **AGP 9.0 hard-rejects `com.android.library` + `org.jetbrains.kotlin.multiplatform`.** The mandatory route is the **`com.android.kotlin.multiplatform.library`** plugin (applied together with `kotlin("multiplatform")`). The `android.builtInKotlin=false` bypass is not viable: it is repo-wide and the other modules already rely on AGP 9 built-in Kotlin.
- New DSL lives inside `kotlin { androidLibrary { … } }`: `namespace` (per module), `compileSdk`, `minSdk`, `androidResources.enable = true` (required — shared uses `R.string` resources; disabled by default), and Robolectric host tests via `withHostTestBuilder {}.configure { isIncludeAndroidResources = true }`.
- **`jvmTarget` defaults to 21** on the android compilations and must be forced to `JVM_11` (`compilations.configureEach { compileTaskProvider.configure { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } } }`), otherwise every downstream module fails with “Cannot inline bytecode built with JVM target 21”.
- KMP source-set names: `androidMain`, `androidHostTest` (not `androidUnitTest`), `androidDeviceTest`. Layout that worked: `src/main/java` → `src/androidMain/kotlin`, manifest → `src/androidMain/AndroidManifest.xml`, `res` → `src/androidMain/res`, `src/test/java` → `src/androidHostTest/kotlin`.
- commonTest uses `implementation(kotlin("test"))`; JUnit-flavored `kotlin-test-junit` stays in `androidHostTest` only.
- The vanniktech publish plugin creates the KMP publications (root + android + iOS variants) without extra config; local `publishToMavenLocal` fails only on missing GPG signatory, which is expected off-CI.
- Not carried over by the new DSL and needing a decision in phase 00: `resourcePrefix`, `buildConfig`, release `buildTypes`/proguard files, `coreLibraryDesugaring`. The spike compiled and passed tests without all four.
- `util/Url.kt` wraps `android.net.Uri` + `java.net.URI`; its `normalize()` delegates to `java.io.File.normalize()` — see phase 02's high-risk parity task.
- `streamer` and `opds` are nearly platform-clean (3 and 2 files with platform imports); PDF parsing depends only on shared's `PdfDocumentFactory` interface (interface goes common; PDFium/PSPDFKit adapters stay Android).
