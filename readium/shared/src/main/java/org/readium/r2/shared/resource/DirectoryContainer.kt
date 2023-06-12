/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.resource

import android.content.ContentResolver
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.extensions.isParentOf
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url

/**
 * A file system directory as a [Container].
 */
internal class DirectoryContainer(
    private val root: File,
    private val entries: List<File>
) : Container {

    private inner class FailureEntry(
        override val path: String
    ) : Container.Entry,
        Resource by FailureResource(
            Resource.Exception.NotFound(Exception("No file entry at path $path."))
        )

    private inner class SuccessEntry(
        override val file: File
    ) : Container.Entry, Resource by FileResource(file) {

        override val path: String get() = file.relativeTo(root).path

        override suspend fun close() {}
    }

    override suspend fun name(): ResourceTry<String> =
        ResourceTry.success(root.name)

    override suspend fun entries(): List<Container.Entry> =
        entries.map { SuccessEntry(it) }.toList()

    override suspend fun entry(path: String): Container.Entry {
        val file = File(root, path)

        return if (!root.isParentOf(file) || !file.isFile)
            FailureEntry(path)
        else
            SuccessEntry(file)
    }

    override suspend fun close() {}

    companion object {

        suspend operator fun invoke(root: File): Try<DirectoryContainer, Exception> =
            withContext(Dispatchers.IO) {
                try {
                    val entries = root.walk()
                        .filter { it.isFile }
                        .toList()
                    val container = DirectoryContainer(root, entries)
                    Try.success(container)
                } catch (e: Exception) {
                    Try.failure(e)
                }
            }
    }
}

class DirectoryContainerFactory : ContainerFactory {

    override suspend fun create(url: Url): Try<Container, Exception> {
        if (url.scheme != ContentResolver.SCHEME_FILE) {
            return Try.failure(Exception("Scheme not supported"))
        }

        val file = File(url.path)
        return create(file)
    }

    // Internal for testing purpose
    internal suspend fun create(file: File): Try<Container, Exception> {
        return DirectoryContainer(file)
    }
}
