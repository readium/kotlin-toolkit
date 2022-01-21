/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.media2

import org.readium.r2.shared.publication.Locator
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

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
        .associate { it[0].trim().lowercase(Locale.ROOT) to it[1].trim() }

/**
 * Media fragment, used for example in audiobooks.
 *
 * https://www.w3.org/TR/media-frags/
 */
@OptIn(ExperimentalTime::class)
internal val Locator.Locations.time: Duration? get() =
    fragmentParameters["t"]?.toIntOrNull()?.seconds
