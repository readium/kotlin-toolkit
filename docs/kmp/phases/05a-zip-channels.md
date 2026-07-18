# Phase 05a — Zip: channel abstractions onto Okio

Read `docs/kmp/PLAN.md` first. Depends on phase 04. First of three zip phases; the goal of 05a–05c is to delete `:readium:readium-shared-zip-legacy` entirely.

## Background

The vendored zip stack (temporarily parked in `readium/shared-zip-legacy/`) is built on a vendored mirror of `java.nio.channels` in package `org.readium.r2.shared.util.zip.jvm` (10 files: `SeekableByteChannel`, `ReadableByteChannel`, `ByteChannel`, `Channel`, `WritableByteChannel` + 5 exception types). On top of it, Kotlin adapters live in shared androidMain: `ReadableChannelAdapter.kt`, `CachingReadableChannel.kt`, `BufferedReadableChannel.kt`, `FileChannelAdapter.java`.

## Task 5a.1 — Kotlin common channel interfaces

Translate the `jvm/` package to Kotlin in **commonMain**, same package (`org.readium.r2.shared.util.zip.jvm` — keep it; renaming is a later cleanup): interfaces are tiny (single-method); exceptions become Kotlin classes extending `IOException` equivalents (use `okio.IOException` for the KMP-safe base). `ByteBuffer` is the hard dependency: the channel methods take `java.nio.ByteBuffer`. Two options — **decide by inspecting call sites in `compress/`**:

- Preferred: replace `ByteBuffer` parameters with a minimal Readium-owned buffer type or plain `ByteArray + offset/length` if `compress/` uses only simple get/put/position/limit/flip operations.
- If `compress/` leans heavily on ByteBuffer semantics (slice, duplicate, byte order), port a minimal `ZipBuffer` class in commonMain implementing exactly the used subset (enumerate operations by grep: `\.flip()|\.slice()|\.order(|\.duplicate()` etc. across `compress/`).

Document the chosen buffer mapping in this file for 05b agents.

### Buffer mapping decided during 05a (binding for 05b)

A grep survey of `compress/` (2026-07-18) showed only simple `ByteBuffer` semantics are used — `allocate`, `wrap(array)`, `wrap(array, offset, length)`, `position`/`limit`/`capacity`/`remaining`/`hasRemaining`, `clear`/`flip`/`rewind`, relative single & bulk `get`/`put`, `put(ByteBuffer)`, `array()`/`arrayOffset()`. **No** `order()`/`ByteOrder`, multi-byte accessors (`getShort`…), `slice()`, `duplicate()`, `mark()`/`reset()`, `compact()`, nor caught `BufferUnderflow/OverflowException` anywhere (`.putShort`/`.putLong` hits are `ZipShort`/`ZipLong` statics, not `ByteBuffer`).

The mapping is a minimal Readium-owned buffer: **`org.readium.r2.shared.util.zip.jvm.ZipBuffer`** (commonMain, internal), array-backed, implementing exactly that subset with `java.nio.Buffer` invariants (`0 <= position <= limit <= capacity`). Differences 05b must know:

- Out-of-bounds relative accesses throw `IndexOutOfBoundsException` instead of `BufferUnderflowException`/`BufferOverflowException` (never caught by the zip stack).
- `arrayOffset()` is always 0 (no views).
- `CharBuffer`/`CharsetEncoder` in `NioZipEncoding` has no `ZipBuffer` equivalent — per the 05b doc, replace `java.nio.charset` with a common encoding table (CP437/UTF-8).

Additionally, the translated channel interfaces are **suspend-first**: `read`/`write`/`position`/`size`/`truncate` are `suspend fun` (mandated by the plan's concurrency ground rules — the adapters bridge to the suspending `Readable` and `runBlocking` is banned in common code). `Channel` extends Readium's `org.readium.r2.shared.util.Closeable` (non-suspending `close()`, never throws checked exceptions) and exposes `isOpen` as a property. The 05b port must propagate `suspend` through the `compress/` call graph (`ZipFile`, input streams…).

**Thread-safety contract:** the leaf adapters (`ReadableChannelAdapter`, `FileChannelAdapter`) are **not** thread-safe — mutable `position` is unguarded, mirroring the legacy adapters which also had no internal locking. Callers must serialize accesses externally: today the wrapping `CachingReadableChannel`/`BufferedReadableChannel` (internal `Mutex`) and `StreamingZipContainer` (its own `Mutex`) do. Any new 05b/05c consumer driving a leaf adapter directly must guarantee a single caller at a time or wrap it.

## Task 5a.2 — Port the Kotlin adapters

`ReadableChannelAdapter`, `CachingReadableChannel`, `BufferedReadableChannel` (androidMain) → commonMain onto the new interfaces, replacing `java.io`/`java.nio` internals with Okio/`ByteArray`. `FileChannelAdapter.java` → Kotlin in commonMain on `okio.FileHandle`.

These adapt Readium's `Readable` to channels — the seam that makes zip-over-HTTP work. Preserve the buffering/caching behavior exactly (read sizes, cache invalidation) — write characterization tests first in commonTest (a `Readable` fake + read-pattern assertions).

## Task 5a.3 — Compatibility with the legacy project

Until 05b lands, shared androidMain code (`FileZipContainer`, `StreamingZipContainer`…) still calls the **legacy Java** channels. Options, pick the one that compiles cleanly:

1. Keep legacy code using legacy channels; the new common channels live alongside unused-by-legacy until 05c swaps the containers. (Simplest — no interop needed.)
2. If names clash on the Android classpath (same FQN in commonMain and in the legacy jar), rename the legacy package in the legacy project (`…zip.legacyjvm`) with a mechanical find-replace inside `shared-zip-legacy` + the androidMain call sites.

**Decision taken during 05a:** option 2. The commonMain channels reuse the same FQNs, so the legacy package was renamed `org.readium.r2.shared.util.zip.legacyjvm` (mechanical find-replace in `shared-zip-legacy` + androidMain call sites); `FileChannelAdapter.java` moved into `legacyjvm` too for the same reason (its constructor became `public` since callers are no longer in the same package). The three Kotlin adapters were ported to commonMain and their blocking originals kept in androidMain as `LegacyReadableChannelAdapter` / `LegacyCachingReadableChannel` / `LegacyBufferedReadableChannel` (used by `StreamingZipArchiveProvider` until 05c, then deleted with the legacy project). This rename had to land *before* the commonMain channels to keep the repo green, so 5a.3 was executed first.

**Phase acceptance:** commonTest channel/adapter characterization tests green on both platforms; canonical commands pass; no behavior change on Android (existing zip tests still green via legacy path).
