/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.domain.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = Catalog.TABLE_NAME)
data class Catalog(
    @PrimaryKey
    @ColumnInfo(name = ID)
    var id: Long? = null,
    @ColumnInfo(name = TITLE)
    var title: String,
    @ColumnInfo(name = HREF)
    var href: String,
    @ColumnInfo(name = TYPE)
    var type: Int
) : Parcelable {
    companion object {

        const val TABLE_NAME = "CATALOG"
        const val ID = "ID"
        const val TITLE = "TITLE"
        const val HREF = "HREF"
        const val TYPE = "TYPE"
    }
}