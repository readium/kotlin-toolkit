/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser.audio

import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.firstWithHref
import org.readium.r2.shared.publication.services.LocatorService
import org.readium.r2.shared.util.mediatype.MediaType

/** Locator service for audio publications. */
class AudioLocatorService(private val readingOrder: List<Link>) : LocatorService {

    /** Duration per reading order index. */
    private val durations: List<Double> =
        readingOrder.map { it.duration ?: 0.0 }

    /** Total duration of the publication. */
    private val totalDuration: Double? =
        durations.sum().takeIf { it > 0 }

    override suspend fun locate(locator: Locator): Locator? {
        if (readingOrder.firstWithHref(locator.href) != null) {
            return locator
        }

        val target = locator.locations.totalProgression?.let { locateProgression(it) }
        if (target != null) {
            return target.copy(
                title = locator.title,
                text = locator.text
            )
        }

        return null
    }

    override suspend fun locateProgression(totalProgression: Double): Locator? {
        totalDuration ?: return null
        val positionInPublication = totalProgression * totalDuration

        val (link, resourcePosition) = readingOrderItemAtPosition(positionInPublication)
            ?: return null

        val positionInResource = positionInPublication - resourcePosition

        return Locator(
            href = link.href,
            type = link.type ?: MediaType.BINARY.toString(),
            locations = Locator.Locations(
                fragments = listOf("t=${positionInResource.toInt()}"),
                progression = link.duration?.let { duration ->
                    if (duration == 0.0) 0.0
                    else positionInResource / duration
                },
                totalProgression = totalProgression
            )
        )
    }

    /**
     * Finds the reading order item containing the time [position] (in seconds), as well as its
     * start time.
     */
    private fun readingOrderItemAtPosition(position: Double): Pair<Link, Double>? {
        var current = 0.0
        for ((i, _) in durations.withIndex()) {
            val link = readingOrder[i]
            val itemDuration = link.duration ?: 0.0
            if (position >= current && position < (current + itemDuration)) {
                return Pair(link, current)
            }

            current += itemDuration
        }

        if (position == totalDuration) {
            return readingOrder.lastOrNull()?.let { Pair(it, current - (it.duration ?: 0.0)) }
        }

        return null
    }

    companion object {

        fun createFactory(): (Publication.Service.Context) -> AudioLocatorService = {
            AudioLocatorService(readingOrder = it.manifest.readingOrder)
        }

    }

}
