/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.archive

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.zip.ZipException
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.util.MessageError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.Blob
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.FileSystemError
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeHints
import org.readium.r2.shared.util.mediatype.MediaTypeSnifferError
import org.readium.r2.shared.util.resource.MediaTypeRetriever
import org.readium.r2.shared.util.resource.Resource

/**
 * An [ArchiveFactory] to open local ZIP files with Java's [ZipFile].
 */
public class FileZipArchiveProvider(
    private val mediaTypeRetriever: MediaTypeRetriever = MediaTypeRetriever()
) : ArchiveProvider {

    override fun sniffHints(hints: MediaTypeHints): Try<MediaType, MediaTypeSnifferError.NotRecognized> =
        ZipHintMediaTypeSniffer.sniffHints(hints)

    override suspend fun sniffBlob(blob: Blob): Try<MediaType, MediaTypeSnifferError> {
        val file = blob.source?.toFile()
            ?: return Try.Failure(MediaTypeSnifferError.NotRecognized)

        return withContext(Dispatchers.IO) {
            try {
                FileZipContainer(ZipFile(file), file, mediaTypeRetriever)
                Try.success(MediaType.ZIP)
            } catch (e: ZipException) {
                Try.failure(MediaTypeSnifferError.NotRecognized)
            } catch (e: SecurityException) {
                Try.failure(
                    MediaTypeSnifferError.Read(
                        ReadError.Access(FileSystemError.Forbidden(e))
                    )
                )
            } catch (e: IOException) {
                Try.failure(
                    MediaTypeSnifferError.Read(
                        ReadError.Access(FileSystemError.IO(e))
                    )
                )
            }
        }
    }

    override suspend fun create(
        blob: Blob,
        password: String?
    ): Try<Container<Resource>, ArchiveFactory.Error> {
        if (password != null) {
            return Try.failure(ArchiveFactory.Error.PasswordsNotSupported())
        }

        val file = blob.source?.toFile()
            ?: return Try.Failure(
                ArchiveFactory.Error.FormatNotSupported(
                    MessageError("Resource not supported because file cannot be directly accessed.")
                )
            )

        val container = open(file)
            .getOrElse { return Try.failure(it) }

        return Try.success(container)
    }

    // Internal for testing purpose
    internal suspend fun open(file: File): Try<Container<Resource>, ArchiveFactory.Error> =
        withContext(Dispatchers.IO) {
            try {
                val archive = FileZipContainer(ZipFile(file), file, mediaTypeRetriever)
                Try.success(archive)
            } catch (e: FileNotFoundException) {
                Try.failure(
                    ArchiveFactory.Error.ReadError(
                        ReadError.Access(FileSystemError.NotFound(e))
                    )
                )
            } catch (e: ZipException) {
                Try.failure(
                    ArchiveFactory.Error.ReadError(
                        ReadError.Decoding(e)
                    )
                )
            } catch (e: SecurityException) {
                Try.failure(
                    ArchiveFactory.Error.ReadError(
                        ReadError.Access(FileSystemError.Forbidden(e))
                    )
                )
            } catch (e: IOException) {
                Try.failure(
                    ArchiveFactory.Error.ReadError(
                        ReadError.Access(FileSystemError.IO(e))
                    )
                )
            }
        }
}
