/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared

/**
 * Thrown when the runtime runs out of memory.
 *
 * The Kotlin standard library declares [OutOfMemoryError] on both the JVM and Native, but not in
 * common code. This expect declaration bridges the platform types so that common code can catch
 * and wrap them.
 */
public expect open class OutOfMemoryError : Error {
    public constructor()
    public constructor(message: String?)
}
