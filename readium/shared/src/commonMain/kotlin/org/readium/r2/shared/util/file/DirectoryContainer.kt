/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.file

import kotlinx.coroutines.withContext
import okio.FileNotFoundException
import okio.IOException
import okio.Path.Companion.toPath
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.RelativeUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.fromFilePath
import org.readium.r2.shared.util.io.IoDispatcher
import org.readium.r2.shared.util.resource.Resource

/**
 * A file system directory as a [Container].
 */
public class DirectoryContainer(
    private val root: AbsoluteUrl,
    override val entries: Set<Url>,
) : Container<Resource> {

    override fun get(url: Url): Resource? = url
        .takeIf { it in entries }
        ?.let { it as? RelativeUrl }
        ?.let { root.resolve(it) }
        ?.let { FileResource(it) }

    override fun close() {}

    public companion object {

        /**
         * Creates a [DirectoryContainer] serving the files of the directory at [root].
         *
         * Returns a [FileSystemError.FileNotFound] failure if [root] does not exist on the file
         * system.
         */
        public suspend operator fun invoke(root: AbsoluteUrl): Try<DirectoryContainer, FileSystemError> {
            require(root.isFile) { "DirectoryContainer requires a file:// URL, was: $root" }

            val rootUrl = checkNotNull(
                AbsoluteUrl.fromFilePath(checkNotNull(root.path), isDirectory = true)
            )
            val rootPath = checkNotNull(root.path).toPath()

            val entries =
                try {
                    withContext(IoDispatcher) {
                        fileSystem.listRecursively(rootPath)
                            .filter { fileSystem.metadataOrNull(it)?.isRegularFile == true }
                            .mapNotNull { path ->
                                AbsoluteUrl.fromFilePath(path.toString())
                                    ?.let { rootUrl.relativize(it) }
                            }
                            .toSet()
                    }
                } catch (e: FileNotFoundException) {
                    return Try.failure(FileSystemError.FileNotFound(e))
                } catch (e: IOException) {
                    return Try.failure(FileSystemError.IO(e))
                }
            val container = DirectoryContainer(rootUrl, entries)
            return Try.success(container)
        }
    }
}
