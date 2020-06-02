/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.container

import org.readium.r2.shared.RootFile
import org.readium.r2.shared.drm.DRM
import org.readium.r2.shared.extensions.tryOrNull
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URL

/** Container providing access to standalone files. */
class FileContainer(path: String, mimetype: String) : Container {

    /** Two types of file served: from a path, or from a bytes buffer. */
    sealed class File {
        abstract val bytes: ByteArray?
        abstract val length: Long?
        abstract fun stream(): InputStream?

        data class Path(val path: String) : File() {

            override val bytes: ByteArray? get() =
                tryOrNull { URL(path).readBytes() }

            override val length: Long? =
                tryOrNull { File(path).length() }

            override fun stream(): InputStream? =
                tryOrNull { FileInputStream(path).buffered() }

        }

        data class Bytes(override val bytes: ByteArray) : File() {
            override val length: Long? = bytes.size.toLong()
            override fun stream(): InputStream? = ByteArrayInputStream(bytes)
        }
    }

    /** Maps between container relative paths, and the matching file to serve. */
    var files = mutableMapOf<String, File>()

    override var rootFile = RootFile(rootPath = path, mimetype = mimetype)
    override var drm: DRM? = null

    override fun data(relativePath: String): ByteArray =
        fileAt(relativePath)?.bytes
            ?: throw ContainerError.fileNotFound

    override fun dataLength(relativePath: String): Long =
        fileAt(relativePath)?.length
            ?: throw ContainerError.fileNotFound

    override fun dataInputStream(relativePath: String): InputStream =
        fileAt(relativePath)?.stream()
            ?: throw ContainerError.fileNotFound

    private fun fileAt(path: String): File? =
        files[path] ?: files["/$path"]

}
