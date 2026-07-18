/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util.zip

import kotlinx.coroutines.CancellationException
import okio.FileNotFoundException
import okio.IOException
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.findInstance
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.archive.ArchiveOpener
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.file.FileSystemError
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.Specification
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.zip.compress.archivers.zip.ZipException

/**
 * An [ArchiveOpener] to open local ZIP files.
 *
 * Since phase 05c of the KMP migration, it runs on the same ported zip stack as
 * [StreamingZipArchiveProvider] (option (a) of the phase doc: the `java.util.zip` file-based
 * implementation was deleted after benchmarking showed no meaningful regression), and only
 * differs by its file-specific error mapping.
 */
internal class FileZipArchiveProvider {

    private val streamingZipArchiveProvider = StreamingZipArchiveProvider()

    suspend fun sniffOpen(file: AbsoluteUrl): Try<Container<Resource>, ArchiveOpener.SniffOpenError> =
        openContainer(file).mapFailure { error ->
            when (error) {
                is OpenFileError.FileNotFound ->
                    ArchiveOpener.SniffOpenError.Reading(
                        ReadError.Access(FileSystemError.FileNotFound(error.exception))
                    )
                is OpenFileError.NotAZip ->
                    ArchiveOpener.SniffOpenError.NotRecognized
                is OpenFileError.IO ->
                    ArchiveOpener.SniffOpenError.Reading(
                        ReadError.Access(FileSystemError.IO(error.exception))
                    )
            }
        }

    suspend fun open(
        format: Format,
        file: AbsoluteUrl,
    ): Try<Container<Resource>, ArchiveOpener.OpenError> {
        if (!format.conformsTo(Specification.Zip)) {
            return Try.failure(
                ArchiveOpener.OpenError.FormatNotSupported(format)
            )
        }

        return open(file)
    }

    // Internal for testing purpose
    internal suspend fun open(file: AbsoluteUrl): Try<Container<Resource>, ArchiveOpener.OpenError> =
        openContainer(file).mapFailure { error ->
            when (error) {
                is OpenFileError.FileNotFound ->
                    ArchiveOpener.OpenError.Reading(
                        ReadError.Access(FileSystemError.FileNotFound(error.exception))
                    )
                is OpenFileError.NotAZip ->
                    ArchiveOpener.OpenError.Reading(
                        ReadError.Decoding(error.exception)
                    )
                is OpenFileError.IO ->
                    ArchiveOpener.OpenError.Reading(
                        ReadError.Access(FileSystemError.IO(error.exception))
                    )
            }
        }

    private sealed class OpenFileError {
        /** The file does not exist on the file system. */
        class FileNotFound(val exception: Exception) : OpenFileError()

        /** The file is not a ZIP archive, or is too malformed to be read as one. */
        class NotAZip(val exception: Exception) : OpenFileError()

        /** Any other file system error. */
        class IO(val exception: Exception) : OpenFileError()
    }

    private suspend fun openContainer(
        file: AbsoluteUrl,
    ): Try<Container<Resource>, OpenFileError> =
        try {
            Try.success(streamingZipArchiveProvider.openFile(file))
        } catch (e: CancellationException) {
            throw e
        } catch (e: FileNotFoundException) {
            Try.failure(OpenFileError.FileNotFound(e))
        } catch (e: Exception) {
            when {
                // ZipFile wraps its errors in a plain IOException, so look through the causes.
                e.findInstance<ZipException>() != null ->
                    Try.failure(OpenFileError.NotAZip(e))
                e is IOException ->
                    Try.failure(OpenFileError.IO(e))
                else ->
                    // The zip parser signals malformed archives with IndexOutOfBoundsException
                    // (ZipBuffer), IllegalArgumentException or IllegalStateException too.
                    Try.failure(OpenFileError.NotAZip(e))
            }
        }
}
