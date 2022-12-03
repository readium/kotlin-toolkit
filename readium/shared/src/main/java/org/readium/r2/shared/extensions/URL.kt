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
import java.net.URL

fun URL.removeLastComponent(): URL {
    val lastPathComponent = path.split("/")
        .lastOrNull { it.isNotEmpty() }
        ?: return this

    return URL(
        toString()
            .removeSuffix("?$query")
            .removeSuffix("/")
            .removeSuffix(lastPathComponent)
    )
}

/** Returns the file extension of the URL. */
val URL.extension: String? get() =
    File(path).extension.ifBlank { null }
