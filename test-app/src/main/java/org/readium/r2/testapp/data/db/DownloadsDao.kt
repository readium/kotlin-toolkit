/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.readium.r2.testapp.data.model.Download

@Dao
interface DownloadsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: Download)

    @Query(
        "DELETE FROM " + Download.TABLE_NAME +
            " WHERE " + Download.ID + " = :id AND " + Download.TYPE + " = :type"
    )
    suspend fun delete(id: String, type: Download.Type)

    @Query(
        "SELECT * FROM " + Download.TABLE_NAME +
            " WHERE " + Download.ID + " = :id AND " + Download.TYPE + " = :type"
    )
    suspend fun get(id: String, type: Download.Type): Download?

    @Query(
        "SELECT * FROM " + Download.TABLE_NAME +
            " WHERE " + Download.TYPE + " = :type"
    )
    suspend fun getDownloads(type: Download.Type): List<Download>
}
