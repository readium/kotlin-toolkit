/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */
package org.readium.r2.shared.publication.presentation

import org.readium.r2.shared.publication.Properties

// Presentation extensions for link [Properties]

/**
 * Specifies whether or not the parts of a linked resource that flow out of the viewport are
 * clipped.
 */
val Properties.clipped: Boolean?
    get() = this["clipped"] as? Boolean

/**
 * Suggested method for constraining a resource inside the viewport.
 */
val Properties.fit: Presentation.Fit?
    get() = Presentation.Fit(this["fit"] as? String)

/**
 * Suggested orientation for the device when displaying the linked resource.
 */
val Properties.orientation: Presentation.Orientation?
    get() = Presentation.Orientation(this["orientation"] as? String)

/**
 * Suggested method for handling overflow while displaying the linked resource.
 */
val Properties.overflow: Presentation.Overflow?
    get() = Presentation.Overflow(this["overflow"] as? String)

/**
 * Indicates how the linked resource should be displayed in a reading environment that displays
 * synthetic spreads.
 */
val Properties.page: Presentation.Page?
    get() = Presentation.Page(this["page"] as? String)

/**
 * Indicates the condition to be met for the linked resource to be rendered within a synthetic
 * spread.
 */
val Properties.spread: Presentation.Spread?
    get() = Presentation.Spread(this["spread"] as? String)
