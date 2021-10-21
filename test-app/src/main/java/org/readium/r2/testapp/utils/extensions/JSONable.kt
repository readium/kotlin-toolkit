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
