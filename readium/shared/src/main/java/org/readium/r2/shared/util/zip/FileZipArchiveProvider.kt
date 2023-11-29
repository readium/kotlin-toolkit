/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.zip.ZipException
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.archive.ArchiveFactory
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.file.FileSystemError
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeSnifferError
import org.readium.r2.shared.util.resource.Resource

/**
 * An [ArchiveFactory] to open local ZIP files with Java's [ZipFile].
 */
internal class FileZipArchiveProvider {

    suspend fun sniffFile(file: File): Try<MediaType, MediaTypeSnifferError> {
        return withContext(Dispatchers.IO) {
            try {
                FileZipContainer(ZipFile(file), file)
                Try.success(MediaType.ZIP)
            } catch (e: ZipException) {
                Try.failure(MediaTypeSnifferError.NotRecognized)
            } catch (e: SecurityException) {
                Try.failure(
                    MediaTypeSnifferError.Reading(
                        ReadError.Access(FileSystemError.Forbidden(e))
                    )
                )
            } catch (e: IOException) {
                Try.failure(
                    MediaTypeSnifferError.Reading(
                        ReadError.Access(FileSystemError.IO(e))
                    )
                )
            }
        }
    }

    suspend fun create(
        mediaType: MediaType,
        file: File
    ): Try<Container<Resource>, ArchiveFactory.Error> {
        if (mediaType != MediaType.ZIP) {
            return Try.failure(
                ArchiveFactory.Error.FormatNotSupported(mediaType)
            )
        }

        val container = open(file)
            .getOrElse { return Try.failure(it) }

        return Try.success(container)
    }

    // Internal for testing purpose
    internal suspend fun open(file: File): Try<Container<Resource>, ArchiveFactory.Error> =
        withContext(Dispatchers.IO) {
            try {
                val archive = FileZipContainer(ZipFile(file), file)
                Try.success(archive)
            } catch (e: FileNotFoundException) {
                Try.failure(
                    ArchiveFactory.Error.Reading(
                        ReadError.Access(FileSystemError.NotFound(e))
                    )
                )
            } catch (e: ZipException) {
                Try.failure(
                    ArchiveFactory.Error.Reading(
                        ReadError.Decoding(e)
                    )
                )
            } catch (e: SecurityException) {
                Try.failure(
                    ArchiveFactory.Error.Reading(
                        ReadError.Access(FileSystemError.Forbidden(e))
                    )
                )
            } catch (e: IOException) {
                Try.failure(
                    ArchiveFactory.Error.Reading(
                        ReadError.Access(FileSystemError.IO(e))
                    )
                )
            }
        }
}
