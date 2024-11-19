/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.publication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.*
import org.readium.r2.shared.publication.Accessibility.AccessMode.Companion.toJSONArray
import org.readium.r2.shared.publication.Accessibility.Feature.Companion.toJSONArray
import org.readium.r2.shared.publication.Accessibility.Hazard.Companion.toJSONArray
import org.readium.r2.shared.publication.Accessibility.PrimaryAccessMode.Companion.toJSONArray
import org.readium.r2.shared.publication.Accessibility.Profile.Companion.toJSONArray
import org.readium.r2.shared.util.MapCompanion
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.logging.log

/**
 * Holds the accessibility metadata of a Publication.
 *
 * https://www.w3.org/2021/a11y-discov-vocab/latest/
 * https://readium.org/webpub-manifest/schema/a11y.schema.json
 *
 * @property conformsTo An established standard to which the described resource conforms.
 * @property certification Certification of accessible publications.
 * @property summary A human-readable summary of specific accessibility features or deficiencies,
 *   consistent with the other accessibility metadata but expressing subtleties such as
 *   "short descriptions are present but long descriptions will be needed for non-visual users" or
 *   "short descriptions are present and no long descriptions are needed."
 * @property accessModes The human sensory perceptual system or cognitive faculty through which
 *   a person may process or perceive information.
 * @property accessModesSufficient A list of single or combined accessModes that are sufficient
 *   to understand all the intellectual content of a resource.
 * @property [features] Content features of the resource, such as accessible media, alternatives and
 *   supported enhancements for accessibility.
 * @property [hazards] A characteristic of the described resource that is physiologically
 *   dangerous to some users.
 */
