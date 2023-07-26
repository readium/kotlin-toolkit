/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.pdf

import android.graphics.PointF
import androidx.fragment.app.Fragment
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.PreferencesEditor
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.resource.Resource

/**
 * To be implemented by adapters for third-party PDF engines which can be used with [PdfNavigatorFragment].
 */
@ExperimentalReadiumApi
public interface PdfEngineProvider<S : Configurable.Settings, P : Configurable.Preferences<P>, E : PreferencesEditor<P>> {

    /**
     * Creates a [PdfDocumentFragment] for [input].
     */
    public suspend fun createDocumentFragment(input: PdfDocumentFragmentInput<S>): PdfDocumentFragment<S>

    /**
     * Creates settings for [metadata] and [preferences].
     */
    public fun computeSettings(metadata: Metadata, preferences: P): S

    /**
     * Infers a [VisualNavigator.Presentation] from settings.
     */
    public fun computePresentation(settings: S): VisualNavigator.Presentation

    /**
     * Creates a preferences editor for [publication] and [initialPreferences].
     */
    public fun createPreferenceEditor(publication: Publication, initialPreferences: P): E

    /**
     * Creates an empty set of preferences of this PDF engine provider.
     */
    public fun createEmptyPreferences(): P
}

@ExperimentalReadiumApi
public typealias PdfDocumentFragmentFactory<S> = suspend (PdfDocumentFragmentInput<S>) -> PdfDocumentFragment<S>

/**
 * A [PdfDocumentFragment] renders a single PDF resource.
 */
@ExperimentalReadiumApi
public abstract class PdfDocumentFragment<S : Configurable.Settings> : Fragment() {

    public interface Listener {
        /**
         * Called when the fragment navigates to a different page.
         */
        public fun onPageChanged(pageIndex: Int)

        /**
         * Called when the user triggers a tap on the document.
         */
        public fun onTap(point: PointF): Boolean

        /**
         * Called when the PDF engine fails to load the PDF document.
         */
        public fun onResourceLoadFailed(link: Link, error: Resource.Exception)
    }

    /**
     * Returns the current page index in the document, from 0.
     */
    public abstract val pageIndex: Int

    /**
     * Jumps to the given page [index].
     *
     * @param animated Indicates if the transition should be animated.
     * @return Whether the jump is valid.
     */
    public abstract fun goToPageIndex(index: Int, animated: Boolean): Boolean

    /**
     * Current settings for the PDF document.
     */
    public abstract var settings: S
}

@ExperimentalReadiumApi
public data class PdfDocumentFragmentInput<S : Configurable.Settings>(
    val publication: Publication,
    val link: Link,
    val initialPageIndex: Int,
    val settings: S,
    val listener: PdfDocumentFragment.Listener?
)
