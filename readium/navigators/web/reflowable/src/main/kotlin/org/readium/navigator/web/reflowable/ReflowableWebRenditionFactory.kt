/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.reflowable

import android.app.Application
import org.readium.navigator.web.reflowable.preferences.ReflowableWebDefaults
import org.readium.navigator.web.reflowable.preferences.ReflowableWebPreferences
import org.readium.navigator.web.reflowable.preferences.ReflowableWebPreferencesEditor
import org.readium.navigator.web.reflowable.preferences.ReflowableWebSettings
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Layout
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.PositionsService
import org.readium.r2.shared.publication.services.isProtected
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.util.Try

/**
 * Creates components to render a reflowable Web publication.
 *
 * These components are meant to work together. Do not mix components from different
 * factory instances.
 */
@ExperimentalReadiumApi
@OptIn(InternalReadiumApi::class)
public class ReflowableWebRenditionFactory private constructor(
    private val application: Application,
    private val publication: Publication,
    private val positionsService: PositionsService,
    private val configuration: ReflowableWebConfiguration,
) {

    public companion object {

        public operator fun invoke(
            application: Application,
            publication: Publication,
            configuration: ReflowableWebConfiguration = ReflowableWebConfiguration(),
        ): ReflowableWebRenditionFactory? {
            if (!publication.conformsTo(Publication.Profile.EPUB) ||
                publication.metadata.layout != null && publication.metadata.layout != Layout.REFLOWABLE
            ) {
                return null
            }

            if (publication.readingOrder.isEmpty()) {
                return null
            }

            if (publication.isRestricted) {
                return null
            }

            val positionsService = publication.findService(PositionsService::class)
                ?: return null

            return ReflowableWebRenditionFactory(
                application,
                publication,
                positionsService,
                configuration
            )
        }
    }

    public sealed class Error(
        override val message: String,
        override val cause: org.readium.r2.shared.util.Error?,
    ) : org.readium.r2.shared.util.Error {

        public class Initialization(
            cause: org.readium.r2.shared.util.Error,
        ) : Error("Could not create a rendition state.", cause)
    }

    public suspend fun createRenditionState(
        initialSettings: ReflowableWebSettings,
        initialLocation: ReflowableWebGoLocation? = null,
        readingOrder: List<Link> = publication.readingOrder,
        positionsService: PositionsService = this.positionsService,
    ): Try<ReflowableWebRenditionState, Error> {
        // TODO: enable apps not to disable selection when publication is protected

        val readingOrderItems = readingOrder.map { link ->
            ReflowableWebPublication.Item(
                href = link.url(),
                mediaType = link.mediaType,
            )
        }

        val positionNumbers = positionsService.positionsByReadingOrder()
            .map { it.size }

        val resourceItems = (publication.readingOrder - readingOrder + publication.resources).map {
            ReflowableWebPublication.Item(
                href = it.url(),
                mediaType = it.mediaType
            )
        }

        val renditionPublication = ReflowableWebPublication(
            readingOrder = ReflowableWebPublication.ReadingOrder(readingOrderItems, positionNumbers),
            otherResources = resourceItems,
            container = publication.container
        )

        val initialLocation = initialLocation
            ?: ReflowableWebGoLocation(readingOrderItems[0].href)

        val state =
            ReflowableWebRenditionState(
                application = application,
                publication = renditionPublication,
                initialSettings = initialSettings,
                initialLocation = initialLocation,
                configuration = configuration,
                disableSelection = publication.isProtected
            )

        return Try.success(state)
    }

    public fun createPreferencesEditor(
        initialPreferences: ReflowableWebPreferences,
        defaults: ReflowableWebDefaults = ReflowableWebDefaults(),
    ): ReflowableWebPreferencesEditor =
        ReflowableWebPreferencesEditor(
            initialPreferences,
            publication.metadata,
            defaults
        )
}
