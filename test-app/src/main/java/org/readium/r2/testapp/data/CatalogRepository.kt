/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.data

import kotlinx.coroutines.flow.Flow
import org.readium.r2.testapp.data.db.CatalogDao
import org.readium.r2.testapp.data.model.Catalog

class CatalogRepository(private val catalogDao: CatalogDao) {

    suspend fun insertCatalog(catalog: Catalog): Long {
        return catalogDao.insertCatalog(catalog)
    }

    fun getCatalogsFromDatabase(): Flow<List<Catalog>> = catalogDao.getCatalogModels()

    suspend fun deleteCatalog(id: Long) = catalogDao.deleteCatalog(id)
}
