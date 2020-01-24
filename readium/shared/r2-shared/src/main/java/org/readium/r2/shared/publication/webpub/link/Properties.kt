/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.webpub.link

import org.json.JSONObject
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.toMutableMap
import java.io.Serializable

/**
 * Link Properties
 * https://readium.org/webpub-manifest/schema/properties.schema.json
 *
 * @property orientation Suggested orientation for the device when displaying the linked resource.
 * @property page Indicates how the linked resource should be displayed in a reading environment
 *     that displays synthetic spreads.
 * @property otherProperties Additional properties for extensions.
 */
data class Properties(
    var orientation: Orientation? = null,
    var page: Page? = null,
    var otherProperties: Map<String, Any> = mapOf()

//    /// Identifies content contained in the linked resource, that cannot be
//    /// strictly identified using a media type.
//    var contains: MutableList<String> = mutableListOf(),
//    /// Location of a media-overlay for the resource referenced in the Link Object.
//    private var mediaOverlay: String? = null,
//    /// Indicates that a resource is encrypted/obfuscated and provides relevant
//    /// information for decryption.
//    var encryption: Encryption? = null,
//    /// Hint about the nature of the layout for the linked resources.
//    var layout: String? = null,
//    /// Suggested method for handling overflow while displaying the linked resource.
//    var overflow: String? = null,
//    /// Indicates the condition to be met for the linked resource to be rendered
//    /// within a synthetic spread.
//    var spread: String? = null,
//    ///
//    var numberOfItems: Int? = null,
//    ///
//    var price: Price? = null,
//    ///
//    var indirectAcquisition: MutableList<IndirectAcquisition> = mutableListOf()

) : JSONable, Serializable {

    /**
     * Suggested orientation for the device when displaying the linked resource.
     */
    enum class Orientation(val value: String) {
        AUTO("auto"),
        LANDSCAPE("landscape"),
        PORTRAIT("portrait");

        companion object {
            fun from(value: String?) = Orientation.values().firstOrNull { it.value == value }
        }
    }

    /**
     * Indicates how the linked resource should be displayed in a reading environment that displays
     * synthetic spreads.
     */
    enum class Page(val value: String) {
        LEFT("left"),
        RIGHT("right"),
        CENTER("center");

        companion object {
            fun from(value: String?) = Page.values().firstOrNull { it.value == value }
        }
    }

    override fun toJSON() = JSONObject().apply {
        for ((key, value) in otherProperties) {
            put(key, value)
        }
        put("orientation", orientation?.value)
        put("page", page?.value)
    }

    /**
     * Syntactic sugar to access the `otherProperties` values by subscripting `Properties` directly.
     * `properties["price"] == properties.otherProperties["price"]`
     */
    operator fun get(key: String): Any? = otherProperties[key]
    operator fun set(key: String, value: Any) {
        otherProperties = otherProperties.toMutableMap().apply {
            put(key, value)
        }
    }

    companion object {

        fun fromJSON(json: JSONObject?): Properties {
            json ?: return Properties()

            val orientation = json.remove("orientation") as? String
            val page = json.remove("page") as? String
            val otherProperties = json.toMutableMap()

            return Properties(
                orientation = Orientation.from(orientation),
                page = Page.from(page),
                otherProperties = otherProperties
            )
        }

    }

}