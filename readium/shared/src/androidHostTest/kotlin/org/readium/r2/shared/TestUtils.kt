/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared

import java.io.File
import java.net.URL
import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.util.resource.Resource

class ClasspathFixtures(val path: String? = null) {

    fun urlAt(resourcePath: String): URL {
        val path = this.path?.let { "$it/$resourcePath" } ?: resourcePath
        return ClasspathFixtures::class.java.getResource(path)!!
    }

    fun pathAt(resourcePath: String): String =
        urlAt(resourcePath).path

    fun fileAt(resourcePath: String): File =
        File(pathAt(resourcePath))
}

internal fun Resource.readBlocking(range: LongRange? = null) = runBlocking { read(range) }

internal fun Resource.lengthBlocking() = runBlocking { length() }
