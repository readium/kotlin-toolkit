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

@Entity(tableName = Passphrase.TABLE_NAME)
data class Passphrase(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Long? = null,
    @ColumnInfo(name = LICENSEID)
    var licenseId: String?,
    @ColumnInfo(name = PROVIDER)
    val provider: String?,
    @ColumnInfo(name = USERID)
    val userId: String?,
    @ColumnInfo(name = PASSPHRASE)
    val passphrase: String
) {

    companion object {

        const val TABLE_NAME = "passphrases"
        const val ID = "id"
        const val LICENSEID = "license_id"
        const val PROVIDER = "provider"
        const val USERID = "user_id"
        const val PASSPHRASE = "passphrase"
    }
}