@Parcelize
public data class Accessibility(
    val conformsTo: Set<Profile>,
    val certification: Certification? = null,
    val summary: String? = null,
    val accessModes: Set<AccessMode>,
    val accessModesSufficient: Set<Set<PrimaryAccessMode>>,
    val features: Set<Feature>,
    val hazards: Set<Hazard>,
) : JSONable, Parcelable {

    /**
     * Accessibility profile.
     */
    @Parcelize
    public data class Profile(public val uri: String) : Parcelable {

        public companion object {

            public val EPUB_A11Y_10_WCAG_20_A: Profile = Profile(
                "http://www.idpf.org/epub/a11y/accessibility-20170105.html#wcag-a"
            )

            public val EPUB_A11Y_10_WCAG_20_AA: Profile = Profile(
                "http://www.idpf.org/epub/a11y/accessibility-20170105.html#wcag-aa"
            )

            public val EPUB_A11Y_10_WCAG_20_AAA: Profile = Profile(
                "http://www.idpf.org/epub/a11y/accessibility-20170105.html#wcag-aaa"
            )

            public fun Set<Profile>.toJSONArray(): JSONArray =
                JSONArray(this.map(Profile::uri))
        }
    }

    /**
     * Certification of accessible publications.
     *
     * @property certifiedBy Identifies a party responsible for the testing and certification of
     *   the accessibility of a Publication.
     * @property credential Identifies a credential or badge that establishes the authority of
     *   the party identified in the associated [certifiedBy] property to certify content accessible.
     * @property report Provides a link to an accessibility report created by the party identified
     *   in the associated [certifiedBy] property.
     */
    @Parcelize
    public data class Certification(
        val certifiedBy: String?,
        val credential: String?,
        val report: String?,
    ) : JSONable, Parcelable {

        override fun toJSON(): JSONObject = JSONObject().apply {
            put("certifiedBy", certifiedBy)
            put("credential", credential)
            put("report", report)
        }

        public companion object {

            /**
             * Parses a [Certification] from its RWPM JSON representation.
             *
             * If the certification can't be parsed, a warning will be logged with [warnings].
             */
            public fun fromJSON(
                json: JSONObject?,
                warnings: WarningLogger? = null,
            ): Certification? {
                json ?: return null
                val certifiedBy = json.optNullableString("certifiedBy")
                val credential = json.optNullableString("credential")
                val report = json.optNullableString("report")

                if (listOfNotNull(certifiedBy, credential, report).isEmpty()) {
                    warnings?.log(
                        Certification::class.java,
                        "no valid property in certification object",
                        json
                    )
                    return null
                }

                return Certification(
                    certifiedBy = certifiedBy,
                    credential = credential,
                    report = report
                )
            }
        }
    }

    /**
     * A human sensory perceptual system or cognitive faculty through which a person may process
     * or perceive information.
     */
    @Parcelize
    public data class AccessMode(public val value: String) : Parcelable {

        public companion object {
            /**
             * Indicates that the resource contains information encoded in auditory form.
             */
            public val AUDITORY: AccessMode = AccessMode("auditory")

            /**
             * Indicates that the resource contains charts encoded in visual form.
             */
            public val CHART_ON_VISUAL: AccessMode = AccessMode("chartOnVisual")

            /**
             * Indicates that the resource contains chemical equations encoded in visual form.
             */
            public val CHEM_ON_VISUAL: AccessMode = AccessMode("chemOnVisual")

            /**
             * Indicates that the resource contains information encoded such that color perception is necessary.
             */
            public val COLOR_DEPENDENT: AccessMode = AccessMode("colorDependent")

            /**
             * Indicates that the resource contains diagrams encoded in visual form.
             */
            public val DIAGRAM_ON_VISUAL: AccessMode = AccessMode("diagramOnVisual")

            /**
             * Indicates that the resource contains mathematical notations encoded in visual form.
             */
            public val MATH_ON_VISUAL: AccessMode = AccessMode("mathOnVisual")

            /**
             * Indicates that the resource contains musical notation encoded in visual form.
             */
            public val MUSIC_ON_VISUAL: AccessMode = AccessMode("musicOnVisual")

            /**
             * Indicates that the resource contains information encoded in tactile form.
             *
             * Note that although an indication of a tactile mode often indicates the content is encoded
             * using a braille system, this is not always the case. Tactile perception may also indicate,
             * for example, the use of tactile graphics to convey information.
             */
            public val TACTILE: AccessMode = AccessMode("tactile")

            /**
             * Indicates that the resource contains text encoded in visual form.
             */
            public val TEXT_ON_VISUAL: AccessMode = AccessMode("textOnVisual")

            /**
             * Indicates that the resource contains information encoded in textual form.
             */
            public val TEXTUAL: AccessMode = AccessMode("textual")

            /**
             * Indicates that the resource contains information encoded in visual form.
             */
            public val VISUAL: AccessMode = AccessMode("visual")

            /**
             * Creates a list of [AccessMode] from its RWPM JSON representation.
             */
            public fun fromJSONArray(json: JSONArray?): List<AccessMode> =
                json?.filterIsInstance(String::class.java)
                    ?.map { AccessMode(it) }
                    .orEmpty()

            public fun Set<AccessMode>.toJSONArray(): JSONArray =
                JSONArray(this.map(AccessMode::value))
        }
    }

    /**
     * A human primary sensory perceptual system or cognitive faculty through which a person may process
     * or perceive information.
     */
    @Parcelize
    @Serializable
    public enum class PrimaryAccessMode(public val value: String) : Parcelable {

        /**
         * Indicates that auditory perception is necessary to consume the information.
         */
        @SerialName("auditory")
        AUDITORY("auditory"),

        /**
         * Indicates that tactile perception is necessary to consume the information.
         */
        @SerialName("tactile")
        TACTILE("tactile"),

        /**
         * Indicates that the ability to read textual content is necessary to consume the information.
         *
         * Note that reading textual content does not require visual perception, as textual content
         * can be rendered as audio using a text-to-speech capable device or assistive technology.
         */
        @SerialName("textual")
        TEXTUAL("textual"),

        /**
         * Indicates that visual perception is necessary to consume the information.
         */
        @SerialName("visual")
        VISUAL("visual"),
        ;

        public companion object : MapCompanion<String, PrimaryAccessMode>(
            entries.toTypedArray(),
            PrimaryAccessMode::value
        ) {

            /**
             * Creates a list of [PrimaryAccessMode] from its RWPM JSON representation.
             */
            public fun fromJSONArray(json: JSONArray?): List<PrimaryAccessMode> =
                json?.filterIsInstance(String::class.java)
                    ?.mapNotNull { get(it) }
                    .orEmpty()

            public fun Set<PrimaryAccessMode>.toJSONArray(): JSONArray =
                JSONArray(this.map(PrimaryAccessMode::value))
        }
    }

    /**
     * A content feature of the described resource, such as accessible media, alternatives and
     * supported enhancements for accessibility.
     */
    @Parcelize
    public data class Feature(public val value: String) : Parcelable {

        public companion object {
            /**
             * The work includes annotations from the author, instructor and/or others.
             */
            public val ANNOTATIONS: Feature = Feature("annotations")

            /**
             * Indicates the resource includes ARIA roles to organize and improve the structure and navigation.
             *
             * The use of this value corresponds to the inclusion of Document Structure, Landmark,
             * Live Region, and Window roles [WAI-ARIA].
             */
            public val ARIA: Feature = Feature("ARIA")

            /**
             * The work includes bookmarks to facilitate navigation to key points.
             */
            public val BOOKMARKS: Feature = Feature("bookmark")

            /**
             * The work includes an index to the content.
             */
            public val INDEX: Feature = Feature("index")

            /**
             * The work includes equivalent print page numbers. This setting is most commonly used
             * with ebooks for which there is a print equivalent.
             */
            public val PRINT_PAGE_NUMBERS: Feature = Feature("printPageNumbers")

            /**
             * The reading order of the content is clearly defined in the markup
             * (e.g., figures, sidebars and other secondary content has been marked up to allow it
             * to be skipped automatically and/or manually escaped from).
             */
            public val READING_ORDER: Feature = Feature("readingOrder")

            /**
             * The use of headings in the work fully and accurately reflects the document hierarchy,
             * allowing navigation by assistive technologies.
             */
            public val STRUCTURAL_NAVIGATION: Feature = Feature("structuralNavigation")

            /**
             * The work includes a table of contents that provides links to the major sections of the content.
             */
            public val TABLE_OF_CONTENTS: Feature = Feature("tableOfContents")

            /**
             * The contents of the PDF have been tagged to permit access by assistive technologies.
             */
            public val TAGGED_PDF: Feature = Feature("taggedPDF")

            /**
             * Alternative text is provided for visual content (e.g., via the HTML `alt` attribute).
             */
            public val ALTERNATIVE_TEXT: Feature = Feature("alternativeText")

            /**
             * Audio descriptions are available (e.g., via an HTML `track` element with its `kind`
             * attribute set to "descriptions").
             */
            public val AUDIO_DESCRIPTION: Feature = Feature("audioDescription")

            /**
             * Indicates that synchronized captions are available for audio and video content.
             */
            public val CAPTIONS: Feature = Feature("captions")

            /**
             * Textual descriptions of math equations are included, whether in the alt attribute
             * for image-based equations,
             */
            public val DESCRIBED_MATH: Feature = Feature("describeMath")

            /**
             * Descriptions are provided for image-based visual content and/or complex structures
             * such as tables, mathematics, diagrams, and charts.
             */
            public val LONG_DESCRIPTION: Feature = Feature("longDescription")

            /**
             * Indicates that `ruby` annotations HTML are provided in the content. Ruby annotations
             * are used as pronunciation guides for the logographic characters for languages like
             * Chinese or Japanese. It makes difficult Kanji or CJK ideographic characters more accessible.
             *
             * The absence of rubyAnnotations implies that no CJK ideographic characters have ruby.
             */
            public val RUBY_ANNOTATIONS: Feature = Feature("rubyAnnotations")

            /**
             * Sign language interpretation is available for audio and video content.
             */
            public val SIGN_LANGUAGE: Feature = Feature("signLanguage")

            /**
             * Indicates that a transcript of the audio content is available.
             */
            public val TRANSCRIPT: Feature = Feature("transcript")

            /**
             * Display properties are controllable by the user. This property can be set, for example,
             * if custom CSS style sheets can be applied to the content to control the appearance.
             * It can also be used to indicate that styling in document formats like Word and PDF
             * can be modified.
             */
            public val DISPLAY_TRANSFORMABILITY: Feature = Feature("displayTransformability")

            /**
             * Describes a resource that offers both audio and text, with information that allows them
             * to be rendered simultaneously. The granularity of the synchronization is not specified.
             * This term is not recommended when the only material that is synchronized is
             * the document headings.
             */
            public val SYNCHRONIZED_AUDIO_TEXT: Feature = Feature("synchronizedAudioText")

            /**
             * For content with timed interaction, this value indicates that the user can control
             * the timing to meet their needs (e.g., pause and reset)
             */
            public val TIMING_CONTROL: Feature = Feature("timingControl")

            /**
             * No digital rights management or other content restriction protocols have been applied
             * to the resource.
             */
            public val UNLOCKED: Feature = Feature("unlocked")

            /**
             * Identifies that chemical information is encoded using the ChemML markup language.
             */
            public val CHEM_ML: Feature = Feature("ChemML")

            /**
             * Identifies that mathematical equations and formulas are encoded in the LaTeX
             * typesetting system.
             */
            public val LATEX: Feature = Feature("latex")

            /**
             * Identifies that mathematical equations and formulas are encoded in MathML.
             */
            public val MATH_ML: Feature = Feature("MathML")

            /**
             * One or more of SSML, Pronunciation-Lexicon, and CSS3-Speech properties has been used
             * to enhance text-to-speech playback quality.
             */
            public val TTS_MARKUP: Feature = Feature("ttsMarkup")

            /**
             * Audio content with speech in the foreground meets the contrast thresholds set out
             * in WCAG Success Criteria 1.4.7.
             */
            public val HIGH_CONTRAST_AUDIO: Feature = Feature("highContrastAudio")

            /**
             * Content meets the visual contrast threshold set out in WCAG Success Criteria 1.4.6.
             */
            public val HIGH_CONTRAST_DISPLAY: Feature = Feature("highContrastDisplay")

            /**
             * The content has been formatted to meet large print guidelines.
             *
             * The property is not set if the font size can be increased. See DISPLAY_TRANSFORMABILITY.
             */
            public val LARGE_PRINT: Feature = Feature("largePrint")

            /**
             * The content is in braille format, or alternatives are available in braille.
             */
            public val BRAILLE: Feature = Feature("braille")

            /**
             * When used with creative works such as books, indicates that the resource includes
             * tactile graphics. When used to describe an image resource or physical object,
             * indicates that the resource is a tactile graphic.
             */
            public val TACTILE_GRAPHIC: Feature = Feature("tactileGraphic")

            /**
             * When used with creative works such as books, indicates that the resource includes models
             * to generate tactile 3D objects. When used to describe a physical object,
             * indicates that the resource is a tactile 3D object.
             */
            public val TACTILE_OBJECT: Feature = Feature("tactileObject")

            /**
             * Indicates that the resource does not contain any accessibility features.
             */
            public val NONE: Feature = Feature("none")

            /**
             * Creates a list of [Feature] from its RWPM JSON representation.
             */
            public fun fromJSONArray(json: JSONArray?): List<Feature> =
                json?.filterIsInstance(String::class.java)
                    ?.map { Feature(it) }
                    .orEmpty()

            public fun Set<Feature>.toJSONArray(): JSONArray =
                JSONArray(this.map(Feature::value))
        }
    }

    /**
     * A characteristic of the described resource that is physiologically dangerous to some users.
     */
    @Parcelize
    public data class Hazard(public val value: String) : Parcelable {

        public companion object {

            /**
             * Indicates that the resource presents a flashing hazard for photosensitive persons.
             */
            public val FLASHING: Hazard = Hazard("flashing")

            /**
             * Indicates that the resource does not present a flashing hazard.
             */
            public val NO_FLASHING_HAZARD: Hazard = Hazard("noFlashingHazard")

            /**
             * Indicates that the resource contains instances of motion simulation that
             * may affect some individuals.
             *
             * Some examples of motion simulation include video games with a first-person perspective
             * and CSS-controlled backgrounds that move when a user scrolls a page.
             */
            public val MOTION_SIMULATION: Hazard = Hazard("motionSimulation")

            /**
             * Indicates that the resource does not contain instances of motion simulation.
             *
             * See MOTION_SIMULATION.
             */
            public val NO_MOTION_SIMULATION_HAZARD: Hazard = Hazard("noMotionSimulationHazard")

            /**
             * Indicates that the resource contains auditory sounds that may affect some individuals.
             */
            public val SOUND: Hazard = Hazard("sound")

            /**
             * Indicates that the resource does not contain auditory hazards.
             */
            public val NO_SOUND_HAZARD: Hazard = Hazard("noSoundHazard")

            /**
             * Indicates that the author is not able to determine if the resource presents any hazards.
             */
            public val UNKNOWN: Hazard = Hazard("unknown")

            /**
             * Indicates that the resource does not contain any hazards.
             */
            public val NONE: Hazard = Hazard("none")

            /**
             * Creates a list of [Hazard] from its RWPM JSON representation.
             */
            public fun fromJSONArray(json: JSONArray?): List<Hazard> =
                json?.filterIsInstance(String::class.java)
                    ?.map { Hazard(it) }
                    .orEmpty()

            public fun Set<Hazard>.toJSONArray(): JSONArray =
                JSONArray(this.map(Hazard::value))
        }
    }

    override fun toJSON(): JSONObject = JSONObject().apply {
        putIfNotEmpty("conformsTo", conformsTo.toJSONArray())
        put("certification", certification?.toJSON())
        put("summary", summary)
        putIfNotEmpty("accessMode", accessModes.toJSONArray())
        putIfNotEmpty("accessModeSufficient", accessModesSufficient.map { it.toJSONArray() })
        putIfNotEmpty("hazard", hazards.toJSONArray())
        putIfNotEmpty("feature", features.toJSONArray())
    }

    public companion object {

        /**
         * Parses a [Accessibility] from its RWPM JSON representation.
         */
        public fun fromJSON(json: Any?): Accessibility? {
            if (json !is JSONObject) {
                return null
            }

            val conformsTo = json.optStringsFromArrayOrSingle("conformsTo")
                .map { Profile(it) }
            val certification = Certification.fromJSON(json.remove("certification") as? JSONObject)
            val summary = json.optNullableString("summary")

            val accessModes = AccessMode.fromJSONArray(json.remove("accessMode") as? JSONArray)
            val accessModesSufficient = (json.remove("accessModeSufficient") as? JSONArray)
                ?.mapNotNull {
                    when (it) {
                        is JSONArray -> PrimaryAccessMode.fromJSONArray(it).toSet()
                            .takeUnless(Set<PrimaryAccessMode>::isEmpty)
                        is String -> setOfNotNull(PrimaryAccessMode(it))
                            .takeUnless(Set<PrimaryAccessMode>::isEmpty)
                        else -> null
                    }
                }.orEmpty()

            val features = Feature.fromJSONArray(json.remove("feature") as? JSONArray)
            val hazards = Hazard.fromJSONArray(json.remove("hazard") as? JSONArray)

            return Accessibility(
                conformsTo = conformsTo.toSet(),
                certification = certification,
                summary = summary,
                accessModes = accessModes.toSet(),
                accessModesSufficient = accessModesSufficient.toSet(),
                features = features.toSet(),
                hazards = hazards.toSet()
            )
        }
    }
}
