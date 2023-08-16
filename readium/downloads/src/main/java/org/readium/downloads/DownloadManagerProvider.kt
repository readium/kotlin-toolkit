/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.downloads

public interface DownloadManagerProvider {

    public fun createDownloadManager(listener: DownloadManager.Listener): DownloadManager
}
