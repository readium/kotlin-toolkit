package org.readium.r2.shared.util.resource

import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.mediatype.MediaTypeHints

private const val FILENAME_KEY = "filename"

private const val MEDIA_TYPE_KEY = "mediaType"

public val Resource.Properties.filename: String?
    get() = this[FILENAME_KEY] as? String

public var Resource.Properties.Builder.filename: String?
    get() = this[FILENAME_KEY] as? String?
    set(value) {
        if (value == null) {
            remove(FILENAME_KEY)
        } else {
            put(FILENAME_KEY, value)
        }
    }

public val Resource.Properties.mediaType: MediaType?
    get() = (this[MEDIA_TYPE_KEY] as? String?)
        ?.let { MediaType(it) }

public var Resource.Properties.Builder.mediaType: MediaType?
    get() = (this[MEDIA_TYPE_KEY] as? String?)
        ?.let { MediaType(it) }
    set(value) {
        if (value == null) {
            remove(MEDIA_TYPE_KEY)
        } else {
            put(FILENAME_KEY, value.toString())
        }
    }

public operator fun MediaTypeHints.Companion.invoke(properties: Resource.Properties): MediaTypeHints =
    MediaTypeHints(mediaType = properties.mediaType)
