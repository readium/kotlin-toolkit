/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer

import org.readium.r2.shared.asset.Asset
import org.readium.r2.shared.asset.AssetType
import org.readium.r2.shared.resource.ArchiveFactory
import org.readium.r2.shared.resource.ContainerFactory
import org.readium.r2.shared.resource.Resource
import org.readium.r2.shared.resource.ResourceFactory
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.mediatype.MediaType

class AssetFactory(
    private val archiveFactory: ArchiveFactory,
    private val resourceFactory: ResourceFactory,
    private val containerFactory: ContainerFactory
) {

    suspend fun createAsset(
        url: Url,
        mediaType: MediaType,
        type: AssetType
    ): Try<Asset, Exception> =
        when (type) {
            AssetType.Archive ->
                createAssetForPackagedPublication(url, mediaType)
            AssetType.Directory ->
                createAssetForExplodedPublication(url, mediaType)
            AssetType.File ->
                createAssetForContentFile(url, mediaType)
        }

    private suspend fun createAssetForPackagedPublication(
        url: Url,
        mediaType: MediaType
    ): Try<Asset.Container, Exception> {
        return resourceFactory.create(url)
            .flatMap { resource: Resource -> archiveFactory.create(resource, password = null) }
            .map { container -> Asset.Container(url.file, mediaType, container) }
    }

    private suspend fun createAssetForExplodedPublication(
        url: Url,
        mediaType: MediaType
    ): Try<Asset.Container, Exception> {
        return containerFactory.create(url)
            .map { container -> Asset.Container(url.file, mediaType, container) }
    }

    private suspend fun createAssetForContentFile(
        url: Url,
        mediaType: MediaType
    ): Try<Asset.Resource, Exception> {
        return resourceFactory.create(url)
            .map { resource -> Asset.Resource(url.file, mediaType, resource) }
    }
}
