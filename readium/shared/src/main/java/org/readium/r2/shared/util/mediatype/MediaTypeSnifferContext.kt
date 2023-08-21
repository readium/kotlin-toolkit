package org.readium.r2.shared.util.mediatype

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.r2.shared.extensions.read
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.shared.parser.xml.XmlParser
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.util.SuspendingCloseable

public interface MediaTypeSnifferContext : SuspendingCloseable {
    /** Format hints. */
    public val hints: MediaTypeHints
}

/** Finds the first [Charset] declared in the media types' `charset` parameter. */
public val MediaTypeSnifferContext.charset: Charset? get() =
    hints.mediaTypes.firstNotNullOfOrNull { it.charset }

/** Returns whether this context has any of the given file extensions, ignoring case. */
public fun MediaTypeSnifferContext.hasFileExtension(vararg fileExtensions: String): Boolean {
    for (fileExtension in fileExtensions.map { it.lowercase() }) {
        if (hints.fileExtensions.contains(fileExtension.lowercase(Locale.ROOT))) {
            return true
        }
    }
    return false
}

/**
 * Returns whether this context has any of the given media type, ignoring case and extra
 * parameters.
 *
 * Implementation note: Use [MediaType] to handle the comparison to avoid edge cases.
 */
public fun MediaTypeSnifferContext.hasMediaType(vararg mediaTypes: String): Boolean {
    @Suppress("NAME_SHADOWING")
    val mediaTypes = mediaTypes.mapNotNull { MediaType(it) }
    for (mediaType in mediaTypes) {
        if (hints.mediaTypes.any { mediaType.contains(it) }) {
            return true
        }
    }
    return false
}

public interface ContentMediaTypeSnifferContext : MediaTypeSnifferContext {

    /**
     * Reads all the bytes or the given [range].
     *
     * It can be used to check a file signature, aka magic number.
     * See https://en.wikipedia.org/wiki/List_of_file_signatures
     */
    public suspend fun read(range: LongRange? = null): ByteArray?

    /**
     * Content as plain text.
     *
     * It will extract the charset parameter from the media type hints to figure out an encoding.
     * Otherwise, fallback on UTF-8.
     */
    public suspend fun contentAsString(): String? =
        read()?.let {
            tryOrNull {
                withContext(Dispatchers.Default) {
                    String(it, charset = charset ?: Charsets.UTF_8)
                }
            }
        }

    /** Content as an XML document. */
    public suspend fun contentAsXml(): ElementNode? =
        read()?.let {
            tryOrNull {
                withContext(Dispatchers.Default) {
                    XmlParser().parse(ByteArrayInputStream(it))
                }
            }
        }

    /**
     * Content parsed from JSON.
     */
    public suspend fun contentAsJson(): JSONObject? =
        contentAsString()?.let {
            tryOrNull {
                withContext(Dispatchers.Default) {
                    JSONObject(it)
                }
            }
        }

    /** Readium Web Publication Manifest parsed from the content. */
    public suspend fun contentAsRwpm(): Manifest? =
        Manifest.fromJSON(contentAsJson())

    /**
     * Raw bytes stream of the content.
     *
     * A byte stream can be useful when sniffers only need to read a few bytes at the beginning of
     * the file.
     */
    public suspend fun contentAsStream(): InputStream =
        ByteArrayInputStream(read() ?: ByteArray(0))
}

/**
 * Returns whether the content is a JSON object containing all of the given root keys.
 */
public suspend fun ContentMediaTypeSnifferContext.containsJsonKeys(vararg keys: String): Boolean {
    val json = contentAsJson() ?: return false
    return json.keys().asSequence().toSet().containsAll(keys.toList())
}

public interface ContainerMediaTypeSnifferContext : MediaTypeSnifferContext {
    /**
     * Returns all the known entry paths in the container.
     */
    public suspend fun entries(): Set<String>?

    /**
     * Returns the entry data at the given [path] in this container.
     */
    public suspend fun read(path: String): ByteArray?
}

/**
 * Returns whether an entry exists in the container.
 */
public suspend fun ContainerMediaTypeSnifferContext.contains(path: String): Boolean =
    entries()?.contains(path)
        ?: (read(path) != null)

public class HintMediaTypeSnifferContext(
    override val hints: MediaTypeHints
) : MediaTypeSnifferContext {
    override suspend fun close() {}
}

public class BytesContentMediaTypeSnifferContext(
    override val hints: MediaTypeHints = MediaTypeHints(),
    bytes: suspend () -> ByteArray
) : ContentMediaTypeSnifferContext {

    private val bytesFactory = bytes
    private lateinit var _bytes: ByteArray

    private suspend fun bytes(): ByteArray {
        if (::_bytes.isInitialized) {
            return _bytes
        }
        _bytes = bytesFactory()
        return _bytes
    }

    override suspend fun read(range: LongRange?): ByteArray =
        bytes().read(range)

    override suspend fun close() {}
}
