/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Represents a language with its region.
 *
 * @param code BCP-47 language code
 */
@Serializable(with = Language.Serializer::class)
public class Language(code: String) {

    /**
     * BCP-47 language code.
     */
    public val code: String = code.replace("_", "-")

    /** Indicates whether this language is a regional variant. */
    public val isRegional: Boolean by lazy {
        // `this.code` on purpose: a bare `code` would resolve to the raw constructor parameter,
        // bypassing the underscore normalization.
        !localeRegionOf(this.code).isNullOrEmpty()
    }

    /** Returns this [Language] after stripping the region. */
    public fun removeRegion(): Language =
        Language(code.split("-", limit = 2).first())

    override fun toString(): String =
        "Language($code)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Language) return false
        if (code != other.code) return false
        return true
    }

    override fun hashCode(): Int =
        code.hashCode()

    internal object Serializer : KSerializer<Language> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
            "Language",
            PrimitiveKind.STRING
        )

        override fun serialize(encoder: Encoder, value: Language) {
            encoder.encodeString(value.code)
        }

        override fun deserialize(decoder: Decoder): Language =
            Language(decoder.decodeString())
    }
}

/**
 * Returns the region subtag (e.g. `US` in `en-US`) of the given BCP-47 language tag, as determined
 * by the platform locale APIs, or null if there is none.
 */
internal expect fun localeRegionOf(bcp47Tag: String): String?

/**
 * Returns the BCP-47 language tag of the current platform locale (e.g. `en-US`).
 */
internal expect fun defaultLanguageTag(): String
