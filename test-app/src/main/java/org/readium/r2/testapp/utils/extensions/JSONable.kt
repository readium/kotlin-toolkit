/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.utils.extensions

import androidx.room.TypeConverter
import org.readium.r2.shared.JSONable

/**
 * A Room converter to store a [JSONable] object.
 */
class JSONableConverters {

    @TypeConverter
    fun jsonableToString(json: JSONable?): String? =
        json?.toJSON()?.toString()

}
