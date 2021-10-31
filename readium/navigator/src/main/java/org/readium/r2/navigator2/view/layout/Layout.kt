package org.readium.r2.navigator2.view.layout

import org.readium.r2.shared.publication.presentation.Presentation

data class Layout(
    val progression: EffectiveReadingProgression,
    val spread: Presentation.Spread,
    val overflow: Presentation.Overflow,
    val continuous: Boolean,
    val landscape: Boolean
)
