/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.pdf

import androidx.fragment.app.Fragment
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.OverflowableNavigator
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
public interface PdfEngineProvider<S : Configurable.Settings, P : Configurable.Preferences<P>, E : PreferencesEditor<P>> {

    public interface Listener

    /**
     * Creates a [PdfDocumentFragment] factory for [input].
     */
    public fun createDocumentFragmentFactory(input: PdfDocumentFragmentInput<S>): SingleFragmentFactory<*>

    /**
     * Creates settings for [metadata] and [preferences].
     */
    public fun computeSettings(metadata: Metadata, preferences: P): S

    /**
     * Infers a [OverflowableNavigator.Overflow] from [settings].
     */
    public fun computeOverflow(settings: S): OverflowableNavigator.Overflow

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
public abstract class PdfDocumentFragment<S : Configurable.Settings> : Fragment() {

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
public data class PdfDocumentFragmentInput<S : Configurable.Settings>(
    val publication: Publication,
    val href: Url,
    val pageIndex: Int,
    val settings: S,
    val navigatorListener: Navigator.Listener?,
    val inputListener: InputListener?,
)
