/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.zip

import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.extensions.readSafe
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.getOrThrow
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.http.HttpException
import org.readium.r2.shared.util.http.HttpRequest
import org.readium.r2.shared.util.http.HttpResponse
import org.readium.r2.shared.util.http.head
import org.readium.r2.shared.util.io.CountingInputStream
import org.readium.r2.shared.util.zip.jvm.NonWritableChannelException
import org.readium.r2.shared.util.zip.jvm.SeekableByteChannel
import timber.log.Timber

@OptIn(ExperimentalReadiumApi::class)
internal class HttpChannel(
    private val url: String,
    private val client: HttpClient,
    private val maxSkipBytes: Long = 8192
) : SeekableByteChannel {

    private var position: Long = 0

    private val lock = Any()

    private var inputStream: CountingInputStream? = null

    private var inputStreamStart = 0L

    /** Cached HEAD response to get the expected content length and other metadata. */
    private lateinit var _headResponse: Try<HttpResponse, HttpException>

    private suspend fun headResponse(): Try<HttpResponse, HttpException> {
        if (::_headResponse.isInitialized) {
            return _headResponse
        }

        _headResponse = client.head(HttpRequest(url))
        return _headResponse
    }

    /**
     * Returns an HTTP stream for the resource, starting at the [from] byte offset.
     *
     * The stream is cached and reused for next calls, if the next [from] offset is in a forward
     * direction.
     */
    private suspend fun stream(from: Long? = null): Try<InputStream, HttpException> {
        Timber.d("getStream")
        val stream = inputStream
        if (from != null && stream != null) {
            tryOrLog {
                val bytesToSkip = from - (inputStreamStart + stream.count)
                if (bytesToSkip in 0 until maxSkipBytes) {
                    stream.skip(bytesToSkip)
                    Timber.d("reusing stream")
                    return Try.success(stream)
                }
            }
        }

        tryOrLog { inputStream?.close() }

        val request = HttpRequest(url) {
            from?.takeUnless { it == 0L }
                ?.let { setRange(from..-1) }
        }

        Timber.d("request ${request.headers}")

        return client.stream(request)
            .map {
                Timber.d("responseCode ${it.response.statusCode}")
                Timber.d("response ${it.response}")
                Timber.d("responseHeaders ${it.response.headers}")
                CountingInputStream(it.body)
            }
            .onSuccess {
                inputStream = it
                inputStreamStart = from ?: 0
            }
    }

    override fun close() {}

    override fun isOpen(): Boolean {
        return true
    }

    override fun read(dst: ByteBuffer): Int {
        synchronized(lock) {
            return runBlocking {
                withContext(Dispatchers.IO) {
                    val size = headResponse()
                        .map { it.contentLength }
                        .getOrThrow()
                        ?: throw IOException("Server didn't provide content length.")

                    if (position >= size) {
                        return@withContext -1
                    }

                    Timber.d("position $position")
                    val available = size - position
                    val buffer = ByteArray(dst.remaining().coerceAtMost(available.toInt()))
                    Timber.d("bufferSize ${buffer.size}")
                    val read = stream(position)
                        .getOrThrow()
                        .readSafe(buffer)
                    Timber.d("read $read")
                    if (read != -1) {
                        dst.put(buffer, 0, read)
                        position += read
                    }
                    return@withContext read
                }
            }
        }
    }

    override fun write(src: ByteBuffer): Int {
        throw NonWritableChannelException()
    }

    override fun position(): Long {
        synchronized(lock) {
            return position
        }
    }

    override fun position(newPosition: Long): HttpChannel {
        synchronized(lock) {
            if (newPosition < 0) {
                throw IllegalArgumentException("Requested position is negative.")
            }

            position = newPosition
            return this
        }
    }

    override fun size(): Long {
        return synchronized(lock) {
            runBlocking { headResponse() }
                .getOrThrow()
                .contentLength
                ?: throw IOException("Unknown file length.")
        }
    }

    override fun truncate(size: Long): HttpChannel {
        throw NonWritableChannelException()
    }
}
