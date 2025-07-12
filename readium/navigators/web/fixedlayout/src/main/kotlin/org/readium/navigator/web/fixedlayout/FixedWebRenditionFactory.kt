/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.fixedlayout

import android.app.Application
import java.io.IOException
import org.readium.navigator.web.fixedlayout.FixedWebPublication.ReadingOrder
import org.readium.navigator.web.fixedlayout.preferences.FixedWebDefaults
import org.readium.navigator.web.fixedlayout.preferences.FixedWebPreferences
import org.readium.navigator.web.fixedlayout.preferences.FixedWebPreferencesEditor
import org.readium.navigator.web.fixedlayout.preferences.FixedWebSettings
import org.readium.navigator.web.internals.server.WebViewServer
import org.readium.navigator.web.internals.webapi.FixedDoubleAreaApi
import org.readium.navigator.web.internals.webapi.FixedSingleAreaApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.page
import org.readium.r2.shared.publication.presentation.presentation
import org.readium.r2.shared.publication.services.isProtected
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.getOrElse

/**
 * Creates components to render a fixed layout Web publication.
 *
 * These components are meant to work together. Do not mix components from different
 * factory instances.
 */
@OptIn(InternalReadiumApi::class)
@ExperimentalReadiumApi
public class FixedWebRenditionFactory private constructor(
    private val application: Application,
    private val publication: Publication,
    private val configuration: FixedWebConfiguration,
) {

    public companion object {

        public operator fun invoke(
            application: Application,
            publication: Publication,
            configuration: FixedWebConfiguration,
        ): FixedWebRenditionFactory? {
            if (!publication.conformsTo(Publication.Profile.EPUB) ||
                publication.metadata.presentation.layout != EpubLayout.FIXED
            ) {
                return null
            }

            if (publication.readingOrder.isEmpty()) {
                return null
            }

            return FixedWebRenditionFactory(
                application,
                publication,
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
        initialSettings: FixedWebSettings,
        initialLocation: FixedWebGoLocation? = null,
        readingOrder: List<Link> = publication.readingOrder,
    ): Try<FixedWebRenditionState, Error> {
        // TODO: enable apps not to disable selection when publication is protected

        val readingOrderItems = readingOrder.map {
            FixedWebPublication.ReadingOrderItem(
                href = it.url(),
                page = it.properties.page,
                mediaType = it.mediaType
            )
        }

        val resourceItems = (publication.readingOrder - readingOrder + publication.resources).map {
            FixedWebPublication.OtherItem(
                href = it.url(),
                mediaType = it.mediaType
            )
        }

        val renditionPublication = FixedWebPublication(
            readingOrder = ReadingOrder(readingOrderItems),
            otherResources = resourceItems,
            container = publication.container
        )

        val preloads = preloadData()
            .getOrElse { return Try.failure(it) }

        val initialLocation = initialLocation
            ?: FixedWebGoLocation(readingOrderItems[0].href)

        val state =
            FixedWebRenditionState(
                application = application,
                publication = renditionPublication,
                initialSettings = initialSettings,
                initialLocation = initialLocation,
                configuration = configuration,
                preloadedData = preloads,
                disableSelection = publication.isProtected,
            )

        return Try.success(state)
    }

    private suspend fun preloadData(): Try<FixedWebPreloadedData, Error.Initialization> =
        try {
            val assetsUrl = WebViewServer.assetUrl("readium/navigator/web/internals")!!

            val prepaginatedSingleContent = FixedSingleAreaApi.Companion.getPageContent(
                assetManager = application.assets,
                assetsUrl = assetsUrl
            )

            val prepaginatedDoubleContent = FixedDoubleAreaApi.Companion.getPageContent(
                assetManager = application.assets,
                assetsUrl = assetsUrl
            )

            val preloadData = FixedWebPreloadedData(
                fixedSingleContent = prepaginatedSingleContent,
                fixedDoubleContent = prepaginatedDoubleContent
            )

            Try.success(preloadData)
        } catch (e: IOException) {
            Try.failure(Error.Initialization(ThrowableError(e)))
        }

    public fun createPreferencesEditor(
        initialPreferences: FixedWebPreferences,
        defaults: FixedWebDefaults = FixedWebDefaults(),
    ): FixedWebPreferencesEditor =
        FixedWebPreferencesEditor(
            initialPreferences,
            publication.metadata,
            defaults
        )
}
