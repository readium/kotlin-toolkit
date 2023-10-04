/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.AbsoluteUrl

/**
 * A navigator supporting hyperlinks.
 */
@ExperimentalReadiumApi
public interface HyperlinkNavigator : Navigator {

    @ExperimentalReadiumApi
    public interface Listener : Navigator.Listener {

        /**
         * Called when a link to an internal resource was clicked in the navigator.
         *
         * You can use this callback to perform custom navigation like opening a new window
         * or other operations.
         *
         * By returning false the navigator wont try to open the link itself and it is up
         * to the calling app to decide how to display the link.
         */
        @ExperimentalReadiumApi
        public fun shouldJumpToLink(link: Link): Boolean { return true }

        /**
         * Called when a link to an external URL was clicked in the navigator.
         */
        @ExperimentalReadiumApi
        public fun onOpenExternalLinkRequested(url: AbsoluteUrl)
    }
}
