/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.extensions

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.*

fun Date.toIso8601String(timeZone: TimeZone = TimeZone.getTimeZone("UTC")): String =
    DateTime(this, DateTimeZone.forTimeZone(timeZone)).toString()

