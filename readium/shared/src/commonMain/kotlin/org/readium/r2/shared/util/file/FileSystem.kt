/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.file

import okio.FileSystem
import okio.SYSTEM

/**
 * File system used by the file-backed resources and containers.
 *
 * Okio is an implementation detail: the public API only exposes Readium's own `Resource` and
 * `Container` abstractions, with `file://` [org.readium.r2.shared.util.AbsoluteUrl]s as the
 * canonical cross-platform file reference.
 */
internal val fileSystem: FileSystem = FileSystem.SYSTEM
