/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.downloads.android

import android.content.Context
import android.os.Environment
import org.readium.downloads.DownloadManager
import org.readium.downloads.DownloadManagerProvider
import org.readium.r2.shared.units.Hz
import org.readium.r2.shared.units.hz

public class AndroidDownloadManagerProvider(
    private val context: Context,
    private val destStorage: AndroidDownloadManager.Storage = AndroidDownloadManager.Storage.App,
    private val dirType: String = Environment.DIRECTORY_DOWNLOADS,
    private val refreshRate: Hz = 0.1.hz
) : DownloadManagerProvider {

    override fun createDownloadManager(
        listener: DownloadManager.Listener,
        name: String
    ): DownloadManager {
        return AndroidDownloadManager(
            context,
            name,
            destStorage,
            dirType,
            refreshRate,
            listener
        )
    }
}
