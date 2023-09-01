/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.data

import org.readium.r2.testapp.data.db.DownloadsDao
import org.readium.r2.testapp.data.model.Download

class DownloadRepository(
    private val downloadsDao: DownloadsDao
) {

    suspend fun insertOpdsDownload(
        manager: String,
        id: String,
        cover: String?
    ) {
        downloadsDao.insert(
            Download(manager = manager, id = id, extra = cover)
        )
    }

    suspend fun getOpdsDownloadCover(
        manager: String,
        id: String
    ): String? {
        return downloadsDao.get(manager, id)!!.extra
    }

    suspend fun removeDownload(
        manager: String,
        id: String
    ) {
        downloadsDao.delete(manager, id)
    }
}
