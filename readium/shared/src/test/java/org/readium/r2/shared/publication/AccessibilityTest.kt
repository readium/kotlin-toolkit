/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication

import kotlin.test.assertEquals
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.assertJSONEquals
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccessibilityTest {

    @Test
    fun `null properties are not written`() {
        assertJSONEquals(
            Accessibility(
                conformsTo = emptySet(),
                certification = null,
                summary = null,
                accessModes = emptySet(),
                accessModesSufficient = emptySet(),
                features = emptySet(),
                hazards = emptySet()
            ).toJSON(),
            JSONObject("""{}""")
        )
    }

    @Test
    fun `invalid summary is just ignored`() {
        assertEquals(
            Accessibility(
                conformsTo = emptySet(),
                certification = null,
                summary = null,
                accessModes = emptySet(),
                accessModesSufficient = emptySet(),
                features = emptySet(),
                hazards = emptySet()
            ),
            Accessibility.fromJSON(
                JSONObject(
                    """{
                    "summary": ["sum1", "sum2"]
                }"""
                )
            )
        )
    }

    @Test
    fun `parse minimal JSON`() {
        assertEquals(
            Accessibility(
                conformsTo = emptySet(),
                certification = null,
                summary = null,
                accessModes = emptySet(),
                accessModesSufficient = emptySet(),
                hazards = emptySet(),
                features = emptySet()
            ),
            Accessibility.fromJSON(JSONObject("{}"))
        )
    }

    @Test
    fun `parse null JSON`() {
        Assert.assertNull(Accessibility.fromJSON(null))
    }

    @Test
    fun `parse full JSON`() {
        assertEquals(
            Accessibility(
                conformsTo = setOf(
                    Accessibility.Profile("https://profile1"),
                    Accessibility.Profile("https://profile2")
                ),
                certification = Accessibility.Certification(
                    certifiedBy = "company1",
                    credential = "credential1",
                    report = "https://report1"
                ),
                summary = "Summary",
                accessModes = setOf(
                    Accessibility.AccessMode.AUDITORY,
                    Accessibility.AccessMode.CHART_ON_VISUAL
                ),
                accessModesSufficient = setOf(
                    setOf(
                        Accessibility.PrimaryAccessMode.VISUAL,
                        Accessibility.PrimaryAccessMode.TACTILE
                    )
                ),
                features = setOf(
                    Accessibility.Feature.READING_ORDER,
                    Accessibility.Feature.ALTERNATIVE_TEXT
                ),
                hazards = setOf(
                    Accessibility.Hazard.FLASHING,
                    Accessibility.Hazard.MOTION_SIMULATION
                ),
                exemptions = setOf(
                    Accessibility.Exemption.EAA_DISPROPORTIONATE_BURDEN,
                    Accessibility.Exemption.EAA_MICROENTERPRISE
                )
            ),
            Accessibility.fromJSON(
                JSONObject(
                    """{
                    "conformsTo": ["https://profile1", "https://profile2"],
                    "certification": {
                        "certifiedBy": "company1",
                        "credential": "credential1",
                        "report": "https://report1"
                    },
                    "summary": "Summary",
                    "accessMode": ["auditory", "chartOnVisual"],
                    "accessModeSufficient": [["visual", "tactile"]],
                    "feature": ["readingOrder", "alternativeText"],
                    "hazard": ["flashing", "motionSimulation"],
                    "exemption": ["eaa-disproportionate-burden", "eaa-microenterprise"]
                }"""
                )
            )
        )
    }

    @Test
    fun `conformsTo can be a JSON literal`() {
        assertEquals(
            Accessibility(
                conformsTo = setOf(Accessibility.Profile.EPUB_A11Y_10_WCAG_20_A),
                certification = null,
                summary = null,
                accessModes = emptySet(),
                accessModesSufficient = emptySet(),
                features = emptySet(),
                hazards = emptySet()
            ),
            Accessibility.fromJSON(
                JSONObject(
                    """{
                    "conformsTo": "http://www.idpf.org/epub/a11y/accessibility-20170105.html#wcag-a"
                }"""
                )
            )
        )
    }

    @Test
    fun `conformsTo can be a JSON array`() {
        assertEquals(
            Accessibility(
                conformsTo = setOf(
                    Accessibility.Profile.EPUB_A11Y_10_WCAG_20_A,
                    Accessibility.Profile("https://profile2")
                ),
                certification = null,
                summary = null,
                accessModes = emptySet(),
                accessModesSufficient = emptySet(),
                features = emptySet(),
                hazards = emptySet()
            ),
            Accessibility.fromJSON(
                JSONObject(
                    """{
                    "conformsTo": ["http://www.idpf.org/epub/a11y/accessibility-20170105.html#wcag-a", "https://profile2"]
                 }"""
                )
            )
        )
    }

    fun `accessModes are correctly parsed`() {
        assertEquals(
            Accessibility(
                conformsTo = setOf(),
                certification = null,
                summary = null,
                accessModes = setOf(
                    Accessibility.AccessMode.AUDITORY,
                    Accessibility.AccessMode.CHART_ON_VISUAL,
                    Accessibility.AccessMode.CHEM_ON_VISUAL
                ),
                accessModesSufficient = emptySet(),
                features = emptySet(),
                hazards = emptySet()
            ),
            Accessibility.fromJSON(
                JSONObject(
                    """{
                    "accessMode": ["auditory", "chartOnVisual", "chemOnVisual"],
                }"""
                )
            )
        )
    }

    @Test
    fun `accessModeSufficient can contain both strings and arrays`() {
        assertEquals(
            Accessibility(
                conformsTo = setOf(),
                certification = null,
                summary = null,
                accessModes = emptySet(),
                accessModesSufficient = setOf(
                    setOf(Accessibility.PrimaryAccessMode.AUDITORY),
                    setOf(
                        Accessibility.PrimaryAccessMode.VISUAL,
                        Accessibility.PrimaryAccessMode.TACTILE
                    ),
                    setOf(Accessibility.PrimaryAccessMode.VISUAL)
                ),
                features = emptySet(),
                hazards = emptySet()
            ),
            Accessibility.fromJSON(
                JSONObject(
                    """{
                    "accessModeSufficient": ["auditory", ["visual", "tactile"], [], "visual"]
                }"""
                )
            )
        )
    }

    @Test
    fun `features are correctly parsed`() {
        assertEquals(
            Accessibility(
                conformsTo = setOf(),
                certification = null,
                summary = null,
                accessModes = emptySet(),
                accessModesSufficient = emptySet(),
                features = setOf(
                    Accessibility.Feature.INDEX,
                    Accessibility.Feature.ARIA,
                    Accessibility.Feature.ANNOTATIONS
                ),
                hazards = emptySet()
            ),
            Accessibility.fromJSON(
                JSONObject(
                    """{
                    "feature": ["index", "ARIA", "annotations"]
                }"""
                )
            )
        )
    }

    @Test
    fun `hazards are correctly parsed`() {
        assertEquals(
            Accessibility(
                conformsTo = setOf(),
                certification = null,
                summary = null,
                accessModes = emptySet(),
                accessModesSufficient = emptySet(),
                features = emptySet(),
                hazards = setOf(
                    Accessibility.Hazard.FLASHING,
                    Accessibility.Hazard.NO_SOUND_HAZARD,
                    Accessibility.Hazard.MOTION_SIMULATION
                ),
                exemptions = emptySet()
            ),
            Accessibility.fromJSON(
                JSONObject(
                    """{
                    "hazard": ["flashing", "noSoundHazard", "motionSimulation"]
                }"""
                )
            )
        )
    }

    @Test
    fun `exemptions are correctly parsed`() {
        assertEquals(
            Accessibility(
                conformsTo = setOf(),
                certification = null,
                summary = null,
                accessModes = emptySet(),
                accessModesSufficient = emptySet(),
                features = emptySet(),
                hazards = emptySet(),
                exemptions = setOf(
                    Accessibility.Exemption.EAA_DISPROPORTIONATE_BURDEN,
                    Accessibility.Exemption.EAA_FUNDAMENTAL_ALTERATION,
                    Accessibility.Exemption.EAA_MICROENTERPRISE,
                )
            ),
            Accessibility.fromJSON(
                JSONObject(
                    """{
                    "exemption": ["eaa-disproportionate-burden", "eaa-fundamental-alteration", "eaa-microenterprise"]
                }"""
                )
            )
        )
    }

    @Test
    fun `get full JSON`() {
        assertJSONEquals(
            JSONObject(
                """{
                "conformsTo": ["http://www.idpf.org/epub/a11y/accessibility-20170105.html#wcag-a", "https://profile2"],
                "certification": {
                    "certifiedBy": "company1",
                    "credential": "credential1",
                    "report": "https://report1"
                },
                "summary": "Summary",
                "accessMode": ["auditory", "chartOnVisual"],
                "accessModeSufficient": [["auditory"], ["visual", "tactile"], ["visual"]],
                "feature": ["readingOrder", "alternativeText"],
                "hazard": ["flashing", "motionSimulation"],
                "exemption": ["eaa-disproportionate-burden", "eaa-microenterprise"]
            }"""
            ),
            Accessibility(
                conformsTo = setOf(
                    Accessibility.Profile.EPUB_A11Y_10_WCAG_20_A,
                    Accessibility.Profile("https://profile2")
                ),
                certification = Accessibility.Certification(
                    certifiedBy = "company1",
                    credential = "credential1",
                    report = "https://report1"
                ),
                summary = "Summary",
                accessModes = setOf(
                    Accessibility.AccessMode.AUDITORY,
                    Accessibility.AccessMode.CHART_ON_VISUAL
                ),
                accessModesSufficient = setOf(
                    setOf(Accessibility.PrimaryAccessMode.AUDITORY),
                    setOf(
                        Accessibility.PrimaryAccessMode.VISUAL,
                        Accessibility.PrimaryAccessMode.TACTILE
                    ),
                    setOf(Accessibility.PrimaryAccessMode.VISUAL)
                ),
                features = setOf(
                    Accessibility.Feature.READING_ORDER,
                    Accessibility.Feature.ALTERNATIVE_TEXT
                ),
                hazards = setOf(
                    Accessibility.Hazard.FLASHING,
                    Accessibility.Hazard.MOTION_SIMULATION
                ),
                exemptions = setOf(
                    Accessibility.Exemption.EAA_DISPROPORTIONATE_BURDEN,
                    Accessibility.Exemption.EAA_MICROENTERPRISE
                )
            ).toJSON()
        )
    }
}
