/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared

import okio.FileSystem

internal actual val fixturesFileSystem: FileSystem = FileSystem.SYSTEM

internal actual fun getenv(name: String): String? = System.getenv(name)
