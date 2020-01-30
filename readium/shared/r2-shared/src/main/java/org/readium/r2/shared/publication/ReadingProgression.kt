/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication

import java.util.*

enum class ReadingProgression(val value: String) {
    /** Right to left */
    RTL("rtl"),
    /** Left to right */
    LTR("ltr"),
    /** Top to bottom */
    TTB("ttb"),
    /** Bottom to top */
    BTT("btt"),
    AUTO("auto");

    companion object {

        /**
         * Returns the [ReadingProgression] from its RWPM JSON representation.
         * Fallbacks on [AUTO].
         */
        fun from(value: String?): ReadingProgression =
            ReadingProgression.values().firstOrNull { it.value == value?.toLowerCase(Locale.ROOT) }
                ?: AUTO

    }

}
