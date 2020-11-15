/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.extensions

import org.readium.r2.shared.publication.Link
import java.io.File


/** Returns a [File] to the directory containing all links, if there is such a directory. */
internal fun List<Link>.hrefCommonFirstComponent(): File? =
    map { it.href.removePrefix("/").substringBefore("/")  }
        .distinct()
        .takeIf { it.size == 1 }
        ?.firstOrNull()
        ?.let { File(it) }
