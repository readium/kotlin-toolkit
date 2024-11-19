/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.adapter.pdfium.navigator

import android.graphics.PointF
import com.github.barteksc.pdfviewer.PDFView
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.SimpleOverflow
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.pdf.PdfDocumentFragmentInput
import org.readium.r2.navigator.pdf.PdfEngineProvider
import org.readium.r2.navigator.util.SingleFragmentFactory
import org.readium.r2.navigator.util.createFragmentFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.ReadError

/**
 * Main component to use the PDF navigator with the PDFium adapter.
 *
 * Provide [PdfiumDefaults] to customize the default values that will be used by
 * the navigator for some preferences.
 */
@ExperimentalReadiumApi
public class PdfiumEngineProvider(
    private val defaults: PdfiumDefaults = PdfiumDefaults(),
    private val listener: Listener? = null,
) : PdfEngineProvider<PdfiumSettings, PdfiumPreferences, PdfiumPreferencesEditor> {

    public interface Listener : PdfEngineProvider.Listener {

        /** Called when configuring [PDFView]. */
        public fun onConfigurePdfView(configurator: PDFView.Configurator) {}
    }

    override fun createDocumentFragmentFactory(
        input: PdfDocumentFragmentInput<PdfiumSettings>,
    ): SingleFragmentFactory<PdfiumDocumentFragment> =
        createFragmentFactory {
            PdfiumDocumentFragment(
                publication = input.publication,
                href = input.href,
                initialPageIndex = input.pageIndex,
                initialSettings = input.settings,
                listener = object : PdfiumDocumentFragment.Listener {
                    override fun onResourceLoadFailed(href: Url, error: ReadError) {
                        input.navigatorListener?.onResourceLoadFailed(href, error)
                    }

                    override fun onConfigurePdfView(configurator: PDFView.Configurator) {
                        listener?.onConfigurePdfView(configurator)
                    }

                    override fun onTap(point: PointF): Boolean =
                        input.inputListener?.onTap(TapEvent(point)) ?: false
                }
            )
        }

    override fun computeSettings(metadata: Metadata, preferences: PdfiumPreferences): PdfiumSettings {
        val settingsPolicy = PdfiumSettingsResolver(metadata, defaults)
        return settingsPolicy.settings(preferences)
    }

    override fun computeOverflow(settings: PdfiumSettings): OverflowableNavigator.Overflow =
        SimpleOverflow(
            readingProgression = settings.readingProgression,
            scroll = true,
            axis = settings.scrollAxis
        )

    override fun createPreferenceEditor(
        publication: Publication,
        initialPreferences: PdfiumPreferences,
    ): PdfiumPreferencesEditor =
        PdfiumPreferencesEditor(
            initialPreferences,
            publication.metadata,
            defaults
        )

    override fun createEmptyPreferences(): PdfiumPreferences =
        PdfiumPreferences()
}
