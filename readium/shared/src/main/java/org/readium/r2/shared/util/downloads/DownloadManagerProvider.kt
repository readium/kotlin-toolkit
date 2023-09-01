/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.downloads

/**
 * To be implemented by custom implementations of [DownloadManager].
 *
 * Downloads can keep going on the background and the listener be called at any time.
 * Naming [DownloadManager]s is useful to retrieve the downloads they own and
 * associated data after app restarted.
 */
public interface DownloadManagerProvider {

    /**
     * Creates a [DownloadManager].
     *
     * @param listener listener to implement to observe the status of downloads
     * @param name name of the download manager
     */
    public fun createDownloadManager(
        listener: DownloadManager.Listener,
        name: String = "default"
    ): DownloadManager
}
