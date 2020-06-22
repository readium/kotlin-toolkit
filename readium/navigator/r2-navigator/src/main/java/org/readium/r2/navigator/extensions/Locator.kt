/*
 * Module: r2-navigator-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.extensions

import org.readium.r2.shared.publication.Locator
import java.util.*

// FIXME: This should be in r2-shared once this public API is specified.

// Reference: https://www.w3.org/TR/fragid-best-practices

/**
 * All named parameters found in the fragments, such as `p=5`.
 */
internal val Locator.Locations.fragmentParameters: Map<String, String> get() =
    fragments
        // Concatenates fragments together, after dropping any #
        .map { it.removePrefix("#") }
        .joinToString(separator = "&")
        // Splits parameters
        .split("&")
        .filter { !it.startsWith("=") }
        .map { it.split("=") }
        // Only keep named parameters
        .filter { it.size == 2 }
        .associate { it[0].trim().toLowerCase(Locale.ROOT) to it[1].trim() }

/**
 * HTML ID fragment identifier.
 */
internal val Locator.Locations.htmlId: String? get() {
    // The HTML 5 specification (used for WebPub) allows any character in an HTML ID, except
    // spaces. This is an issue to differentiate with named parameters, so we ignore any
    // ID containing `=`.
    val id = fragments.firstOrNull { !it.isBlank() && !it.contains("=") }
        ?: fragmentParameters["id"]
        ?: fragmentParameters["name"]

    return id?.removePrefix("#")
}

/**
 * Page fragment identifier, used for example in PDF.
 */
internal val Locator.Locations.page: Int? get() =
    fragmentParameters["page"]?.toIntOrNull()