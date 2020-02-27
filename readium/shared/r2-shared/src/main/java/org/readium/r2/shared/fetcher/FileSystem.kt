/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import android.os.Build
import org.readium.r2.shared.publication.Link
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

class FileFetcher(val files: Map<String, String>) : Fetcher {

    constructor(file: String, href: String) : this(mapOf(href to file))

    override fun fetch(link: Link): ResourceHandle? = FileHandle(link.href)
}

class DirectoryFetcher(val directory: String) : Fetcher {

    companion object {
        fun fromPath(path: String): DirectoryFetcher? = if (File(path).isDirectory) fromPath(path) else null
    }

    override fun fetch(link: Link): ResourceHandle? = FileHandle(link.href, directory)


}

private class FileHandle(href: String, val parent: String? = null) : ResourceHandle(href) {

    private val file = File(parent, href)

    override fun stream(): InputStream? = streamOfFile(file)

    override val length: Long? by lazy { lengthOfFile(file) ?: super.length }

    private fun streamOfFile(file: File): InputStream? =
        try {
            file.inputStream().buffered()
        } catch (e: FileNotFoundException) {
            null
        }

    private fun lengthOfFile(file: File): Long? =
        if (Build.VERSION.SDK_INT > 25) {
            // this version reads file's attributes in bulk, so consistency is ensured
            try {
                val attributes = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
                if (attributes.isRegularFile) attributes.size() else null
            } catch (e: Exception) {
                null
            }
        } else if (!file.isFile) {
            null
        } else {
            file.length().takeUnless { it == 0L }
        }
}
