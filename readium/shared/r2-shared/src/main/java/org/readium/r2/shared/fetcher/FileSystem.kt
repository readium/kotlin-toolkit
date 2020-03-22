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

    constructor(path: String, href: String) : this(mapOf(href to path))

    override fun get(link: Link): Resource =
        files[link.href]?.let { FileResource(link, it) } ?: NotFoundResource(link)
}

class DirectoryFetcher(val path: String) : Fetcher {

    override fun get(link: Link): Resource = FileResource(link, File(path, link.href).absolutePath)
}

private class FileResource(override val link: Link, val path: String) : ResourceImpl() {

    private val file = File(path)

    override fun stream(): InputStream? =
        try {
            file.inputStream().buffered()
        } catch (e: FileNotFoundException) {
            null
        }

    override val metadataLength: Long? by lazy {
        if (Build.VERSION.SDK_INT > 25) {
            // this version reads file's attributes in bulk, so consistency is ensured
            try {
                val attributes = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
                if (attributes.isRegularFile) attributes.size() else null
            } catch (e: Exception) {
                null
            }
        } else {
            file.length().takeIf { file.isFile && it != 0L }
        }
    }
}
