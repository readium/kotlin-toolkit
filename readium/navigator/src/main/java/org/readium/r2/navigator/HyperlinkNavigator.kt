/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator

import android.graphics.PointF
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.AbsoluteUrl

/**
 * A navigator supporting hyperlinks.
 */
@ExperimentalReadiumApi
public interface HyperlinkNavigator : Navigator {

    @ExperimentalReadiumApi
    public sealed interface LinkContext {
        public val referrer: String
        public val activationPoint: PointF
    }

    @ExperimentalReadiumApi
    public data class FootnoteContext(
        public override val referrer: String,
        override val activationPoint: PointF,
        public val content: String
    ) : LinkContext

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
        public fun shouldFollowInternalLink(link: Link, context: LinkContext?): Boolean { return true }

        /**
         * Called when a link to an external URL was activated in the navigator.
         *
         * If it is an HTTP URL, you should open it with a `CustomTabsIntent` or `WebView`, for
         * example:
         *
         * ```kotlin
         * override fun onExternalLinkActivated(url: AbsoluteUrl) {
         *     if (!url.isHttp) return
         *
         *     val context = requireActivity()
         *     val uri = url.toUri()
         *
         *     try {
         *         CustomTabsIntent.Builder()
         *             .build()
         *             .launchUrl(context, uri)
         *     } catch (e: ActivityNotFoundException) {
         *         context.startActivity(Intent(Intent.ACTION_VIEW, uri))
         *     }
         * }
         * ```
         */
        @ExperimentalReadiumApi
        public fun onExternalLinkActivated(url: AbsoluteUrl)
    }
}
