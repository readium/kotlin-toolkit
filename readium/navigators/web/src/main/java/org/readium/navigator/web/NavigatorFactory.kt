/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web

import android.app.Application
import org.readium.navigator.web.preferences.NavigatorDefaults
import org.readium.navigator.web.preferences.NavigatorPreferences
import org.readium.navigator.web.preferences.NavigatorPreferencesEditor
import org.readium.navigator.web.util.WebViewServer
import org.readium.r2.navigator.extensions.normalizeLocator
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.indexOfFirstWithHref
import org.readium.r2.shared.util.Try

@ExperimentalReadiumApi
@OptIn(DelicateReadiumApi::class)
public class NavigatorFactory private constructor(
    private val application: Application,
    private val publication: Publication,
    private val defaults: NavigatorDefaults
) {

    public companion object {

        public operator fun invoke(
            application: Application,
            publication: Publication
        ): NavigatorFactory? {
            if (!publication.conformsTo(Publication.Profile.EPUB)) {
                return null
            }

            if (publication.readingOrder.isEmpty()) {
                return null
            }

            return NavigatorFactory(application, publication, NavigatorDefaults())
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
        initialPreferences: NavigatorPreferences? = null,
        readingOrder: List<Link> = publication.readingOrder
    ): Try<NavigatorState, Nothing> {
        val items = readingOrder.map {
            NavigatorState.ReadingOrder.Item(
                href = it.url()
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
                servedAssets = emptyList(),
                disableSelectionWhenProtected = false,
                onResourceLoadFailed = { _, _ -> }
            )

        val navigatorState =
            NavigatorState(
                publicationMetadata = publication.metadata,
                readingOrder = NavigatorState.ReadingOrder(items),
                initialPreferences = initialPreferences ?: NavigatorPreferences(),
                defaults = defaults,
                initialItem = initialIndex,
                webViewServer = webViewServer
            )

        return Try.success(navigatorState)
    }

    public fun createPreferencesEditor(
        currentPreferences: NavigatorPreferences
    ): NavigatorPreferencesEditor =
        NavigatorPreferencesEditor(
            currentPreferences,
            publication.metadata,
            defaults
        )
}
