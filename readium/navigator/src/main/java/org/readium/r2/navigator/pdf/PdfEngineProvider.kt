/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.pdf

import androidx.fragment.app.Fragment
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.PreferencesEditor
import org.readium.r2.navigator.util.SingleFragmentFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url

/**
 * To be implemented by adapters for third-party PDF engines which can be used with [PdfNavigatorFragment].
 */
@ExperimentalReadiumApi
public interface PdfEngineProvider<F : PdfDocumentFragment<L, S>, L : PdfDocumentFragment.Listener, S : Configurable.Settings, P : Configurable.Preferences<P>, E : PreferencesEditor<P>> {

    /**
     * Creates a [PdfDocumentFragment] factory for [input].
     */
    public fun createDocumentFragmentFactory(input: PdfDocumentFragmentInput<L, S>): SingleFragmentFactory<F>

    /**
     * Creates settings for [metadata] and [preferences].
     */
    public fun computeSettings(metadata: Metadata, preferences: P): S

    /**
     * Infers a [VisualNavigator.Presentation] from [settings].
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

/**
 * A [PdfDocumentFragment] renders a single PDF document.
 */
@ExperimentalReadiumApi
public abstract class PdfDocumentFragment<L : PdfDocumentFragment.Listener, S : Configurable.Settings> : Fragment() {

    public interface Listener

    /**
     * Current page index displayed in the PDF document.
     */
    public abstract val pageIndex: StateFlow<Int>

    /**
     * Jumps to the given page [index].
     *
     * @param animated Indicates if the transition should be animated.
     * @return Whether the jump is valid.
     */
    public abstract fun goToPageIndex(index: Int, animated: Boolean): Boolean

    /**
     * Submits a new set of settings.
     */
    public abstract fun applySettings(settings: S)
}

@ExperimentalReadiumApi
public data class PdfDocumentFragmentInput<L : PdfDocumentFragment.Listener, S : Configurable.Settings>(
    val publication: Publication,
    val href: Url,
    val pageIndex: Int,
    val settings: S,
    val listener: L?,
    val inputListener: InputListener
)
