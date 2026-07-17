/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:Suppress("DEPRECATION")

// TODO(kmp): move to commonMain — blocked by: publication/Metadata.kt (phase 07)

package org.readium.r2.shared.publication.presentation

import kotlinx.serialization.json.JsonObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.util.json.toJsonObject

// Presentation extensions for [Metadata]

@Deprecated("This was removed from RWPM. You can still use the EPUB extensibility to access the original values.", level = DeprecationLevel.ERROR)
@OptIn(InternalReadiumApi::class)
public val Metadata.presentation: Presentation
    get() = Presentation.fromJSON(
        (this["presentation"] as? Map<*, *>)
            ?.toJsonObject()
    )
