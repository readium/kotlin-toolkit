/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.readium.r2.shared.util.MapWithDefaultCompanion
import java.util.*

@Parcelize
enum class ReadingProgression(val value: String) : Parcelable {
    /** Right to left */
    RTL("rtl"),
    /** Left to right */
    LTR("ltr"),
    /** Top to bottom */
    TTB("ttb"),
    /** Bottom to top */
    BTT("btt"),
    AUTO("auto");

    companion object : MapWithDefaultCompanion<String, ReadingProgression>(values(), ReadingProgression::value, AUTO) {

        override fun get(key: String?): ReadingProgression? =
            // For backward compatibility, we allow uppercase keys.
            keys.firstOrNull { it == key?.toLowerCase(Locale.ROOT) }
                ?.let { map[it] }

    }

}
