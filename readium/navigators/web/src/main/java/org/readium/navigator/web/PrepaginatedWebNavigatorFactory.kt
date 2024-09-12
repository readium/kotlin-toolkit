/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web

import android.app.Application
import org.readium.navigator.web.preferences.PrepaginatedWebNavigatorDefaults
import org.readium.navigator.web.preferences.PrepaginatedWebNavigatorPreferences
import org.readium.navigator.web.preferences.PrepaginatedWebNavigatorPreferencesEditor
import org.readium.navigator.web.util.WebViewServer
import org.readium.navigator.web.webapi.PrepaginatedDoubleApi
import org.readium.navigator.web.webapi.PrepaginatedSingleApi
import org.readium.r2.navigator.extensions.normalizeLocator
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.indexOfFirstWithHref
import org.readium.r2.shared.publication.presentation.page
import org.readium.r2.shared.util.Try

@ExperimentalReadiumApi
@OptIn(DelicateReadiumApi::class)
public class PrepaginatedWebNavigatorFactory private constructor(
    private val application: Application,
    private val publication: Publication,
    private val defaults: PrepaginatedWebNavigatorDefaults
) {

    public companion object {

        public operator fun invoke(
            application: Application,
            publication: Publication
        ): PrepaginatedWebNavigatorFactory? {
            if (!publication.conformsTo(Publication.Profile.EPUB)) {
                return null
            }

            if (publication.readingOrder.isEmpty()) {
                return null
            }

            return PrepaginatedWebNavigatorFactory(
                application,
                publication,
                PrepaginatedWebNavigatorDefaults()
            )
        }
    }

    public sealed class Error(
        override val message: String,
        override val cause: org.readium.r2.shared.util.Error?
    ) : org.readium.r2.shared.util.Error {

        public class UnsupportedPublication(
            cause: org.readium.r2.shared.util.Error? = null
        ) : Error("Publication is not supported.", cause)
    }

    public suspend fun createNavigator(
        initialLocator: Locator? = null,
        initialPreferences: PrepaginatedWebNavigatorPreferences? = null,
        readingOrder: List<Link> = publication.readingOrder
    ): Try<PrepaginatedWebNavigatorState, Nothing> {
        val items = readingOrder.map {
            PrepaginatedWebNavigatorState.ReadingOrder.Item(
                href = it.url(),
                page = it.properties.page
            )
        }

        val initialIndex = initialLocator
            ?.let { publication.normalizeLocator(it) }
            ?.let { readingOrder.indexOfFirstWithHref(it.href) }
            ?: 0

        val webViewServer =
            WebViewServer(
                application = application,
                publication = publication,
                servedAssets = listOf("readium/.*"),
                disableSelectionWhenProtected = false,
                onResourceLoadFailed = { _, _ -> }
            )

        val prepaginatedSingleContent = PrepaginatedSingleApi.getPageContent(
            assetManager = application.assets,
            assetsUrl = WebViewServer.assetUrl("readium/navigators/web")!!
        )

        val prepaginatedDoubleContent = PrepaginatedDoubleApi.getPageContent(
            assetManager = application.assets,
            assetsUrl = WebViewServer.assetUrl("readium/navigators/web")!!
        )

        val navigatorState =
            PrepaginatedWebNavigatorState(
                publicationMetadata = publication.metadata,
                readingOrder = PrepaginatedWebNavigatorState.ReadingOrder(items),
                initialPreferences = initialPreferences ?: PrepaginatedWebNavigatorPreferences(),
                defaults = defaults,
                initialItem = initialIndex,
                webViewServer = webViewServer,
                preloadedData = PrepaginatedWebNavigatorState.PreloadedData(
                    prepaginatedSingleContent = prepaginatedSingleContent,
                    prepaginatedDoubleContent = prepaginatedDoubleContent
                )
            )

        return Try.success(navigatorState)
    }

    public fun createPreferencesEditor(
        currentPreferences: PrepaginatedWebNavigatorPreferences
    ): PrepaginatedWebNavigatorPreferencesEditor =
        PrepaginatedWebNavigatorPreferencesEditor(
            currentPreferences,
            publication.metadata,
            defaults
        )
}
