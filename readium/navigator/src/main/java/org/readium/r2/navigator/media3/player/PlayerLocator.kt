/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.player

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.Serializable
import org.readium.r2.navigator.extensions.fragmentParameters
import org.readium.r2.navigator.media3.api.MediaNavigatorInternal
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator

@ExperimentalReadiumApi
@Serializable
data class PlayerLocator(
    val index: Int,
    @Serializable(with = DurationSerializer::class)
    val position: Duration
) : MediaNavigatorInternal.Position

internal val Locator.Locations.time: Duration? get() =
    fragmentParameters["t"]?.toIntOrNull()?.seconds
