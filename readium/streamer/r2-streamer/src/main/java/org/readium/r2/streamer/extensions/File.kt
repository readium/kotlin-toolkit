/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.extensions

import java.io.File
import java.util.Locale

internal val File.lowercasedExtension: String
    get() = extension.toLowerCase(Locale.getDefault())

internal val File.isHiddenOrThumbs: Boolean
    get() = name.let { it.startsWith(".") || it == "Thumbs.db" }

/**
 * Returns a [File] to the first component of the [File]'s path,
 * regardless of whether it is a directory or a file.
 */
internal val File.firstComponent: File
    get() = parent.takeUnless { it == "/" }
        ?.let { File(it).firstComponent }
        ?: this
