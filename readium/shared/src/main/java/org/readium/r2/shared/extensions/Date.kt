/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.extensions

import java.util.Date
import kotlinx.datetime.Instant
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import org.readium.r2.shared.InternalReadiumApi

@InternalReadiumApi
public fun Date.toIso8601String(): String =
    Instant.fromEpochMilliseconds(time).format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET)
