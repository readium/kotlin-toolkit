/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web

import android.app.Application
import java.io.IOException
import org.readium.navigator.web.layout.ReadingOrder
import org.readium.navigator.web.layout.ReadingOrderItem
import org.readium.navigator.web.location.FixedWebGoLocation
import org.readium.navigator.web.location.FixedWebLocatorAdapter
import org.readium.navigator.web.location.HrefLocation
import org.readium.navigator.web.preferences.FixedWebDefaults
import org.readium.navigator.web.preferences.FixedWebPreferences
import org.readium.navigator.web.preferences.FixedWebPreferencesEditor
import org.readium.navigator.web.util.WebViewServer
import org.readium.navigator.web.webapi.FixedDoubleApi
import org.readium.navigator.web.webapi.FixedSingleApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.page
import org.readium.r2.shared.publication.presentation.presentation
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.getOrElse

@ExperimentalReadiumApi
public class FixedWebNavigatorFactory private constructor(
    private val application: Application,
    private val publication: Publication,
    private val defaults: FixedWebDefaults
) {

    public companion object {

        public operator fun invoke(
            application: Application,
            publication: Publication
        ): FixedWebNavigatorFactory? {
            if (!publication.conformsTo(Publication.Profile.EPUB) ||
                publication.metadata.presentation.layout != EpubLayout.FIXED
            ) {
                return null
            }

            if (publication.readingOrder.isEmpty()) {
                return null
            }

            return FixedWebNavigatorFactory(
                application,
                publication,
                FixedWebDefaults()
            )
        }
    }

    public sealed class Error(
        override val message: String,
        override val cause: org.readium.r2.shared.util.Error?
    ) : org.readium.r2.shared.util.Error {

        public class Initialization(
            cause: org.readium.r2.shared.util.Error
        ) : Error("Could not initialize the navigator.", cause)
    }

    public suspend fun createRenditionState(
        initialLocation: FixedWebGoLocation? = null,
        initialPreferences: FixedWebPreferences? = null,
        readingOrder: List<Link> = publication.readingOrder
    ): Try<FixedWebRenditionState, Error> {
        val items = readingOrder.map {
            ReadingOrderItem(
                href = it.url(),
                page = it.properties.page
            )
        }

        val webViewServer =
            WebViewServer(
                application = application,
                publication = publication,
                servedAssets = listOf("readium/.*"),
                disableSelectionWhenProtected = false,
                onResourceLoadFailed = { _, _ -> }
            )

        val preloads = preloadData()
            .getOrElse { return Try.failure(it) }

        val state =
            FixedWebRenditionState(
                publicationMetadata = publication.metadata,
                readingOrder = ReadingOrder(items),
                initialPreferences = initialPreferences ?: FixedWebPreferences(),
                defaults = defaults,
                initialLocation = initialLocation ?: HrefLocation(items[0].href),
                webViewServer = webViewServer,
                preloadedData = preloads
            )

        return Try.success(state)
    }

    private suspend fun preloadData(): Try<FixedWebPreloadedData, Error.Initialization> =
        try {
            val assetsUrl = WebViewServer.assetUrl("readium/navigators/web")!!

            val prepaginatedSingleContent = FixedSingleApi.getPageContent(
                assetManager = application.assets,
                assetsUrl = assetsUrl
            )

            val prepaginatedDoubleContent = FixedDoubleApi.getPageContent(
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
        currentPreferences: FixedWebPreferences
    ): FixedWebPreferencesEditor =
        FixedWebPreferencesEditor(
            currentPreferences,
            publication.metadata,
            defaults
        )

    public fun createLocatorAdapter(): FixedWebLocatorAdapter =
        FixedWebLocatorAdapter(publication)
}