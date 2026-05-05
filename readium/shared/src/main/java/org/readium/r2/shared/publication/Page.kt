/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.publication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.MapCompanion

/**
 * Indicates how the linked resource should be displayed in a reading environment that displays
 * synthetic spreads.
 */
public val Properties.page: Page?
    get() = Page(this["page"] as? String)

/**
 * Indicates how the linked resource should be displayed in a reading environment that displays
 * synthetic spreads.
 */
@Parcelize
@Serializable
public enum class Page(public val value: String) : Parcelable {
    @SerialName("left")
    LEFT("left"),

    @SerialName("right")
    RIGHT("right"),

    @SerialName("center")
    CENTER("center"),
    ;

    public companion object : MapCompanion<String, Page>(entries.toTypedArray(), Page::value)
}
