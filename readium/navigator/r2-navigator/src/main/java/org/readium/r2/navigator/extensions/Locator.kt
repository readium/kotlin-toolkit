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

// FIXME: To add to r2-shared
internal val Locator.Locations.fragmentParameters: Map<String, String> get() =
    fragments
        // Concatenates fragments together, after dropping any #
        .map { it.removePrefix("#") }
        .joinToString(separator = "&")
        // Splits parameters
        .split("&")
        .map { it.split("=") }
        // Only keep named parameters
        .filter { it.size == 2 }
        .associate { Pair(it[0].trim().toLowerCase(Locale.ROOT), it[1].trim()) }

internal val Locator.Locations.page: Int? get() =
    fragmentParameters["page"]?.toIntOrNull()
