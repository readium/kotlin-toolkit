package org.readium.r2.shared.util.file

import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.extensions.coerceFirstNonNegative
import org.readium.r2.shared.extensions.read
import org.readium.r2.shared.extensions.readFully
import org.readium.r2.shared.extensions.requireLengthFitInt
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.resource.Resource

/**
 * A [Resource] to access content [path] thanks to an [AssetManager].
 */
@ExperimentalReadiumApi
public class AndroidAssetResource(
    private val path: String,
    private val assetManager: AssetManager
) : Resource {

    private lateinit var _length: Try<Long, ReadError>

    override val sourceUrl: AbsoluteUrl? = null

    override suspend fun close() {
    }

    override suspend fun properties(): Try<Resource.Properties, ReadError> =
        Try.success(Resource.Properties())

    override suspend fun read(range: LongRange?): Try<ByteArray, ReadError> {
        if (range == null) {
            return readFully()
        }

        @Suppress("NAME_SHADOWING")
        val range = range
            .coerceFirstNonNegative()
            .requireLengthFitInt()

        if (range.isEmpty()) {
            return Try.success(ByteArray(0))
        }

        return readRange(range)
    }

    private suspend fun readFully(): Try<ByteArray, ReadError> =
        withStream { it.readFully() }

    private suspend fun readRange(range: LongRange): Try<ByteArray, ReadError> =
        withStream {
            withContext(Dispatchers.IO) {
                var skipped: Long = 0

                while (skipped != range.first) {
                    skipped += it.skip(range.first - skipped)
                    if (skipped == 0L) {
                        throw IOException("Could not skip InputStream to read ranges from $path.")
                    }
                }

                val length = range.last - range.first + 1
                it.read(length)
            }
        }

    override suspend fun length(): Try<Long, ReadError> {
        if (!::_length.isInitialized) {
            _length = tryOrNull {
                assetManager.openFd(path).declaredLength
                    .takeUnless { it == AssetFileDescriptor.UNKNOWN_LENGTH }
                    ?.let { Try.success(it) }
            } ?: readFully().map { it.size.toLong() }
        }

        return _length
    }

    private suspend fun <T> withStream(block: suspend (InputStream) -> T): Try<T, ReadError> {
        return Try.catching {
            assetManager.open(path).use { block(it) }
        }
    }

    private inline fun <T> Try.Companion.catching(closure: () -> T): Try<T, ReadError> =
        try {
            success(closure())
        } catch (e: FileNotFoundException) {
            failure(ReadError.Access(FileSystemError.FileNotFound(e)))
        } catch (e: IOException) {
            failure(ReadError.Access(FileSystemError.IO(e)))
        } catch (e: SecurityException) {
            failure(ReadError.Access(FileSystemError.Forbidden(e)))
        } catch (e: OutOfMemoryError) { // We don't want to catch any Error, only OOM.
            failure(ReadError.OutOfMemory(e))
        }

    override fun toString(): String =
        "${javaClass.simpleName}(${runBlocking { length() } } bytes)"
}
