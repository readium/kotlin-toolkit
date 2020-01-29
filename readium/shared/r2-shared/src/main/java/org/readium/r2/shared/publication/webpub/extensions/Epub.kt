/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.webpub.extensions

import org.json.JSONObject
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.Warning
import org.readium.r2.shared.WarningLogger
import org.readium.r2.shared.extensions.optNullableInt
import org.readium.r2.shared.extensions.optNullableString
import org.readium.r2.shared.publication.webpub.WebPublication
import org.readium.r2.shared.publication.webpub.firstWithRole
import org.readium.r2.shared.publication.webpub.link.Link
import org.readium.r2.shared.publication.webpub.link.Properties
import org.readium.r2.shared.publication.webpub.metadata.Metadata
import java.io.Serializable

/**
 * Hints how the layout of the resource should be presented.
 * https://readium.org/webpub-manifest/schema/extensions/epub/metadata.schema.json
 */
enum class EpubLayout(val value: String) {
    FIXED("fixed"),
    REFLOWABLE("reflowable");

    companion object {

        fun from(value: String?) = EpubLayout.values().firstOrNull { it.value == value }

        /**
         * Resolves from an EPUB rendition:layout property.
         */
        fun fromEpub(value: String, fallback: EpubLayout = REFLOWABLE): EpubLayout {
            return when(value) {
                "reflowable" -> REFLOWABLE
                "pre-paginated" -> FIXED
                else -> fallback
            }
        }

    }

}

/**
 * Indicates that a resource is encrypted/obfuscated and provides relevant information for
 * decryption.
 *
 * @param algorithm Identifies the algorithm used to encrypt the resource (URI).
 * @param compression Compression method used on the resource.
 * @param originalLength Original length of the resource in bytes before compression and/or
 *     encryption.
 * @param profile Identifies the encryption profile used to encrypt the resource (URI).
 * @param scheme Identifies the encryption scheme used to encrypt the resource (URI).
 */
data class EpubEncryption(
    val algorithm: String,
    val compression: String? = null,
    val originalLength: Int? = null,
    val profile: String? = null,
    val scheme: String? = null
) : JSONable, Serializable {

    /**
     * Serializes an [EpubEncryption] to its RWPM JSON representation.
     */
    override fun toJSON() = JSONObject().apply {
        put("algorithm", algorithm)
        put("compression", compression)
        put("originalLength", originalLength)
        put("profile", profile)
        put("scheme", scheme)
    }

    companion object {

        /**
         * Creates an [EpubEncryption] from its RWPM JSON representation.
         * If the encryption can't be parsed, a warning will be logged with [warnings].
         */
        fun fromJSON(json: JSONObject?, warnings: WarningLogger? = null): EpubEncryption? {
            val algorithm = json?.optNullableString("algorithm")
            if (algorithm.isNullOrEmpty()) {
                warnings?.log(Warning.JsonParsing(EpubEncryption::class.java, "[algorithm] is required", json))
                return null
            }

            return EpubEncryption(
                algorithm = algorithm,
                compression = json.optNullableString("compression"),
                // Fallback on [original-length] for legacy reasons
                // See https://github.com/readium/webpub-manifest/pull/43
                originalLength = json.optNullableInt("originalLength")
                    ?: json.optNullableInt("original-length"),
                profile = json.optNullableString("profile"),
                scheme = json.optNullableString("scheme")
            )
        }

    }
}


// EPUB extensions for [WebPublication].
// https://readium.org/webpub-manifest/schema/extensions/epub/subcollections.schema.json
// https://idpf.github.io/epub-vocabs/structure/#navigation

/**
 * Provides navigation to positions in the Publication content that correspond to the locations of
 * page boundaries present in a print source being represented by this EPUB Publication.
 */
val WebPublication.pageList: List<Link> get() = linksWithRole("page-list")

/**
 * Identifies fundamental structural components of the publication in order to enable Reading
 * Systems to provide the User efficient access to them.
 */
val WebPublication.landmarks: List<Link> get() = linksWithRole("landmarks")

val WebPublication.listOfAudioClips: List<Link> get() = linksWithRole("loa")
val WebPublication.listOfIllustrations: List<Link> get() = linksWithRole("loi")
val WebPublication.listOfTables: List<Link> get() = linksWithRole("lot")
val WebPublication.listOfVideoClips: List<Link> get() = linksWithRole("lov")


// EPUB extensions for [Metadata].
// https://readium.org/webpub-manifest/schema/extensions/epub/metadata.schema.json

/**
 * Hints how the layout of the resource should be presented.
 */
val Metadata.layout: EpubLayout?
    get() = EpubLayout.from(this["layout"] as? String)


// EPUB extensions for link [Properties].
// https://readium.org/webpub-manifest/schema/extensions/epub/properties.schema.json

/**
 * Identifies content contained in the linked resource, that cannot be strictly identified using a
 * media type.
 */
val Properties.contains: Set<String>
    get() = (this["contains"] as? List<*>)
        ?.filterIsInstance(String::class.java)
        ?.toSet()
        ?: emptySet()

/**
 * Hints how the layout of the resource should be presented.
 */
val Properties.layout: EpubLayout?
    get() = EpubLayout.from(this["layout"] as? String)

/**
 * Location of a media-overlay for the resource referenced in the Link Object.
 */
val Properties.mediaOverlay: String?
    get() = this["mediaOverlay"] as? String

/**
 * Indicates that a resource is encrypted/obfuscated and provides relevant information for
 * decryption.
 */
val Properties.encryption: EpubEncryption?
    get() = (this["encrypted"] as? Map<*, *>)
        ?.let { EpubEncryption.fromJSON(JSONObject(it)) }
