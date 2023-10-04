/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator

import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.resource.Resource

/**
 * Base interface for a navigator rendering a publication.
 *
 * A few points to keep in mind when implementing this interface:
 *
 * - **The navigator should have a minimal UX** and be focused only on browsing and interacting with
 *   the document. However, it offers a rich API to build a user interface around it.
 * - **The last read page (progression) should not be persisted and restored by the navigator.**
 *   Instead, the reading app will save the [Locator] reported by the navigator in [currentLocator],
 *   and provide the initial location when creating the navigator.
 * - **User accessibility settings should override the behavior when needed** (eg. disabling
 *   animated transition, even when requested by the caller).
 * - **The navigator is the single source of truth for the current location.** So for example, the
 *   TTS should observe the position from the navigator instead of having the reading app move
 *   manually both the navigator and the TTS reader when the user skips forward.
 * - **The navigator should only provide a minimal gestures/interactions set.** For example,
 *   scrolling through a web view or zooming a fixed image is expected from the user. But additional
 *   interactions such as tapping/clicking the edge of the page to skip to the next one should be
 *   implemented by the reading app, and not the navigator.
 */
public interface Navigator {

    /**
     * Publication rendered by this navigator.
     */
    public val publication: Publication

    /**
     * Current position in the publication.
     * Can be used to save a bookmark to the current position.
     */
    public val currentLocator: StateFlow<Locator>

    /**
     * Moves to the position in the publication corresponding to the given [Locator].
     */
    public fun go(locator: Locator, animated: Boolean = false, completion: () -> Unit = {}): Boolean

    /**
     * Moves to the position in the publication targeted by the given link.
     */
    public fun go(link: Link, animated: Boolean = false, completion: () -> Unit = {}): Boolean

    public interface Listener {

        /**
         * Called when a publication resource failed to be loaded.
         */
        public fun onResourceLoadFailed(href: Url, error: Resource.Exception) {}

        /**
         * Called when the navigator jumps to an explicit location, which might break the linear
         * reading progression.
         *
         * For example, it is called when clicking on internal links or programmatically calling
         * [go], but not when turning pages.
         *
         * You can use this callback to implement a navigation history by differentiating between
         * continuous and discontinuous moves.
         */
        public fun onJumpToLocator(locator: Locator) {}
    }

    @Deprecated(
        "Use [currentLocator.value] instead",
        ReplaceWith("currentLocator.value"),
        level = DeprecationLevel.ERROR
    )
    public val currentLocation: Locator? get() = currentLocator.value

    @Deprecated(
        "Use [VisualNavigator.Listener] instead",
        ReplaceWith("VisualNavigator.Listener"),
        level = DeprecationLevel.ERROR
    )
    public interface VisualListener : VisualNavigator.Listener
}

public interface NavigatorDelegate {
    @Deprecated("Observe [currentLocator] instead", level = DeprecationLevel.ERROR)
    public fun locationDidChange(navigator: Navigator? = null, locator: Locator) {}
}
