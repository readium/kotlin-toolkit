/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import java.util.*

class Language private constructor(val code: String, val locale: Locale) {

    constructor(locale: Locale) : this(code = locale.toLanguageTag(), locale = locale)

    companion object {
        operator fun invoke(code: String): Language {
            val fixedCode = code.replace("_", "-")
            return Language(code = fixedCode, locale = Locale.forLanguageTag(code))
        }
    }

    val isRegional: Boolean =
        locale.country.isNotEmpty()

    fun removeRegion(): Language =
        Language(code.split("-", limit = 2).first())

    override fun equals(other: Any?): Boolean =
        code == (other as? Language)?.code

    override fun hashCode(): Int =
        code.hashCode()
}