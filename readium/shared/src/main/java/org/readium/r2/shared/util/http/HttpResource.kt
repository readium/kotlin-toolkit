package org.readium.r2.shared.util.http

import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.extensions.read
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.MessageError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.io.CountingInputStream
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.ResourceError
import org.readium.r2.shared.util.resource.ResourceTry

/** Provides access to an external URL. */
@OptIn(ExperimentalReadiumApi::class)
public class HttpResource(
    private val client: HttpClient,
    override val source: AbsoluteUrl,
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
                Try.failure(ResourceError.Unavailable())
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
        } catch (e: Exception) {
            Try.failure(ResourceError.Other(e))
        }
    }

    /** Cached HEAD response to get the expected content length and other metadata. */
    private lateinit var _headResponse: ResourceTry<HttpResponse>

    private suspend fun headResponse(): ResourceTry<HttpResponse> {
        if (::_headResponse.isInitialized) {
            return _headResponse
        }

        _headResponse = client.head(HttpRequest(source.toString()))
            .mapFailure { ResourceError.wrapHttp(it) }

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
                    val error = MessageError("Server seems not to support range requests.")
                    Try.failure(HttpError(HttpError.Kind.Other, cause = error))
                } else {
                    Try.success(response)
                }
            }
            .map { CountingInputStream(it.body) }
            .mapFailure { ResourceError.wrapHttp(it) }
            .onSuccess {
                inputStream = it
                inputStreamStart = from ?: 0
            }
    }

    private var inputStream: CountingInputStream? = null
    private var inputStreamStart = 0L

    private fun ResourceError.Companion.wrapHttp(e: HttpError): ResourceError =
        when (e.kind) {
            HttpError.Kind.MalformedRequest, HttpError.Kind.BadRequest, HttpError.Kind.MethodNotAllowed ->
                ResourceError.BadRequest(cause = e)
            HttpError.Kind.Timeout, HttpError.Kind.Offline, HttpError.Kind.TooManyRedirects ->
                ResourceError.Unavailable(e)
            HttpError.Kind.Unauthorized, HttpError.Kind.Forbidden ->
                ResourceError.Forbidden(e)
            HttpError.Kind.NotFound ->
                ResourceError.NotFound(e)
            HttpError.Kind.Cancelled ->
                ResourceError.Unavailable(e)
            HttpError.Kind.MalformedResponse, HttpError.Kind.ClientError, HttpError.Kind.ServerError, HttpError.Kind.Other ->
                ResourceError.Other(e)
        }

    public companion object {

        private const val MAX_SKIP_BYTES: Long = 8192
    }
}
