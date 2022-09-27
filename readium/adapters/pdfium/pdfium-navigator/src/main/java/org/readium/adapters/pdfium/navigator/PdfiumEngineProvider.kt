package org.readium.adapters.pdfium.navigator

import org.readium.r2.navigator.pdf.PdfDocumentFragmentInput
import org.readium.r2.navigator.pdf.PdfEngineProvider
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.PdfSupport

@PdfSupport
@ExperimentalReadiumApi
class PdfiumEngineProvider(
    private val listener: PdfiumDocumentFragment.Listener? = null
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

    override fun createDefaultSettings(): PdfiumSettings =
        PdfiumSettings()
}
