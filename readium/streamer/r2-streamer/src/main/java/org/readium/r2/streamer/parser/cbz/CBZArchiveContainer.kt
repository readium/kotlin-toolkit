/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mickaël Menu
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.cbz

import org.readium.r2.shared.format.MediaType
import org.readium.r2.streamer.container.ArchiveContainer
import org.readium.r2.streamer.container.Container


interface CBZContainer : Container {
    val files: List<String>
}

class CBZArchiveContainer(path: String) : CBZContainer, ArchiveContainer(path, MediaType.CBZ.toString()) {

    override val files: List<String>
        get() = archive.entries().toList()
            .filter { !it.isDirectory }
            .map { it.name }

}