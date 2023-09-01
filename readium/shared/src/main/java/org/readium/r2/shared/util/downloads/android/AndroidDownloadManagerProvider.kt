/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.downloads.android

import android.content.Context
import android.os.Environment
import org.readium.r2.shared.units.Hz
import org.readium.r2.shared.units.hz
import org.readium.r2.shared.util.downloads.DownloadManager
import org.readium.r2.shared.util.downloads.DownloadManagerProvider

public class AndroidDownloadManagerProvider(
    private val context: Context,
    private val destStorage: AndroidDownloadManager.Storage = AndroidDownloadManager.Storage.App,
    private val refreshRate: Hz = 0.1.hz,
    private val allowDownloadsOverMetered: Boolean = true
) : DownloadManagerProvider {

    override fun createDownloadManager(
        listener: DownloadManager.Listener,
        name: String
    ): DownloadManager {
        return AndroidDownloadManager(
            context,
            name,
            destStorage,
            Environment.DIRECTORY_DOWNLOADS,
            refreshRate,
            allowDownloadsOverMetered,
            listener
        )
    }
}
