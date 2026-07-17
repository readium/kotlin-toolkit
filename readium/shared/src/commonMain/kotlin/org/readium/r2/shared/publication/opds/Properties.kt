/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.publication.opds

import kotlinx.serialization.json.JsonObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.opds.Acquisition
import org.readium.r2.shared.opds.Availability
import org.readium.r2.shared.opds.Copies
import org.readium.r2.shared.opds.Holds
import org.readium.r2.shared.opds.Price
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Properties
import org.readium.r2.shared.util.json.toJsonObject

// OPDS extensions for link [Properties].
// https://specs.opds.io/schema/properties.schema.json

/**
 * Provides a hint about the expected number of items returned.
 */
public val Properties.numberOfItems: Int?
    get() = (this["numberOfItems"] as? Int)
        ?.takeIf { it >= 0 }

/**
 * The price of a publication is tied to its acquisition link.
 */
public val Properties.price: Price?
    get() = (this["price"] as? Map<*, *>)
        ?.toJsonObject()?.let { Price.fromJSON(it) }

/**
 * Indirect acquisition provides a hint for the expected media type that will be acquired after
 * additional steps.
 */
public val Properties.indirectAcquisitions: List<Acquisition>
    get() = (this["indirectAcquisition"] as? List<*>)
        ?.mapNotNull {
            if (it !is Map<*, *>) {
                null
            } else {
                it.toJsonObject()?.let { json -> Acquisition.fromJSON(json) }
            }
        }
        ?: emptyList()

/**
 * Library-specific features when a specific book is unavailable but provides a hold list.
 */
public val Properties.holds: Holds?
    get() = (this["holds"] as? Map<*, *>)
        ?.toJsonObject()?.let { Holds.fromJSON(it) }

/**
 * Library-specific feature that contains information about the copies that a library has acquired.
 */
public val Properties.copies: Copies?
    get() = (this["copies"] as? Map<*, *>)
        ?.toJsonObject()?.let { Copies.fromJSON(it) }

/**
 * Indicated the availability of a given resource.
 */
public val Properties.availability: Availability?
    get() = (this["availability"] as? Map<*, *>)
        ?.toJsonObject()?.let { Availability.fromJSON(it) }

/**
 * Indicates that the linked resource supports authentication with the associated Authentication
 * Document.
 *
 * See https://drafts.opds.io/authentication-for-opds-1.0.html
 */
public val Properties.authenticate: Link?
    get() = (this["authenticate"] as? Map<*, *>)
        ?.toJsonObject()?.let { Link.fromJSON(it) }
