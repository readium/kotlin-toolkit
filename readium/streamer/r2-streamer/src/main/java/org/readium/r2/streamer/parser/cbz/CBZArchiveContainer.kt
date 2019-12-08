/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.cbz

import org.readium.r2.streamer.container.ArchiveContainer
import org.readium.r2.streamer.container.Container


interface CBZContainer : Container {
    val files: List<String>
}

class CBZArchiveContainer(path: String) : CBZContainer, ArchiveContainer(path, CBZConstant.mimetypeCBZ) {
    override val files: List<String>
        get() {
            val filesList = mutableListOf<String>()
            archive.let {
                val listEntries = it.entries()
                listEntries.toList().forEach { entry -> filesList.add(entry.toString()) }
            }
            return filesList
        }
}