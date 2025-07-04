/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.fixedlayout.location

import org.readium.navigator.common.GoLocation
import org.readium.navigator.common.Location
import org.readium.navigator.common.SelectionLocation
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Url

@ExperimentalReadiumApi
public data class FixedWebGoLocation(
    val href: Url,
) : GoLocation

@ExperimentalReadiumApi
public data class FixedWebLocation(
    override val href: Url,
) : Location

@ExperimentalReadiumApi
public data class FixedWebSelectionLocation(
    val href: Url,
    val selectedText: String,
    val textBefore: String,
    val textAfter: String,
) : SelectionLocation
