/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.extensions

import java.io.File

/**
 * Returns whether the `other` is a descendant of this file.
 */
fun File.isParentOf(other: File): Boolean {
    val canonicalThis = canonicalFile
    var parent = other.canonicalFile.parentFile
    while (parent != null) {
        if (parent == canonicalThis) {
            return true
        }
        parent = parent.parentFile
    }
    return false
}
