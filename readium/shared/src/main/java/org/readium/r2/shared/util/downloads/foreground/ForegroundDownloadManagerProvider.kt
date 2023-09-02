/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.downloads.foreground

import org.readium.r2.shared.util.downloads.DownloadManager
import org.readium.r2.shared.util.downloads.DownloadManagerProvider
import org.readium.r2.shared.util.http.HttpClient

public class ForegroundDownloadManagerProvider(
    private val httpClient: HttpClient
) : DownloadManagerProvider {

    override fun createDownloadManager(
        listener: DownloadManager.Listener,
        name: String
    ): DownloadManager {
        return ForegroundDownloadManager(httpClient, listener)
    }
}
