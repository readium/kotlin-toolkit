package org.readium.r2.shared.util.http

import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.extensions.read
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.ResourceTry
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.io.CountingInputStream
import org.readium.r2.shared.util.mediatype.MediaType

/** Provides access to an external URL. */
@OptIn(ExperimentalReadiumApi::class)
public class HttpResource(
    private val client: HttpClient,
    override val source: Url,
    private val maxSkipBytes: Long = MAX_SKIP_BYTES
) : Resource {

    override suspend fun mediaType(): ResourceTry<MediaType> =
        headResponse().map { it.mediaType }

    override suspend fun properties(): ResourceTry<Resource.Properties> =
        ResourceTry.success(Resource.Properties())

    override suspend fun length(): ResourceTry<Long> =
        headResponse().flatMap {
            val contentLength = it.contentLength
            return if (contentLength != null) {
                Try.success(contentLength)
            } else {
                Try.failure(Resource.Exception.Unavailable())
            }
        }

    override suspend fun close() {}

    override suspend fun read(range: LongRange?): ResourceTry<ByteArray> = withContext(
        Dispatchers.IO
    ) {
        try {
            stream(range?.first.takeUnless { it == 0L }).map { stream ->
                if (range != null) {
                    stream.read(range.count().toLong())
                } else {
                    stream.readBytes()
                }
            }
        } catch (e: HttpException) {
            Try.failure(Resource.Exception.wrapHttp(e))
        } catch (e: Exception) {
            Try.failure(Resource.Exception.wrap(e))
        }
    }

    /** Cached HEAD response to get the expected content length and other metadata. */
    private lateinit var _headResponse: ResourceTry<HttpResponse>

    private suspend fun headResponse(): ResourceTry<HttpResponse> {
        if (::_headResponse.isInitialized) {
            return _headResponse
        }

        _headResponse = client.head(HttpRequest(source.toString()))
            .mapFailure { Resource.Exception.wrapHttp(it) }

        return _headResponse
    }

    /**
     * Returns an HTTP stream for the resource, starting at the [from] byte offset.
     *
     * The stream is cached and reused for next calls, if the next [from] offset is not too far
     * and in a forward direction.
     */
    private suspend fun stream(from: Long? = null): ResourceTry<InputStream> {
        val stream = inputStream
        if (from != null && stream != null) {
            tryOrLog {
                val bytesToSkip = from - (inputStreamStart + stream.count)
                if (bytesToSkip in 0 until maxSkipBytes) {
                    stream.skip(bytesToSkip)
                    return Try.success(stream)
                }
            }
        }
        tryOrLog { inputStream?.close() }

        val request = HttpRequest(source.toString()) {
            from?.let { setRange(from..-1) }
        }

        return client.stream(request)
            .flatMap { response ->
                if (from != null && response.response.statusCode != 206
                ) {
                    val exception = Exception("Server seems not to support range requests.")
                    Try.failure(HttpException.wrap(exception))
                } else {
                    Try.success(response)
                }
            }
            .map { CountingInputStream(it.body) }
            .mapFailure { Resource.Exception.wrapHttp(it) }
            .onSuccess {
                inputStream = it
                inputStreamStart = from ?: 0
            }
    }

    private var inputStream: CountingInputStream? = null
    private var inputStreamStart = 0L

    private fun Resource.Exception.Companion.wrapHttp(e: HttpException): Resource.Exception =
        when (e.kind) {
            HttpException.Kind.MalformedRequest, HttpException.Kind.BadRequest, HttpException.Kind.MethodNotAllowed ->
                Resource.Exception.BadRequest(cause = e)
            HttpException.Kind.Timeout, HttpException.Kind.Offline ->
                Resource.Exception.Unavailable(e)
            HttpException.Kind.Unauthorized, HttpException.Kind.Forbidden ->
                Resource.Exception.Forbidden(e)
            HttpException.Kind.NotFound ->
                Resource.Exception.NotFound(e)
            HttpException.Kind.Cancelled ->
                Resource.Exception.Unavailable(e)
            HttpException.Kind.MalformedResponse, HttpException.Kind.ClientError, HttpException.Kind.ServerError, HttpException.Kind.Other ->
                Resource.Exception.Other(e)
        }

    public companion object {

        private const val MAX_SKIP_BYTES: Long = 8192
    }
}
