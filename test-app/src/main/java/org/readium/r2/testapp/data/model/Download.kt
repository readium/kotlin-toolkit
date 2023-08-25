/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = Download.TABLE_NAME, primaryKeys = [Download.MANAGER, Download.ID])
data class Download(
    @ColumnInfo(name = MANAGER)
    val manager: String,
    @ColumnInfo(name = ID)
    val id: Long,
    @ColumnInfo(name = EXTRA)
    val extra: String? = null,
    @ColumnInfo(name = CREATION_DATE, defaultValue = "CURRENT_TIMESTAMP")
    val creation: Long? = null
) {

    companion object {
        const val TABLE_NAME = "downloads"
        const val CREATION_DATE = "creation_date"
        const val MANAGER = "manager"
        const val ID = "id"
        const val EXTRA = "cover"
    }
}
