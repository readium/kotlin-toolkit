/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web

import android.app.Application
import org.readium.navigator.web.layout.ReadingOrder
import org.readium.navigator.web.layout.ReadingOrderItem
import org.readium.navigator.web.location.ReflowableWebGoLocation
import org.readium.navigator.web.location.ReflowableWebLocatorAdapter
import org.readium.navigator.web.preferences.ReflowableWebDefaults
import org.readium.navigator.web.preferences.ReflowableWebPreferences
import org.readium.navigator.web.preferences.ReflowableWebPreferencesEditor
import org.readium.navigator.web.preferences.ReflowableWebSettings
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.page
import org.readium.r2.shared.publication.presentation.presentation
import org.readium.r2.shared.publication.services.ContentProtectionService
import org.readium.r2.shared.util.Try

@OptIn(InternalReadiumApi::class)
@ExperimentalReadiumApi
/**
 * Creates components to render a reflowable publication.
 *
 * These components are meant to work together. DO not mix components from different
 * factory instances.
 */
public class ReflowableWebRenditionFactory private constructor(
    private val application: Application,
    private val publication: Publication,
    private val defaults: ReflowableWebDefaults,
) {

    public companion object {

        public operator fun invoke(
            application: Application,
            publication: Publication,
            defaults: ReflowableWebDefaults = ReflowableWebDefaults(),
        ): ReflowableWebRenditionFactory? {
            if (!publication.conformsTo(Publication.Profile.EPUB) ||
                publication.metadata.presentation.layout != EpubLayout.REFLOWABLE
            ) {
                return null
            }

            if (publication.readingOrder.isEmpty()) {
                return null
            }

            return ReflowableWebRenditionFactory(
                application,
                publication,
                defaults
            )
        }
    }

    public sealed class Error(
        override val message: String,
        override val cause: org.readium.r2.shared.util.Error?,
    ) : org.readium.r2.shared.util.Error {

        public class Initialization(
            cause: org.readium.r2.shared.util.Error,
        ) : Error("Could not initialize the navigator.", cause)
    }

    public suspend fun createRenditionState(
        initialSettings: ReflowableWebSettings,
        initialLocation: ReflowableWebGoLocation? = null,
        readingOrder: List<Link> = publication.readingOrder,
    ): Try<ReflowableWebRenditionState, Error> {
        val readingOrderItems = readingOrder.map {
            ReadingOrderItem(
                href = it.url(),
                page = it.properties.page
            )
        }

        val resourceMediaTypes = (publication.readingOrder + publication.resources)
            .mapNotNull { link -> link.mediaType?.let { link.url() to it } }
            .associate { it }

        val state =
            ReflowableWebRenditionState(
                application = application,
                readingOrder = ReadingOrder(readingOrderItems),
                resourceMediaTypes = resourceMediaTypes,
                isRestricted = publication.findService(ContentProtectionService::class)?.isRestricted == true,
                initialSettings = initialSettings,
                initialLocation = initialLocation ?: ReflowableWebGoLocation(
                    readingOrderItems[0].href
                ),
                container = publication.container,
                fontFamilyDeclarations = emptyList()
            )

        return Try.success(state)
    }

    public fun createPreferencesEditor(
        initialPreferences: ReflowableWebPreferences,
    ): ReflowableWebPreferencesEditor =
        ReflowableWebPreferencesEditor(
            initialPreferences,
            publication.metadata,
            defaults
        )

    public fun createLocatorAdapter(): ReflowableWebLocatorAdapter =
        ReflowableWebLocatorAdapter(publication)
}
