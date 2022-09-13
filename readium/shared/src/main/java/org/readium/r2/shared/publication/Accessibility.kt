/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.*
import org.readium.r2.shared.extensions.putIfNotEmpty
import org.readium.r2.shared.publication.Accessibility.AccessMode.Companion.toJSONArray
import org.readium.r2.shared.publication.Accessibility.Feature.Companion.toJSONArray
import org.readium.r2.shared.publication.Accessibility.Hazard.Companion.toJSONArray
import org.readium.r2.shared.publication.Accessibility.Profile.Companion.toJSONArray
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.util.logging.log

@Parcelize
data class Accessibility(
    val conformsTo: Set<Profile>,
    val certification: Certification? = null,
    val summary: String? = null,
    val accessModes: Set<AccessMode>,
    val accessModesSufficient: Set<Set<AccessMode>>,
    val features: Set<Feature>,
    val hazards: Set<Hazard>
) : JSONable, Parcelable {

    @Parcelize
    @JvmInline
    value class Profile(val uri: String) : Parcelable {

        companion object {

            val WCAG_20_A = Profile("http://www.idpf.org/epub/a11y/accessibility-20170105.html#wcag-a")

            val WCAG_20_AA = Profile("http://www.idpf.org/epub/a11y/accessibility-20170105.html#wcag-aa")

            val WCAG_20_AAA = Profile("http://www.idpf.org/epub/a11y/accessibility-20170105.html#wcag-aaa")

            fun Set<Profile>.toJSONArray(): JSONArray =
                JSONArray(this.map(Profile::uri))
        }
    }

    @Parcelize
    data class Certification(
        val certifiedBy: String?,
        val credential: String?,
        val report: String?
    ) : JSONable, Parcelable {

        override fun toJSON(): JSONObject = JSONObject().apply {
            put("certifiedBy", certifiedBy)
            put("credential", credential)
            put("report", report)
        }

        companion object {

            /**
             * Parses a [Certification] from its RWPM JSON representation.
             *
             * If the certification can't be parsed, a warning will be logged with [warnings].
             */
            fun fromJSON(
                json: JSONObject?,
                warnings: WarningLogger? = null
            ): Certification? {
                json ?: return null
                val certifiedBy = json.optNullableString("certifiedBy")
                val credential = json.optNullableString("credential")
                val report = json.optNullableString("report")

                if (listOfNotNull(certifiedBy, credential, report).isEmpty()) {
                    warnings?.log(Certification::class.java, "no valid property in certification object", json)
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

    @Parcelize
    @JvmInline
    value class AccessMode(val value: String) : Parcelable {

        companion object  {
            val AUDITORY = AccessMode("auditory")
            val CHART_ON_VISUAL =  AccessMode("chartOnVisual")
            val CHEM_ON_VISUAL = AccessMode("chemOnVisual")
            val COLOR_DEPENDENT = AccessMode("colorDependent")
            val DIAGRAM_ON_VISUAL = AccessMode("diagramOnVisual")
            val MATH_ON_VISUAL = AccessMode("mathOnVisual")
            val MUSIC_ON_VISUAL = AccessMode("musicOnVisual")
            val TACTILE = AccessMode("tactile")
            val TEXT_ON_VISUAL = AccessMode("textOnVisual")
            val TEXTUAL = AccessMode("textual")
            val VISUAL = AccessMode("visual")

            /**
             * Creates a list of [Hazard] from its RWPM JSON representation.
             */
            fun fromJSONArray(json: JSONArray?): List<AccessMode> =
                json?.filterIsInstance(String::class.java)
                    ?.map { AccessMode(it) }
                    .orEmpty()

            fun Set<AccessMode>.toJSONArray(): JSONArray =
                JSONArray(this.map(AccessMode::value))
        }
    }

    @Parcelize
    @JvmInline
    value class Feature(val value: String) : Parcelable {

        companion object {
            val ANNOTATIONS = Feature("annotations")
            val ARIA = Feature("ARIA")
            val BOOKMARKS = Feature("bookmark")
            val INDEX = Feature("index")
            val PRINT_PAGE_NUMBERS = Feature("printPageNumbers")
            val READING_ORDER = Feature("readingOrder")
            val STRUCTURAL_NAVIGATION = Feature("structuralNavigation")
            val TABLE_OF_CONTENTS = Feature("tableOfContents")
            val TAGGED_PDF = Feature("taggedPDF")
            val ALTERNATIVE_TEXT = Feature("alternativeText")
            val AUDIO_DESCRIPTION = Feature("audioDescription")
            val CAPTIONS = Feature("captions")
            val DESCRIBED_MATH = Feature("describeMath")
            val LONG_DESCRIPTION = Feature("longDescription")
            val RUBY_ANNOTATIONS = Feature("rubyAnnotations")
            val SIGN_LANGUAGE = Feature("signLanguage")
            val TRANSCRIPT = Feature("transcript")
            val DISPLAY_TRANSFORMABILITY = Feature("displayTransformability")
            val SYNCHRONIZED_AUDIO_TEXT = Feature("synchronizedAudioText")
            val TIMING_CONTROL = Feature("timingControl")
            val UNLOCKED = Feature("unlocked")
            val CHEM_ML = Feature("ChemML")
            val LATEX = Feature("latex")
            val MATH_ML = Feature("MathML")
            val TTS_MARKUP = Feature("ttsMarkup")
            val HIGH_CONTRAST_AUDIO = Feature("highContrastAudio")
            val HIGH_CONTRAST_DISPLAY = Feature("highContrastDisplay")
            val LARGE_PRINT = Feature("largePrint")
            val BRAILLE = Feature("braille")
            val TACTILE_GRAPHIC = Feature("tactileGraphic")
            val TACTILE_OBJECT = Feature("tactileObject")
            val NONE = Feature("none")

            /**
             * Creates a list of [Hazard] from its RWPM JSON representation.
             */
            fun fromJSONArray(json: JSONArray?): List<Feature> =
                json?.filterIsInstance(String::class.java)
                    ?.map { Feature(it) }
                    .orEmpty()

            fun Set<Feature>.toJSONArray(): JSONArray =
                JSONArray(this.map(Feature::value))
        }
    }

    @Parcelize
    @JvmInline
    value class Hazard(val value: String) : Parcelable {

        companion object {
            val FLASHING = Hazard("flashing")
            val NO_FLASHING_HAZARD = Hazard("noFlashingHazard")
            val MOTION_SIMULATION = Hazard("motionSimulation")
            val NO_MOTION_SIMULATION_HAZARD = Hazard("noMotionSimulationHazard")
            val SOUND = Hazard("sound")
            val NO_SOUND_HAZARD = Hazard("noSoundHazard")
            val UNKNOWN = Hazard("unknown")
            val NONE = Hazard("none")

            /**
             * Creates a list of [Hazard] from its RWPM JSON representation.
             */
            fun fromJSONArray(json: JSONArray?): List<Hazard> =
                json?.filterIsInstance(String::class.java)
                    ?.map { Hazard(it) }
                    .orEmpty()

            fun Set<Hazard>.toJSONArray(): JSONArray =
                JSONArray(this.map(Hazard::value))
        }
    }

    override fun toJSON(): JSONObject =  JSONObject().apply {
        putIfNotEmpty("conformsTo", conformsTo.toJSONArray())
        put("certification", certification?.toJSON())
        put("summary", summary)
        putIfNotEmpty("accessMode", accessModes.toJSONArray())
        putIfNotEmpty("accessModeSufficient", accessModesSufficient.map { it.toJSONArray() })
        putIfNotEmpty("hazard", hazards.toJSONArray())
        putIfNotEmpty("feature", features.toJSONArray())
    }

    companion object {

        /**
         * Parses a [Accessibility] from its RWPM JSON representation.
         *
         * If the accessibility metadata can't be parsed, a warning will be logged with [warnings].
         */
        fun fromJSON(json: Any?): Accessibility? {
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
                        is JSONArray -> AccessMode.fromJSONArray(it).toSet()
                            .takeUnless(Set<AccessMode>::isEmpty)
                        is String -> setOf(AccessMode(it))
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
