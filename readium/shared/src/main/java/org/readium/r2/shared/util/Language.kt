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
import java.util.*

/**
 * Represents a language with its region.
 *
 * @param code BCP-47 language code
 */
@Serializable(with = Language.Serializer::class)
data class Language(val code: String) {

    /**
     * Creates a [Language] from a Java [Locale].
     */
    constructor(locale: Locale) : this(code = locale.toLanguageTag())

    val normalizedCode by lazy { code.replace("_", "-") }

    val locale: Locale by lazy { Locale.forLanguageTag(normalizedCode) }

    /** Indicates whether this language is a regional variant. */
    val isRegional: Boolean by lazy {
        locale.country.isNotEmpty()
    }

    /** Returns this [Language] after stripping the region. */
    fun removeRegion(): Language =
        Language(normalizedCode.split("-", limit = 2).first())

    object Serializer : KSerializer<Language> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Language", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Language) {
            encoder.encodeString(value.code)
        }

        override fun deserialize(decoder: Decoder): Language =
            Language(decoder.decodeString())
    }
}