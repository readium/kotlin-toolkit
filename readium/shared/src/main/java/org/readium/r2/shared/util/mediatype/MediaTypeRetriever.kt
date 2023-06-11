package org.readium.r2.shared.util.mediatype

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import java.io.File
import org.readium.r2.shared.BuildConfig
import org.readium.r2.shared.extensions.queryProjection
import org.readium.r2.shared.resource.*
import org.readium.r2.shared.util.Either
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.toUrl

class MediaTypeRetriever(
    resourceFactory: ResourceFactory = FileResourceFactory(),
    containerFactory: ContainerFactory = DirectoryContainerFactory(),
    archiveFactory: ArchiveFactory = DefaultArchiveFactory(),
    sniffers: List<Sniffer> = Sniffers.all,
) {

    private val internalRetriever: MediaTypeRetrieverInternal =
        MediaTypeRetrieverInternal(sniffers)

    private val urlSnifferContextFactory: UrlSnifferContextFactory =
        UrlSnifferContextFactory(resourceFactory, containerFactory, archiveFactory)

    private val bytesSnifferContextFactory: BytesSnifferContextFactory =
        BytesSnifferContextFactory(archiveFactory)

    suspend fun canonicalMediaType(mediaType: MediaType): MediaType =
        of(mediaType = mediaType.toString()) ?: mediaType

    /**
     * Resolves a format from a single file extension and media type hint, without checking the actual
     * content.
     */
    suspend fun of(
        mediaType: String? = null,
        fileExtension: String? = null,
    ): MediaType? {
        if (BuildConfig.DEBUG && mediaType?.startsWith("/") == true) {
            throw IllegalArgumentException("The provided media type is incorrect: $mediaType. To pass a file path, you must wrap it in a File().")
        }
        return of(content = null, mediaTypes = listOfNotNull(mediaType), fileExtensions = listOfNotNull(fileExtension))
    }

    /**
     * Resolves a format from file extension and media type hints, without checking the actual
     * content.
     */
    suspend fun of(
        mediaTypes: List<String>,
        fileExtensions: List<String>,
    ): MediaType? {
        return of(content = null, mediaTypes = mediaTypes, fileExtensions = fileExtensions)
    }

    /**
     * Resolves a format from a local file path.
     */
    suspend fun ofFile(
        file: File,
        mediaType: String? = null,
        fileExtension: String? = null,
    ): MediaType? {
        return ofFile(file, mediaTypes = listOfNotNull(mediaType), fileExtensions = listOfNotNull(fileExtension))
    }

    /**
     * Resolves a format from a local file path.
     */
    suspend fun ofFile(
        file: File,
        mediaTypes: List<String>,
        fileExtensions: List<String>,
    ): MediaType? {
        return of(content = Either.Right(file.toUrl()), mediaTypes = mediaTypes, fileExtensions = listOf(file.extension) + fileExtensions)
    }

    /**
     * Resolves a format from a local file path.
     */
    suspend fun ofFile(
        path: String,
        mediaType: String? = null,
        fileExtension: String? = null,
    ): MediaType? {
        return ofFile(File(path), mediaType = mediaType, fileExtension = fileExtension)
    }

    /**
     * Resolves a format from a local file path.
     */
    suspend fun ofFile(
        path: String,
        mediaTypes: List<String>,
        fileExtensions: List<String>,
    ): MediaType? {
        return ofFile(File(path), mediaTypes = mediaTypes, fileExtensions = fileExtensions)
    }

    /**
     * Resolves a format from bytes, e.g. from an HTTP response.
     */
    suspend fun ofBytes(
        bytes: () -> ByteArray,
        mediaType: String? = null,
        fileExtension: String? = null,
    ): MediaType? {
        return ofBytes(bytes, mediaTypes = listOfNotNull(mediaType), fileExtensions = listOfNotNull(fileExtension))
    }

    /**
     * Resolves a format from bytes, e.g. from an HTTP response.
     */
    suspend fun ofBytes(
        bytes: () -> ByteArray,
        mediaTypes: List<String>,
        fileExtensions: List<String>,
    ): MediaType? {
        return of(content = Either.Left(bytes), mediaTypes = mediaTypes, fileExtensions = fileExtensions)
    }

    /**
     * Resolves a format from a content URI and a [ContentResolver].
     * Accepts the following URI schemes: content, android.resource, file.
     */
    suspend fun ofUri(
        uri: Uri,
        contentResolver: ContentResolver,
        mediaType: String? = null,
        fileExtension: String? = null,
    ): MediaType? {
        return ofUri(uri, contentResolver, mediaTypes = listOfNotNull(mediaType), fileExtensions = listOfNotNull(fileExtension))
    }

    /**
     * Resolves a format from a content URI and a [ContentResolver].
     * Accepts the following URI schemes: content, android.resource, file.
     */
    suspend fun ofUri(
        uri: Uri,
        contentResolver: ContentResolver,
        mediaTypes: List<String>,
        fileExtensions: List<String>,
    ): MediaType? {
        val allMediaTypes = mediaTypes.toMutableList()
        val allFileExtensions = fileExtensions.toMutableList()

        MimeTypeMap.getFileExtensionFromUrl(uri.toString()).ifEmpty { null }?.let {
            allFileExtensions.add(0, it)
        }

        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            contentResolver.getType(uri)
                ?.takeUnless { MediaType.BINARY.matches(it) }
                ?.let { allMediaTypes.add(0, it) }

            contentResolver.queryProjection(uri, MediaStore.MediaColumns.DISPLAY_NAME)?.let { filename ->
                allFileExtensions.add(0, File(filename).extension)
            }
        }

        val url = uri.toUrl() ?: return null
        return of(content = Either.Right(url), mediaTypes = allMediaTypes, fileExtensions = allFileExtensions)
    }

    /**
     * Resolves a media type from a sniffer context.
     *
     * Sniffing a media type is done in two rounds, because we want to give an opportunity to all
     * sniffers to return a [MediaType] quickly before inspecting the content itself:
     *  - Light Sniffing checks only the provided file extension or media type hints.
     *  - Heavy Sniffing reads the bytes to perform more advanced sniffing.
     */
    private suspend fun of(
        content: Either<() -> ByteArray, Url>?,
        mediaTypes: List<String>,
        fileExtensions: List<String>
    ): MediaType? {
        val fullContext = suspend {
            when (content) {
                is Either.Left ->
                    bytesSnifferContextFactory.createContext(
                        content.value.invoke(),
                        mediaTypes,
                        fileExtensions
                    )
                is Either.Right ->
                    urlSnifferContextFactory.createContext(content.value, mediaTypes, fileExtensions)
                null -> null
            }
        }

        return internalRetriever.of(fullContext, mediaTypes, fileExtensions)
    }
}
