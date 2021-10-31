package org.readium.r2.navigator2.settings

import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation

/*data class PresentationSettings(
    val fontSize: Double = 1.0,
    val readingProgression: ReadingProgression = ReadingProgression.AUTO,
    val spread: Presentation.Spread = Presentation.Spread.AUTO,
    val overflow: Presentation.Overflow = Presentation.Overflow.AUTO,
    val continuous: Boolean? = null // null means Automatic
)*/
data class PresentationSettings(
    val fontSize: Double = 1.0,
    val readingProgression: ReadingProgression = ReadingProgression.AUTO,
    val spread: Presentation.Spread = Presentation.Spread.NONE,
    val overflow: Presentation.Overflow = Presentation.Overflow.SCROLLED,
    val continuous: Boolean? = true // null means Automatic
)