# readium-shared-zip-legacy

**Temporary** subproject hosting the vendored Java zip stack (Commons Compress subset and `java.nio` channel shims) while `readium-shared` migrates to Kotlin Multiplatform, whose Android target compiles no Java.

This project is not published and will be deleted at the end of phase 05 of the KMP migration, when the zip stack is ported to pure Kotlin in `commonMain`. See `docs/kmp/PLAN.md`.
