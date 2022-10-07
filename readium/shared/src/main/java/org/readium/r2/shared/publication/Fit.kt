/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Suggested method for constraining a resource inside the viewport.
 */
@ExperimentalReadiumApi
@Serializable
enum class Fit(val value: String){
    @SerialName("cover") COVER("cover"),
    @SerialName("contain") CONTAIN("contain"),
    @SerialName("width") WIDTH("width"),
    @SerialName("height") HEIGHT("height");
}
