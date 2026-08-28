/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:Suppress("DEPRECATION")

package org.readium.r2.shared.publication.presentation

import org.readium.r2.shared.publication.Properties

// Presentation extensions for link [Properties]

/**
 * Specifies whether or not the parts of a linked resource that flow out of the viewport are
 * clipped.
 */
@Deprecated("This was removed from RWPM.")
public val Properties.clipped: Boolean?
    get() = this["clipped"] as? Boolean

/**
 * Suggested method for constraining a resource inside the viewport.
 */
@Deprecated("This was removed from RWPM.")
public val Properties.fit: Presentation.Fit?
    get() = Presentation.Fit(this["fit"] as? String)

/**
 * Suggested orientation for the device when displaying the linked resource.
 */
@Deprecated("This was removed from RWPM. You can still use the EPUB extensibility to access the original value.")
public val Properties.orientation: Presentation.Orientation?
    get() = Presentation.Orientation(this["orientation"] as? String)

/**
 * Suggested method for handling overflow while displaying the linked resource.
 */
@Deprecated("This was removed from RWPM. You can still use the EPUB extensibility to access the original value.")
public val Properties.overflow: Presentation.Overflow?
    get() = Presentation.Overflow(this["overflow"] as? String)

/**
 * Indicates the condition to be met for the linked resource to be rendered within a synthetic
 * spread.
 */
@Deprecated("This was removed from RWPM. You can still use the EPUB extensibility to access the original value.")
public val Properties.spread: Presentation.Spread?
    get() = Presentation.Spread(this["spread"] as? String)
