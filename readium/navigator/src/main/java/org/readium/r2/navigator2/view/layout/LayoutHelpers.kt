package org.readium.r2.navigator2.view.layout

import org.readium.r2.shared.publication.presentation.Presentation

object LayoutHelpers {
    fun arePagesCompatible(first: Presentation.Page?, second: Presentation.Page?, readingProgression: EffectiveReadingProgression): Boolean {
        fun canBeLeft(page: Presentation.Page?) = page == null || page == Presentation.Page.LEFT
        fun canBeRight(page: Presentation.Page?) = page == null || page == Presentation.Page.RIGHT

        return when (readingProgression) {
            EffectiveReadingProgression.LTR -> canBeLeft(first) && canBeRight(second)
            EffectiveReadingProgression.RTL -> canBeLeft(second) && canBeRight(first)
            else -> throw IllegalStateException()
        }
    }
}