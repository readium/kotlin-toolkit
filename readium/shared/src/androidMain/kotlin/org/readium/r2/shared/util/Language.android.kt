/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import java.util.Locale

internal actual fun localeRegionOf(bcp47Tag: String): String? =
    Locale.forLanguageTag(bcp47Tag).country.takeIf { it.isNotEmpty() }

internal actual fun defaultLanguageTag(): String =
    Locale.getDefault().toLanguageTag()

/**
 * Creates a [Language] from a Java [Locale].
 */
public fun Language(locale: Locale): Language =
    Language(code = locale.toLanguageTag())

/**
 * The Java [Locale] for this language.
 */
public val Language.locale: Locale
    get() = Locale.forLanguageTag(code)
