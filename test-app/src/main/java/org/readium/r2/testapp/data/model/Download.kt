/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.TypeConverter

/**
 * Represents an on-going publication download, either from an OPDS catalog or an LCP acquisition.
 *
 * The download [id] is unique relative to its [type] (OPDS or LCP).
 */
@Entity(tableName = Download.TABLE_NAME, primaryKeys = [Download.ID, Download.TYPE])
data class Download(
    @ColumnInfo(name = TYPE)
    val type: Type,
    @ColumnInfo(name = ID)
    val id: String,
    @ColumnInfo(name = COVER)
    val cover: String? = null,
    @ColumnInfo(name = CREATION_DATE, defaultValue = "CURRENT_TIMESTAMP")
    val creation: Long? = null
) {
    enum class Type(val value: String) {
        OPDS("opds"), LCP("lcp");

        class Converter {
            private val values = values().associateBy(Type::value)

            @TypeConverter
            fun fromString(value: String?): Type = values[value]!!

            @TypeConverter
            fun toString(type: Type): String = type.value
        }
    }

    companion object {
        const val TABLE_NAME = "downloads"
        const val CREATION_DATE = "creation_date"
        const val ID = "id"
        const val TYPE = "type"
        const val COVER = "cover"
    }
}
