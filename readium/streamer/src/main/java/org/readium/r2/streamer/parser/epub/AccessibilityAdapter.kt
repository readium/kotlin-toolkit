package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.publication.Accessibility

internal class AccessibilityAdapter(private val items: List<MetadataItem>) {

    fun adapt(): Accessibility? {
        val (accessibilityProfiles, _) =
            items.metasWithProperty(Vocabularies.DCTERMS + "conformsTo")
                .map { it to accessibilityProfileFromString(it.value) }
                .partition { it.second != null }

        //val nonAccessibilityProfiles = otherProfiles.map { it.first }

        val conformsTo = accessibilityProfiles
            .mapNotNull { it.second }
            .toSet()

        val summary = items.firstWithProperty(Vocabularies.SCHEMA + "accessibilitySummary")
            ?.value

        val accessModes = items.metasWithProperty(Vocabularies.SCHEMA + "accessMode")
            .map { Accessibility.AccessMode(it.value) }
            .toSet()

        val accessModesSufficient = adaptAccessModeSufficient()

        val features = items.metasWithProperty(Vocabularies.SCHEMA + "accessibilityFeature")
            .map { Accessibility.Feature(it.value) }
            .toSet()

        val hazards = items.metasWithProperty(Vocabularies.SCHEMA + "accessibilityHazard")
            .map { Accessibility.Hazard(it.value) }
            .toSet()

        val certification = adaptCertification()

        return if (conformsTo.isNotEmpty() || certification != null || summary != null ||
            accessModes.isNotEmpty() || accessModesSufficient.isNotEmpty() ||
            features.isNotEmpty() || hazards.isNotEmpty()) {
            Accessibility(
                conformsTo = conformsTo,
                certification = certification,
                summary = summary,
                accessModes = accessModes,
                accessModesSufficient = accessModesSufficient,
                features = features,
                hazards = hazards
            )
        } else null
    }

    private fun adaptAccessModeSufficient(): Set<Set<Accessibility.PrimaryAccessMode>> =
        items.metasWithProperty(Vocabularies.SCHEMA + "accessModeSufficient")
            .map { it.value.split(",").map(String::trim).distinct() }
            .distinct()
            .mapNotNull { modeGroups -> modeGroups
                .mapNotNull { Accessibility.PrimaryAccessMode(it) }
                .toSet()
                .takeUnless(Set<Accessibility.PrimaryAccessMode>::isEmpty)
            }.toSet()

    private fun adaptCertification(): Accessibility.Certification? {
        var certification = items.firstWithProperty(Vocabularies.A11Y + "certifiedBy")
            ?.toCertification()
            ?: Accessibility.Certification(certifiedBy = null, credential = null, report = null)

        if (certification.credential == null) {
            val credential = items.firstWithProperty(Vocabularies.A11Y + "certifierCredential")
                ?.value
            credential?.let { certification = certification.copy(credential = it) }
        }

        if (certification.report == null) {
            val report = items.firstWithRel(Vocabularies.A11Y + "certifierReport")?.href
                ?: items.firstWithProperty(Vocabularies.A11Y + "certifierReport")?.value
            certification = certification.copy(report = report)
        }

        return certification
            .takeUnless { certification.certifiedBy == null && certification.credential == null &&
                certification.report == null }
    }

    private fun MetadataItem.Meta.toCertification(): Accessibility.Certification {
        require(property == Vocabularies.A11Y + "certifiedBy")

        val credential = children
            .firstWithProperty(Vocabularies.A11Y + "certifierCredential")?.value

        val report = children.firstWithRel(Vocabularies.A11Y + "certifierReport")?.href
            ?: children.firstWithProperty(Vocabularies.A11Y + "certifierReport")?.value

        return Accessibility.Certification(
            certifiedBy = value,
            credential = credential,
            report = report
        )
    }

    fun accessibilityProfileFromString(value: String): Accessibility.Profile? = when {
        isWCAG_20_A(value) -> Accessibility.Profile.WCAG_20_A
        isWCAG_20_AA(value) -> Accessibility.Profile.WCAG_20_AA
        isWCAG_20_AAA(value) -> Accessibility.Profile.WCAG_20_AAA
        else -> null
    }

    private fun isWCAG_20_A(value: String) = value in setOf(
        "EPUB Accessibility 1.1 - WCAG 2.0 Level A",
        "http://idpf.org/epub/a11y/accessibility-20170105.html#wcag-a",
        "http://www.idpf.org/epub/a11y/accessibility-20170105.html#wcag-a",
        "http://wwwidpf.org/epub/a11y/accessibility-20170105.html#wcag-a",
        "https://www.idpf.org/epub/a11y/accessibility-20170105.html#wcag-a",
    )

    private fun isWCAG_20_AA(value: String) = value in setOf(
        "EPUB Accessibility 1.1 - WCAG 2.0 Level AA",
        "http://idpf.org/epub/a11y/accessibility-20170105.html#wcag-aa",
        "http://www.idpf.org/epub/a11y/accessibility-20170105.html#wcag-aa",
        "http://wwwidpf.org/epub/a11y/accessibility-20170105.html#wcag-aa",
        "https://www.idpf.org/epub/a11y/accessibility-20170105.html#wcag-aa",
    )

    private fun isWCAG_20_AAA(value: String) = value in setOf(
        "EPUB Accessibility 1.1 - WCAG 2.0 Level AAA",
        "http://idpf.org/epub/a11y/accessibility-20170105.html#wcag-aaa",
        "http://www.idpf.org/epub/a11y/accessibility-20170105.html#wcag-aaa",
        "http://wwwidpf.org/epub/a11y/accessibility-20170105.html#wcag-aaa",
        "https://www.idpf.org/epub/a11y/accessibility-20170105.html#wcag-aaa",
    )
}
