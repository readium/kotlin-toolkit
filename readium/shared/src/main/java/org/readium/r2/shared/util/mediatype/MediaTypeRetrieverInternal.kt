package org.readium.r2.shared.util.mediatype

internal class MediaTypeRetrieverInternal(
    private val sniffers: List<Sniffer>,
) {

    suspend fun of(
        mediaTypes: List<String>,
        fileExtensions: List<String>
    ): MediaType? {
        return of(null, mediaTypes, fileExtensions)
    }

    /**
     * Resolves a media type from a sniffer context.
     *
     * Sniffing a media type is done in two rounds, because we want to give an opportunity to all
     * sniffers to return a [MediaType] quickly before inspecting the content itself:
     *  - Light Sniffing checks only the provided file extension or media type hints.
     *  - Heavy Sniffing reads the bytes to perform more advanced sniffing.
     */
    internal suspend fun of(
        fullContext: (suspend () -> SnifferContext?)?,
        mediaTypes: List<String>,
        fileExtensions: List<String>
    ): MediaType? {
        // Light sniffing with only media type hints
        if (mediaTypes.isNotEmpty()) {
            val context = HintSnifferContext(mediaTypes = mediaTypes)
            for (sniffer in sniffers) {
                val mediaType = sniffer(context)
                if (mediaType != null) {
                    return mediaType
                }
            }
        }

        // Light sniffing with both media type hints and file extensions
        if (fileExtensions.isNotEmpty()) {
            val context = HintSnifferContext(mediaTypes = mediaTypes, fileExtensions = fileExtensions)
            for (sniffer in sniffers) {
                val mediaType = sniffer(context)
                if (mediaType != null) {
                    return mediaType
                }
            }
        }

        // Heavy sniffing
        val context = fullContext?.invoke()

        if (context != null) {
            for (sniffer in sniffers) {
                val mediaType = sniffer(context)
                if (mediaType != null) {
                    return mediaType
                }
            }
        }

        // Falls back on the system-wide registered media types using [MimeTypeMap].
        // Note: This is done after the heavy sniffing of the provided [sniffers], because
        // otherwise it will detect JSON, XML or ZIP formats before we have a chance of sniffing
        // their content (for example, for RWPM).
        val systemContext = context ?: HintSnifferContext(mediaTypes, fileExtensions)
        Sniffers.system(systemContext)?.let { return it }

        // If nothing else worked, we try to parse the first valid media type hint.
        for (mediaType in mediaTypes) {
            MediaType.parse(mediaType)?.let { return it }
        }

        return null
    }
}
