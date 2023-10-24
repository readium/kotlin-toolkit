/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import java.io.File
import java.util.zip.ZipException
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.util.MessageError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever

/**
 * An [ArchiveFactory] to open local ZIP files with Java's [ZipFile].
 */
public class FileZipArchiveFactory(
    private val mediaTypeRetriever: MediaTypeRetriever
) : ArchiveFactory {

    override suspend fun create(resource: Resource, password: String?): Container? {
        if (password != null) {
            return null
        }

        return resource.source?.toFile()
            ?.let { open(it) }
            ?.getOrNull()
    }

    override suspend fun create(
        resource: Resource,
        password: String?,
        mediaType: MediaType
    ): Try<Container, ArchiveFactory.Error> {
        if (password != null) {
            return Try.failure(ArchiveFactory.Error.PasswordsNotSupported())
        }

        if (!mediaType.isZip) {
            return Try.failure(ArchiveFactory.Error.FormatNotSupported())
        }

        return resource.source?.toFile()
            ?.let { open(it) }
            ?: Try.Failure(
                ArchiveFactory.Error.FormatNotSupported(
                    MessageError("Resource not supported because file cannot be directly accessed.")
                )
            )
    }

    // Internal for testing purpose
    internal suspend fun open(file: File): Try<Container, ArchiveFactory.Error> =
        withContext(Dispatchers.IO) {
            try {
                val archive = JavaZipContainer(ZipFile(file), file, mediaTypeRetriever)
                Try.success(archive)
            } catch (e: ZipException) {
                Try.failure(ArchiveFactory.Error.FormatNotSupported(e))
            } catch (e: SecurityException) {
                Try.failure(ArchiveFactory.Error.ResourceReading(ResourceError.Forbidden(e)))
            } catch (e: Exception) {
                Try.failure(ArchiveFactory.Error.ResourceReading(ResourceError.Other(e)))
            }
        }
}
