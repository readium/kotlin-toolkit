package org.readium.r2.navigator2.view.layout

import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation
import org.readium.r2.shared.publication.presentation.presentation
import org.readium.r2.shared.publication.presentation.spread
import java.util.Locale

object DefaultLayoutPolicy : LayoutPolicy {

    override fun resolveSpreadHint(link: Link, publication: Publication, isLandscape: Boolean): Boolean =
        when (link.properties.spread ?: publication.metadata.presentation.spread) {
            Presentation.Spread.AUTO, null -> true
            Presentation.Spread.LANDSCAPE -> isLandscape
            Presentation.Spread.BOTH -> true
            Presentation.Spread.NONE -> false
        }

    override fun resolveReadingProgression(publication: Publication): EffectiveReadingProgression =
        when (publication.metadata.readingProgression) {
            ReadingProgression.RTL -> EffectiveReadingProgression.RTL
            ReadingProgression.LTR -> EffectiveReadingProgression.LTR
            ReadingProgression.TTB -> EffectiveReadingProgression.TTB
            ReadingProgression.BTT -> EffectiveReadingProgression.BTT
            ReadingProgression.AUTO -> autoReadingProgression(publication.metadata)
        }

    override fun resolveContinuous(publication: Publication): Boolean =
        when (publication.metadata.presentation.continuous) {
            null, false -> false
            true -> true
        }

    private fun autoReadingProgression(metadata: Metadata): EffectiveReadingProgression {
        // https://github.com/readium/readium-css/blob/develop/docs/CSS16-internationalization.md#missing-page-progression-direction
        if (metadata.languages.size != 1) {
            return EffectiveReadingProgression.LTR
        }

        var language = metadata.languages.first().lowercase(Locale.ROOT)

        if (language == "zh-hant" || language == "zh-tw") {
            return EffectiveReadingProgression.RTL
        }

        // The region is ignored for ar, fa and he.
        language = language.split("-", limit = 2).first()
        return when (language) {
            "ar", "fa", "he" -> EffectiveReadingProgression.RTL
            else -> EffectiveReadingProgression.LTR
        }
    }
}