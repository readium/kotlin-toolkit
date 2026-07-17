/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.io

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Dispatcher for offloading blocking I/O operations.
 *
 * `Dispatchers.IO` exists on both the JVM and Native, but is not visible from common code with
 * the current kotlinx-coroutines metadata; this bridges it.
 */
internal expect val IoDispatcher: CoroutineDispatcher
