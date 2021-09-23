/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = License.TABLE_NAME)
data class License(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Long? = null,
    @ColumnInfo(name = LICENSE_ID)
    var licenseId: String,
    @ColumnInfo(name = RIGHTPRINT)
    val rightPrint: Int?,
    @ColumnInfo(name = RIGHTCOPY)
    val rightCopy: Int?,
    @ColumnInfo(name = REGISTERED, defaultValue = "0")
    val registered: Boolean = false
) {

    companion object {

        const val TABLE_NAME = "licenses"
        const val LICENSE_ID = "license_id"
        const val ID = "id"
        const val RIGHTPRINT = "right_print"
        const val RIGHTCOPY = "right_copy"
        const val REGISTERED = "registered"
    }
}
