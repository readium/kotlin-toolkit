/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mickaël Menu
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.publication.encryption

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.optNullableLong
import org.readium.r2.shared.extensions.optNullableString
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.logging.log

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
@Parcelize
public data class Encryption(
    val algorithm: String,
    val compression: String? = null,
    val originalLength: Long? = null,
    val profile: String? = null,
    val scheme: String? = null,
) : JSONable, Parcelable {

    /**
     * Serializes an [Encryption] to its RWPM JSON representation.
     */
    override fun toJSON(): JSONObject = JSONObject().apply {
        put("algorithm", algorithm)
        put("compression", compression)
        put("originalLength", originalLength)
        put("profile", profile)
        put("scheme", scheme)
    }

    public companion object {

        /**
         * Creates an [Encryption] from its RWPM JSON representation.
         * If the encryption can't be parsed, a warning will be logged with [warnings].
         */
        public fun fromJSON(json: JSONObject?, warnings: WarningLogger? = null): Encryption? {
            val algorithm = json?.optNullableString("algorithm")
            if (algorithm.isNullOrEmpty()) {
                warnings?.log(Encryption::class.java, "[algorithm] is required", json)
                return null
            }

            return Encryption(
                algorithm = algorithm,
                compression = json.optNullableString("compression"),
                // Fallback on [original-length] for legacy reasons
                // See https://github.com/readium/webpub-manifest/pull/43
                originalLength = json.optNullableLong("originalLength")
                    ?: json.optNullableLong("original-length"),
                profile = json.optNullableString("profile"),
                scheme = json.optNullableString("scheme")
            )
        }
    }
}
