package org.readium.adapters.pdfium.navigator

import org.readium.r2.navigator.pdf.PdfDocumentFragmentInput
import org.readium.r2.navigator.pdf.PdfEngineProvider
import org.readium.r2.navigator.settings.Preferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.publication.Metadata

@ExperimentalReadiumApi
class PdfiumEngineProvider(
    private val listener: PdfiumDocumentFragment.Listener? = null,
    private val settingsPolicy: PdfiumSettingsPolicy = PdfiumSettingsPolicy()
) : PdfEngineProvider<PdfiumSettings> {

    override suspend fun createDocumentFragment(input: PdfDocumentFragmentInput<PdfiumSettings>) =
        PdfiumDocumentFragment(
            publication = input.publication,
            link = input.link,
            initialPageIndex = input.initialPageIndex,
            settings = input.settings,
            appListener = listener,
            navigatorListener = input.listener
        )

    override fun createSettings(metadata: Metadata, preferences: Preferences): PdfiumSettings =
        PdfiumSettingsFactory(metadata, settingsPolicy).createSettings(preferences)
}
