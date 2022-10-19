package org.readium.adapters.pdfium.navigator

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.ReadingProgression

/**
 * @param readingProgression
 */
@ExperimentalReadiumApi
data class PdfiumSettingsDefaults(
    val readingProgression: ReadingProgression = ReadingProgression.LTR,
)
