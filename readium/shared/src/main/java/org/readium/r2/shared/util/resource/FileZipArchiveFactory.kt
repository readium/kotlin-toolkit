/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import java.io.File
import java.io.IOException
import java.util.zip.ZipException
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.util.MessageError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeRetriever

/**
 * An [ArchiveFactory] to open local ZIP files with Java's [ZipFile].
 */
public class FileZipArchiveFactory(
    private val mediaTypeRetriever: MediaTypeRetriever
) : ArchiveFactory {

    override suspend fun create(
        resource: Resource,
        archiveType: MediaType?,
        password: String?
    ): Try<ArchiveFactory.Result, ArchiveFactory.Error> {
        if (archiveType != null && !archiveType.matches(MediaType.ZIP)) {
            return Try.failure(ArchiveFactory.Error.FormatNotSupported())
        }

        if (password != null) {
            return Try.failure(ArchiveFactory.Error.PasswordsNotSupported())
        }

        val file = resource.source?.toFile()
            ?: return Try.Failure(
                ArchiveFactory.Error.FormatNotSupported(
                    MessageError("Resource not supported because file cannot be directly accessed.")
                )
            )

        val container = open(file)
            .getOrElse { return Try.failure(it) }

        return Try.success(ArchiveFactory.Result(MediaType.ZIP, container))
    }

    // Internal for testing purpose
    internal suspend fun open(file: File): Try<Container, ArchiveFactory.Error> =
        withContext(Dispatchers.IO) {
            try {
                val archive = JavaZipContainer(ZipFile(file), file, mediaTypeRetriever)
                Try.success(archive)
            } catch (e: ZipException) {
                Try.failure(ArchiveFactory.Error.ResourceError(ResourceError.InvalidContent(e)))
            } catch (e: SecurityException) {
                Try.failure(ArchiveFactory.Error.ResourceError(ResourceError.Forbidden(e)))
            } catch (e: IOException) {
                Try.failure(ArchiveFactory.Error.ResourceError(ResourceError.Filesystem(e)))
            } catch (e: Exception) {
                Try.failure(ArchiveFactory.Error.ResourceError(ResourceError.Other(e)))
            }
        }
}
