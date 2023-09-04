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

    suspend fun getLcpDownloads(): List<Download> {
        return downloadsDao.getLcpDownloads()
    }

    suspend fun getOpdsDownloads(): List<Download> {
        return downloadsDao.getOpdsDownloads()
    }

    suspend fun insertLcpDownload(
        id: String,
        cover: String?
    ) {
        downloadsDao.insert(
            Download(id = id, type = Download.TYPE_LCP, extra = cover)
        )
    }

    suspend fun insertOpdsDownload(
        id: String,
        cover: String?
    ) {
        downloadsDao.insert(
            Download(id = id, type = Download.TYPE_OPDS, extra = cover)
        )
    }

    suspend fun getLcpDownloadCover(
        id: String
    ): String? {
        return downloadsDao.get(id, Download.TYPE_LCP)!!.extra
    }
    suspend fun getOpdsDownloadCover(
        id: String
    ): String? {
        return downloadsDao.get(id, Download.TYPE_OPDS)!!.extra
    }

    suspend fun removeLcpDownload(
        id: String
    ) {
        downloadsDao.delete(id, Download.TYPE_LCP)
    }

    suspend fun removeOpdsDownload(
        id: String
    ) {
        downloadsDao.delete(id, Download.TYPE_OPDS)
    }
}
