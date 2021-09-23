/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.readium.r2.testapp.domain.model.Catalog

@Dao
interface CatalogDao {

    /**
     * Inserts an Catalog
     * @param catalog The Catalog model to insert
     * @return ID of the Catalog model that was added (primary key)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCatalog(catalog: Catalog): Long

    /**
     * Retrieve list of Catalog models based on Catalog model
     * @return List of Catalog models as LiveData
     */
    @Query("SELECT * FROM " + Catalog.TABLE_NAME + " WHERE " + Catalog.TITLE + " = :title AND " + Catalog.HREF + " = :href AND " + Catalog.TYPE + " = :type")
    fun getCatalogModels(title: String, href: String, type: Int): LiveData<List<Catalog>>

    /**
     * Retrieve list of all Catalog models
     * @return List of Catalog models as LiveData
     */
    @Query("SELECT * FROM " + Catalog.TABLE_NAME)
    fun getCatalogModels(): LiveData<List<Catalog>>

    /**
     * Deletes an Catalog model
     * @param id The id of the Catalog model to delete
     */
    @Query("DELETE FROM " + Catalog.TABLE_NAME + " WHERE " + Catalog.ID + " = :id")
    suspend fun deleteCatalog(id: Long)
}