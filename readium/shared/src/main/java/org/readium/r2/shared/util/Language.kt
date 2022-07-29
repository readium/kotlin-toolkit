/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import java.util.*

/**
 * Represents a language with its region.
 *
 * @param code BCP-47 language code
 * @param locale Java [Locale]
 */
class Language private constructor(val code: String, val locale: Locale) {

    /**
     * Creates a [Language] from a Java [Locale].
     */
    constructor(locale: Locale) : this(code = locale.toLanguageTag(), locale = locale)

    companion object {
        /**
         * Creates a [Language] from a BCP-47 language code.
         */
        operator fun invoke(code: String): Language {
            val fixedCode = code.replace("_", "-")
            return Language(code = fixedCode, locale = Locale.forLanguageTag(code))
        }
    }

    /** Indicates whether this language is a regional variant. */
    val isRegional: Boolean =
        locale.country.isNotEmpty()

    /** Returns this [Language] after stripping the region. */
    fun removeRegion(): Language =
        Language(code.split("-", limit = 2).first())

    override fun equals(other: Any?): Boolean =
        code == (other as? Language)?.code

    override fun hashCode(): Int =
        code.hashCode()
}