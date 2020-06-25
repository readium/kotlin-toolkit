/*
 * Module: r2-opds-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.opds

import java.io.File
import java.io.InputStream
import java.net.URL

class Fixtures(val path: String? = null) {

    fun urlAt(resourcePath: String): URL {
        val path = this.path?.let { "$it/$resourcePath" } ?: resourcePath
        return Fixtures::class.java.getResource(path)!!
    }

    fun pathAt(resourcePath: String): String =
        urlAt(resourcePath).path

    fun fileAt(resourcePath: String): File =
        File(pathAt(resourcePath))

    fun bytesAt(resourcePath: String): ByteArray =
        fileAt(resourcePath).readBytes()

}
