package org.readium.r2.shared.util.mediatype

import java.io.IOException
import java.io.InputStream
import org.json.JSONObject
import org.readium.r2.shared.extensions.read
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.FilesystemError
import org.readium.r2.shared.util.MessageError
import org.readium.r2.shared.util.NetworkError
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.datasource.DataSource
import org.readium.r2.shared.util.datasource.DataSourceInputStream
import org.readium.r2.shared.util.datasource.DecoderError
import org.readium.r2.shared.util.datasource.readAsJson
import org.readium.r2.shared.util.datasource.readAsRwpm
import org.readium.r2.shared.util.datasource.readAsString
import org.readium.r2.shared.util.datasource.readAsXml
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.xml.ElementNode

/**
 * Provides read access to an asset content.
 */
public sealed interface MediaTypeSnifferContent

/**
 * Provides read access to a resource content.
 */
public interface ResourceMediaTypeSnifferContent : MediaTypeSnifferContent {

    public val source: AbsoluteUrl?

    /**
     * Reads all the bytes or the given [range].
     *
     * It can be used to check a file signature, aka magic number.
     * See https://en.wikipedia.org/wiki/List_of_file_signatures
     */
    public suspend fun read(range: LongRange? = null): Try<ByteArray, MediaTypeSnifferContentError>

    public suspend fun length(): Try<Long, MediaTypeSnifferContentError>
}

internal fun ResourceMediaTypeSnifferContent.asDataSource() =
    ResourceMediaTypeSnifferContentDataSource(this)

internal class ResourceMediaTypeSnifferContentDataSource(
    private val resourceMediaTypeSnifferContent: ResourceMediaTypeSnifferContent
) : DataSource<MediaTypeSnifferContentError> {

    override suspend fun length(): Try<Long, MediaTypeSnifferContentError> =
        resourceMediaTypeSnifferContent.length()

    override suspend fun read(range: LongRange?): Try<ByteArray, MediaTypeSnifferContentError> =
        resourceMediaTypeSnifferContent.read(range)

    override suspend fun close() {
        // ResourceMediaTypeSnifferContent doesn't own the resource.
        // Do nothing.
    }
}

/**
 * Content as plain text.
 *
 * It will extract the charset parameter from the media type hints to figure out an encoding.
 * Otherwise, fallback on UTF-8.
 */
internal suspend fun ResourceMediaTypeSnifferContent.contentAsString(): Try<String, DecoderError<MediaTypeSnifferContentError>> =
    asDataSource().readAsString()

/** Content as an XML document. */
internal suspend fun ResourceMediaTypeSnifferContent.contentAsXml(): Try<ElementNode, DecoderError<MediaTypeSnifferContentError>> =
    asDataSource().readAsXml()

/**
 * Content parsed from JSON.
 */
internal suspend fun ResourceMediaTypeSnifferContent.contentAsJson(): Try<JSONObject, DecoderError<MediaTypeSnifferContentError>> =
    asDataSource().readAsJson()

/** Readium Web Publication Manifest parsed from the content. */
internal suspend fun ResourceMediaTypeSnifferContent.contentAsRwpm(): Try<Manifest, DecoderError<MediaTypeSnifferContentError>> =
    asDataSource().readAsRwpm()

public sealed class MediaTypeSnifferContentError(override val message: String) : Error {

    public class NotFound(public override val cause: Error) :
        MediaTypeSnifferContentError("Resource could not be found.")

    public class Forbidden(public override val cause: Error) :
        MediaTypeSnifferContentError("You are not allowed to access this content.")

    public class Network(public override val cause: NetworkError) :
        MediaTypeSnifferContentError("A network error occurred.")

    public class Filesystem(public override val cause: FilesystemError) :
        MediaTypeSnifferContentError("An unexpected error occurred on filesystem.")

    public class TooBig(public override val cause: ThrowableError<OutOfMemoryError>) :
        MediaTypeSnifferContentError("Sniffing was interrupted because resource is too big.")

    public class ArchiveError(public override val cause: Error) :
        MediaTypeSnifferContentError("An error occurred with archive.")

    public class Unknown(public override val cause: Error) :
        MediaTypeSnifferContentError("An unknown error occurred.")
}

/**
 * Raw bytes stream of the content.
 *
 * A byte stream can be useful when sniffers only need to read a few bytes at the beginning of
 * the file.
 */
internal fun ResourceMediaTypeSnifferContent.contentAsStream(): InputStream =
    DataSourceInputStream(asDataSource(), ::MediaTypeSnifferContentException)

internal class MediaTypeSnifferContentException(
    val error: MediaTypeSnifferContentError
) : IOException() {

    companion object {

        fun Exception.unwrapMediaTypeSnifferContentException(): Exception {
            this.findMediaTypeSnifferContentExceptionCause()?.let { return it }
            return this
        }

        private fun Throwable.findMediaTypeSnifferContentExceptionCause(): MediaTypeSnifferContentException? =
            when {
                this is MediaTypeSnifferContentException -> this
                cause != null -> cause!!.findMediaTypeSnifferContentExceptionCause()
                else -> null
            }
    }
}

/**
 * Returns whether the content is a JSON object containing all of the given root keys.
 */
internal suspend fun ResourceMediaTypeSnifferContent.containsJsonKeys(
    vararg keys: String
): Try<Boolean, DecoderError<MediaTypeSnifferContentError>> {
    val json = contentAsJson()
        .getOrElse { return Try.failure(it) }
    return Try.success(json.keys().asSequence().toSet().containsAll(keys.toList()))
}

/**
 * Provides read access to a container's resources.
 */
public interface ContainerMediaTypeSnifferContent : MediaTypeSnifferContent {
    /**
     * Returns all the known entry urls in the container.
     */
    public suspend fun entries(): Set<Url>?

    /**
     * Returns the entry data at the given [url] in this container.
     */
    public suspend fun read(url: Url, range: LongRange? = null): Try<ByteArray, MediaTypeSnifferContentError>

    public suspend fun length(url: Url): Try<Long, MediaTypeSnifferContentError>
}

/**
 * Returns whether an entry exists in the container.
 */
internal suspend fun ContainerMediaTypeSnifferContent.checkContains(url: Url): Try<Unit, MediaTypeSnifferContentError> =
    entries()?.contains(url)
        ?.let {
            if (it) {
                Try.success(Unit)
            } else {
                Try.failure(
                    MediaTypeSnifferContentError.NotFound(
                        MessageError("Container entry list doesn't contain $url.")
                    )
                )
            }
        }
        ?: read(url, range = 0L..1L)
            .map { }

/**
 * A [ResourceMediaTypeSnifferContent] built from a raw byte array.
 */
public class BytesResourceMediaTypeSnifferContent(
    bytes: suspend () -> ByteArray
) : ResourceMediaTypeSnifferContent {

    private val bytesFactory = bytes
    private lateinit var _bytes: ByteArray

    private suspend fun bytes(): ByteArray {
        if (::_bytes.isInitialized) {
            return _bytes
        }
        _bytes = bytesFactory()
        return _bytes
    }

    override val source: AbsoluteUrl? = null

    override suspend fun read(range: LongRange?): Try<ByteArray, MediaTypeSnifferContentError> =
        Try.success(bytes().read(range))

    override suspend fun length(): Try<Long, MediaTypeSnifferContentError> =
        Try.success(bytes().size.toLong())
}
