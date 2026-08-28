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
import org.readium.r2.shared.util.archive.ArchiveOpener
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.file.FileSystemError
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.Specification
import org.readium.r2.shared.util.resource.Resource

/**
 * An [ArchiveOpener] to open local ZIP files with Java's [ZipFile].
 */
internal class FileZipArchiveProvider {

    suspend fun sniffOpen(file: File): Try<Container<Resource>, ArchiveOpener.SniffOpenError> {
        return withContext(Dispatchers.IO) {
            try {
                val container = FileZipContainer(ZipFile(file), file)
                Try.success(container)
            } catch (e: ZipException) {
                Try.failure(ArchiveOpener.SniffOpenError.NotRecognized)
            } catch (e: SecurityException) {
                Try.failure(
                    ArchiveOpener.SniffOpenError.Reading(
                        ReadError.Access(FileSystemError.Forbidden(e))
                    )
                )
            } catch (e: IOException) {
                Try.failure(
                    ArchiveOpener.SniffOpenError.Reading(
                        ReadError.Access(FileSystemError.IO(e))
                    )
                )
            }
        }
    }

    suspend fun open(
        format: Format,
        file: File,
    ): Try<Container<Resource>, ArchiveOpener.OpenError> {
        if (!format.conformsTo(Specification.Zip)) {
            return Try.failure(
                ArchiveOpener.OpenError.FormatNotSupported(format)
            )
        }

        return open(file)
    }

    // Internal for testing purpose
    internal suspend fun open(file: File): Try<Container<Resource>, ArchiveOpener.OpenError> =
        withContext(Dispatchers.IO) {
            try {
                val archive = FileZipContainer(ZipFile(file), file)
                Try.success(archive)
            } catch (e: FileNotFoundException) {
                Try.failure(
                    ArchiveOpener.OpenError.Reading(
                        ReadError.Access(FileSystemError.FileNotFound(e))
                    )
                )
            } catch (e: ZipException) {
                Try.failure(
                    ArchiveOpener.OpenError.Reading(
                        ReadError.Decoding(e)
                    )
                )
            } catch (e: SecurityException) {
                Try.failure(
                    ArchiveOpener.OpenError.Reading(
                        ReadError.Access(FileSystemError.Forbidden(e))
                    )
                )
            } catch (e: IOException) {
                Try.failure(
                    ArchiveOpener.OpenError.Reading(
                        ReadError.Access(FileSystemError.IO(e))
                    )
                )
            }
        }
}
