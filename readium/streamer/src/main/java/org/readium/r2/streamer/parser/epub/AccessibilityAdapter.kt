/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.publication.Accessibility

internal class AccessibilityAdapter {

    fun adapt(items: List<MetadataItem>): Pair<Accessibility?, List<MetadataItem>> {
        val itemsHolder = MetadataItemsHolder(items)

        val conformsTo = itemsHolder
            .adapt { list -> list.mapTakeNotNull { item -> conformedToProfileOrNull(item) } }
            .toSet()

        val summary = itemsHolder
            .adapt { it.takeFirstWithProperty(Vocabularies.SCHEMA + "accessibilitySummary") }
            ?.value

        val accessModes = itemsHolder
            .adapt { it.takeAllWithProperty(Vocabularies.SCHEMA + "accessMode") }
            .map { accessMode -> Accessibility.AccessMode(accessMode.value) }
            .toSet()

        val accessModesSufficient = itemsHolder
            .adapt(::adaptAccessModeSufficient)

        val features = itemsHolder
            .adapt { it.takeAllWithProperty(Vocabularies.SCHEMA + "accessibilityFeature") }
            .map { Accessibility.Feature(it.value) }
            .toSet()

        val hazards = itemsHolder
            .adapt { it.takeAllWithProperty(Vocabularies.SCHEMA + "accessibilityHazard") }
            .map { Accessibility.Hazard(it.value) }
            .toSet()

        val certification = itemsHolder
            .adapt(::adaptCertification)

        return if (itemsHolder.remainingItems.size == items.size) {
            null to itemsHolder.remainingItems
        } else {
            val accessibility = Accessibility(
                conformsTo = conformsTo,
                certification = certification,
                summary = summary,
                accessModes = accessModes,
                accessModesSufficient = accessModesSufficient,
                features = features,
                hazards = hazards
            )
            accessibility to itemsHolder.remainingItems
        }
    }

    private fun conformedToProfileOrNull(item: MetadataItem): Accessibility.Profile? =
        if (item is MetadataItem.Meta && item.property == Vocabularies.DCTERMS + "conformsTo") {
            accessibilityProfileFromString(item.value)
        } else if (item is MetadataItem.Link && item.rels.contains(
                Vocabularies.DCTERMS + "conformsTo"
            )
        ) {
            accessibilityProfileFromString(item.href.toString())
        } else {
            null
        }

    private fun adaptAccessModeSufficient(items: List<MetadataItem>): Pair<Set<Set<Accessibility.PrimaryAccessMode>>, List<MetadataItem>> = items
        .takeAllWithProperty(Vocabularies.SCHEMA + "accessModeSufficient")
        .mapFirst { metas ->
            metas.map { it.value.split(",").map(String::trim).distinct() }
                .distinct()
                .mapNotNull { modeGroups ->
                    modeGroups
                        .mapNotNull { Accessibility.PrimaryAccessMode(it) }
                        .toSet()
                        .takeUnless(Set<Accessibility.PrimaryAccessMode>::isEmpty)
                }.toSet()
        }

    private fun adaptCertification(items: List<MetadataItem>): Pair<Accessibility.Certification?, List<MetadataItem>> {
        var remainingItems = items

        var certification = remainingItems
            .takeFirstWithProperty(Vocabularies.A11Y + "certifiedBy")
            .let {
                remainingItems = it.second
                it.first
            }
            ?.toCertification()
            ?: Accessibility.Certification(certifiedBy = null, credential = null, report = null)

        if (certification.credential == null) {
            remainingItems.takeFirstWithProperty(Vocabularies.A11Y + "certifierCredential")
                .let {
                    remainingItems = it.second
                    it.first
                }
                ?.let { certification = certification.copy(credential = it.value) }
        }

        if (certification.report == null) {
            remainingItems
                .takeFirstWithProperty(Vocabularies.A11Y + "certifierReport")
                .let {
                    remainingItems = it.second
                    it.first
                }
                ?.let { certification = certification.copy(report = it.value) }
        }

        if (certification.report == null) {
            remainingItems
                .takeFirstWithRel(Vocabularies.A11Y + "certifierReport")
                .let {
                    remainingItems = it.second
                    it.first
                }
                ?.let { certification = certification.copy(report = it.href.toString()) }
        }

        return if (remainingItems.size == items.size) {
            null to remainingItems
        } else {
            certification to remainingItems
        }
    }

    private fun MetadataItem.Meta.toCertification(): Accessibility.Certification {
        require(property == Vocabularies.A11Y + "certifiedBy")

        val credential = children
            .firstWithProperty(Vocabularies.A11Y + "certifierCredential")?.value

        val report = children
            .firstWithRel(Vocabularies.A11Y + "certifierReport")?.href

        return Accessibility.Certification(
            certifiedBy = value,
            credential = credential,
            report = report?.toString()
        )
    }

    private fun accessibilityProfileFromString(value: String): Accessibility.Profile? = when {
        isWCAG_20_A(value) -> Accessibility.Profile.EPUB_A11Y_10_WCAG_20_A
        isWCAG_20_AA(value) -> Accessibility.Profile.EPUB_A11Y_10_WCAG_20_AA
        isWCAG_20_AAA(value) -> Accessibility.Profile.EPUB_A11Y_10_WCAG_20_AAA
        else -> null
    }

    private fun isWCAG_20_A(value: String) = value in setOf(
        "EPUB Accessibility 1.1 - WCAG 2.0 Level A",
        "http://idpf.org/epub/a11y/accessibility-20170105.html#wcag-a",
        "http://www.idpf.org/epub/a11y/accessibility-20170105.html#wcag-a",
        "https://idpf.org/epub/a11y/accessibility-20170105.html#wcag-a",
        "https://www.idpf.org/epub/a11y/accessibility-20170105.html#wcag-a"
    )

    private fun isWCAG_20_AA(value: String) = value in setOf(
        "EPUB Accessibility 1.1 - WCAG 2.0 Level AA",
        "http://idpf.org/epub/a11y/accessibility-20170105.html#wcag-aa",
        "http://www.idpf.org/epub/a11y/accessibility-20170105.html#wcag-aa",
        "https://idpf.org/epub/a11y/accessibility-20170105.html#wcag-aa",
        "https://www.idpf.org/epub/a11y/accessibility-20170105.html#wcag-aa"
    )

    private fun isWCAG_20_AAA(value: String) = value in setOf(
        "EPUB Accessibility 1.1 - WCAG 2.0 Level AAA",
        "http://idpf.org/epub/a11y/accessibility-20170105.html#wcag-aaa",
        "http://www.idpf.org/epub/a11y/accessibility-20170105.html#wcag-aaa",
        "https://idpf.org/epub/a11y/accessibility-20170105.html#wcag-aaa",
        "https://www.idpf.org/epub/a11y/accessibility-20170105.html#wcag-aaa"
    )
}
