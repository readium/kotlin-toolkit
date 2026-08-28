package org.readium.r2.testapp.utils

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.file.FileSystemError

/**
 * Copies the content of this [InputStream] to [file].
 */
suspend fun InputStream.toFile(file: File): Try<Unit, FileSystemError> =
    try {
        Try.success(toFileUnsafe(file))
    } catch (e: IOException) {
        Try.failure(FileSystemError.IO(e))
    } catch (e: FileNotFoundException) {
        Try.failure(FileSystemError.IO(e))
    } catch (e: SecurityException) {
        Try.failure(FileSystemError.Forbidden(e))
    }

/**
 * Copies the content of this [InputStream] to [file].
 * [file] will be created if necessary, as well as any non-existing parent directory.
 *
 * Throws IOException, SecurityException, or FileNotFoundException when [file] is not a regular file
 * and cannot be created.
 */
suspend fun InputStream.toFileUnsafe(file: File) {
    checkNotNull(file.parentFile).mkdirs()
    file.createNewFile()
    try {
        withContext(Dispatchers.IO) {
            use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
        }
    } catch (e: Exception) {
        tryOrLog { file.delete() }
        throw e
    }
}

/**
 * Copies the content of this [InputStream] to a new file in [dir].
 */
suspend fun InputStream.copyToNewFile(dir: File): Try<File, FileSystemError> {
    val filename = UUID.randomUUID().toString()
    val file = File(dir, filename)
    return toFile(file).map { file }
}
