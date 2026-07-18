/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer

import okio.ByteString
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * Access to the fixture files stored under `src/commonTest/fixtures/<group>/`.
 *
 * This works on every test platform (Android host tests and the iOS simulator): the fixtures
 * root directory is injected by the `readium.multiplatform-conventions` Gradle plugin through
 * the `READIUM_FIXTURES_DIR` environment variable. Never use `ClassLoader.getResource` in
 * `commonTest`.
 *
 * Note: this helper is intentionally duplicated in each KMP module's commonTest
 * (readium-shared, readium-streamer, readium-opds) because Kotlin test source sets are not
 * published, and a dedicated test-fixtures module is not worth the extra build complexity for
 * ~40 lines. If you change the mechanism here, update the sibling copies.
 */
public class Fixtures(private val group: String) {

    /** Absolute path to the fixture named [name] in this group. */
    public fun path(name: String): Path =
        fixturesRoot / group / name

    /** Reads the content of the fixture named [name] in this group. */
    public fun read(name: String): ByteString =
        fixturesFileSystem.read(path(name)) { readByteString() }

    private val fixturesRoot: Path get() =
        checkNotNull(getenv("READIUM_FIXTURES_DIR")) {
            "The READIUM_FIXTURES_DIR environment variable is not set. " +
                "It should be injected on test tasks by the readium.multiplatform-conventions plugin."
        }.toPath()
}

internal expect val fixturesFileSystem: FileSystem

internal expect fun getenv(name: String): String?
