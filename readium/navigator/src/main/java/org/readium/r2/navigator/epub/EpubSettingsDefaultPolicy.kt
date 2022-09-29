package org.readium.r2.navigator.epub

import org.readium.r2.navigator.epub.extensions.isCjk
import org.readium.r2.navigator.epub.extensions.isRtl
import org.readium.r2.navigator.settings.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation
import org.readium.r2.shared.util.Language

@ExperimentalReadiumApi
internal object EpubSettingsDefaultPolicy : EpubSettingsPolicy {

    override fun reflowableSettings(metadata: Metadata, preferences: Preferences): EpubSettingsValues.Reflowable {
        val (language, readingProgression) = resolveReadingProgression(metadata, preferences)

        val verticalPref = preferences[EpubSettings.VERTICAL_TEXT]
        val verticalText = resolveVerticalText(verticalPref, language, readingProgression)

        return EpubSettingsValues.Reflowable(
            language = language,
            readingProgression = readingProgression,
            backgroundColor = Color.AUTO,
            columnCount = ColumnCount.AUTO,
            fontFamily = null,
            fontSize = 1.0,
            hyphens = true,
            imageFilter = ImageFilter.NONE,
            letterSpacing = 0.0,
            ligatures = true,
            lineHeight = 1.2,
            pageMargins = 1.0,
            paragraphIndent = 0.0,
            paragraphSpacing = 0.0,
            publisherStyles = true,
            scroll = false,
            textAlign = TextAlign.START,
            textColor = Color.AUTO,
            textNormalization = TextNormalization.NONE,
            theme =  Theme.LIGHT,
            typeScale = 1.2,
            verticalText = verticalText,
            wordSpacing = 0.0,
        )
    }

    override fun fixedLayoutSettings(metadata: Metadata, preferences: Preferences): EpubSettingsValues.FixedLayout {
        val (language, readingProgression) = resolveReadingProgression(metadata, preferences)

        return EpubSettingsValues.FixedLayout(
            language = language,
            readingProgression = readingProgression,
            spread = Presentation.Spread.NONE
        )
    }

    private fun resolveReadingProgression(metadata: Metadata, preferences: Preferences): Pair<Language?, ReadingProgression> {
        val rpPref = preferences[EpubSettings.READING_PROGRESSION]
        val langPref = preferences[EpubSettings.LANGUAGE]
        val metadataLanguage = metadata.language

        // Compute language according to the following rule:
        // preference value > metadata value > default value > null
        val language = langPref
            ?: metadataLanguage

        // Compute readingProgression according to the following rule:
        // preference value > value inferred from language preference > metadata value
        // value inferred from metadata languages > default value >
        // value inferred from language default > LTR
        val readingProgression = when {
            rpPref != null ->
                rpPref
            langPref != null ->
                if (langPref.isRtl) ReadingProgression.RTL else ReadingProgression.LTR
            metadata.readingProgression.isHorizontal == true ->
                metadata.readingProgression
            metadataLanguage != null ->
                if (metadataLanguage.isRtl) ReadingProgression.RTL else ReadingProgression.LTR
            else ->
                ReadingProgression.LTR
        }

        return Pair(language, readingProgression)
    }

    // Compute verticalText according to the following rule:
    // preference value > value computed from language > false
    private fun resolveVerticalText(verticalPreference: Boolean?, language: Language?, readingProgression: ReadingProgression) =
        when {
            verticalPreference != null -> verticalPreference
            language != null -> language.isCjk && readingProgression == ReadingProgression.RTL
            else -> false
        }
}
