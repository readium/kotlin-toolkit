/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator

import android.graphics.PointF
import android.view.View
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.media.MediaPlayback
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression as PublicationReadingProgression
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

    /**
     * Moves to the next content portion (eg. page) in the reading progression direction.
     */
    public fun goForward(animated: Boolean = false, completion: () -> Unit = {}): Boolean

    /**
     * Moves to the previous content portion (eg. page) in the reading progression direction.
     */
    public fun goBackward(animated: Boolean = false, completion: () -> Unit = {}): Boolean

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

/**
 * A navigator rendering the publication visually on-screen.
 */
public interface VisualNavigator : Navigator {

    /**
     * View displaying the publication.
     */
    public val publicationView: View

    /**
     * Current presentation rendered by the navigator.
     */
    @ExperimentalReadiumApi
    public val presentation: StateFlow<Presentation>

    @ExperimentalReadiumApi
    public interface Presentation {
        /**
         * Horizontal direction of progression across resources.
         */
        public val readingProgression: ReadingProgression

        /**
         * If the overflow of the content is managed through scroll instead of pagination.
         */
        public val scroll: Boolean

        /**
         * Main axis along which the resources are laid out.
         */
        public val axis: Axis
    }

    /**
     * Returns the [Locator] to the first content element that begins on the current screen.
     */
    @ExperimentalReadiumApi
    public suspend fun firstVisibleElementLocator(): Locator? =
        currentLocator.value

    /**
     * Adds a new [InputListener] to receive touch, mouse or keyboard events.
     *
     * Registration order is critical, as listeners may consume the events and prevent others from
     * receiving them.
     */
    @ExperimentalReadiumApi
    public fun addInputListener(listener: InputListener)

    /**
     * Removes a previously registered [InputListener].
     */
    @ExperimentalReadiumApi
    public fun removeInputListener(listener: InputListener)

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
        public fun shouldJumpToLink(link: Link): Boolean { return true }

        @Deprecated("Use `addInputListener` instead", level = DeprecationLevel.ERROR)
        public fun onTap(point: PointF): Boolean = false

        @Deprecated("Use `addInputListener` instead", level = DeprecationLevel.ERROR)
        public fun onDragStart(startPoint: PointF, offset: PointF): Boolean = false

        @Deprecated("Use `addInputListener` instead", level = DeprecationLevel.ERROR)
        public fun onDragMove(startPoint: PointF, offset: PointF): Boolean = false

        @Deprecated("Use `addInputListener` instead", level = DeprecationLevel.ERROR)
        public fun onDragEnd(startPoint: PointF, offset: PointF): Boolean = false
    }

    /**
     * Current reading progression direction.
     */
    @Deprecated(
        "Use `presentation.value.readingProgression` instead",
        ReplaceWith("presentation.value.readingProgression"),
        level = DeprecationLevel.ERROR
    )
    public val readingProgression: PublicationReadingProgression
}

/**
 * Moves to the left content portion (eg. page) relative to the reading progression direction.
 */
@OptIn(ExperimentalReadiumApi::class)
public fun VisualNavigator.goLeft(animated: Boolean = false, completion: () -> Unit = {}): Boolean {
    return when (presentation.value.readingProgression) {
        ReadingProgression.LTR ->
            goBackward(animated = animated, completion = completion)

        ReadingProgression.RTL ->
            goForward(animated = animated, completion = completion)
    }
}

/**
 * Moves to the right content portion (eg. page) relative to the reading progression direction.
 */
@OptIn(ExperimentalReadiumApi::class)
public fun VisualNavigator.goRight(animated: Boolean = false, completion: () -> Unit = {}): Boolean {
    return when (presentation.value.readingProgression) {
        ReadingProgression.LTR ->
            goForward(animated = animated, completion = completion)

        ReadingProgression.RTL ->
            goBackward(animated = animated, completion = completion)
    }
}

/**
 * A navigator rendering an audio or video publication.
 */
@ExperimentalAudiobook
public interface MediaNavigator : Navigator {

    /**
     * Current playback information.
     */
    public val playback: Flow<MediaPlayback>

    /**
     * Indicates whether the navigator is currently playing.
     */
    public val isPlaying: Boolean

    /**
     * Sets the speed of the media playback.
     *
     * Normal speed is 1.0 and 0.0 is incorrect.
     */
    public fun setPlaybackRate(rate: Double)

    /**
     * Resumes or start the playback at the current location.
     */
    public fun play()

    /**
     * Pauses the playback.
     */
    public fun pause()

    /**
     * Toggles the playback.
     * Can be useful as a handler for play/pause button.
     */
    public fun playPause()

    /**
     * Stops the playback.
     *
     * Compared to [pause], the navigator may clear its state in whatever way is appropriate. For
     * example, recovering a player's resources.
     */
    public fun stop()

    /**
     * Seeks to the given time in the current resource.
     */
    public fun seekTo(position: Duration)

    /**
     * Seeks relatively from the current position in the current resource.
     */
    public fun seekRelative(offset: Duration)

    public interface Listener : Navigator.Listener
}
