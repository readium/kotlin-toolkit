/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.navigator.media.readaloud

import org.readium.navigator.media.readaloud.preferences.ReadAloudSettings
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.guided.GuidedNavigationAudioRef
import org.readium.r2.shared.util.TemporalFragmentParser

internal class ReadAloudNavigationHelper(
    var settings: ReadAloudSettings,
) {

    fun ReadAloudNode.firstMatchingLocation(location: ReadAloudLocation): ReadAloudNode? =
        firstDescendantOrNull { it.matchLocation(location) }

    private fun ReadAloudNode.matchLocation(location: ReadAloudLocation): Boolean =
        when (location) {
            is ReadAloudAudioLocation -> {
                refs.any { ref ->
                    val refFragment = ref.url.fragment?.let { TemporalFragmentParser.parse(it) }
                    ref.url.removeFragment() == location.href && refFragment?.start == location.timeOffset?.value
                }
            }
            is ReadAloudTextLocation -> {
                refs.any { ref ->
                    ref.url.removeFragment() == location.href &&
                        ref.url.fragment == location.cssSelector?.value?.removePrefix("#")
                }
            }
        }

    fun ReadAloudNode.isSkippable(): Boolean =
        nearestSkippable() != null

    fun ReadAloudNode.isEscapable(): Boolean =
        nearestEscapable() != null

    fun ReadAloudNode.firstContentNode(): ReadAloudNode? =
        firstDescendantOrNull { it.hasContent() }

    fun ReadAloudNode.hasContent() =
        text != null || refs.firstNotNullOfOrNull { it as? GuidedNavigationAudioRef } != null

    fun ReadAloudNode.nextContentNode(): ReadAloudNode? {
        val parent = parent
            ?: return children.firstOrNull()?.nextContentNode()

        val siblings = parent.children

        val currentIndex = siblings.indexOf(this)
        check(currentIndex != -1)

        val next = if (currentIndex < siblings.size - 1) {
            siblings[currentIndex + 1]
        } else {
            parent.next()
        }

        return next?.firstContentNode()
    }

    fun ReadAloudNode.escape(force: Boolean): ReadAloudNode? =
        (nearestEscapable() ?: this.takeIf { force })?.next()

    fun ReadAloudNode.skipToNext(force: Boolean): ReadAloudNode? =
        (nearestSkippable() ?: this.takeIf { force })?.next()

    fun ReadAloudNode.skipToPrevious(force: Boolean): ReadAloudNode? =
        (nearestSkippable() ?: this.takeIf { force })?.previous()

    private fun ReadAloudNode.nearestEscapable(): ReadAloudNode? =
        nearestOrNull { roles.any { it in settings.escapableRoles } }

    private fun ReadAloudNode.nearestSkippable(): ReadAloudNode? =
        nearestOrNull { roles.any { it in settings.skippableRoles } }

    private fun ReadAloudNode.nearestOrNull(
        predicate: (ReadAloudNode) -> Boolean,
    ): ReadAloudNode? =
        when {
            predicate(this) -> this
            parent == null -> null
            else -> parent!!.nearestOrNull(predicate)
        }

    private fun ReadAloudNode.firstDescendantOrNull(
        predicate: (ReadAloudNode) -> Boolean,
    ): ReadAloudNode? =
        when {
            predicate(this) -> this
            children.isEmpty() -> null
            else -> children.firstNotNullOfOrNull { it.firstDescendantOrNull(predicate) }
        }
}
