package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.publication.Accessibility

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
