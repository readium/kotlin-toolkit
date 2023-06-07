/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.asset

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
                createAssetForArchive(url, mediaType)
            AssetType.Directory ->
                createAssetForDirectory(url, mediaType)
            AssetType.Resource ->
                createAssetForResource(url, mediaType)
        }

    private suspend fun createAssetForArchive(
        url: Url,
        mediaType: MediaType
    ): Try<Asset.Container, Exception> {
        return resourceFactory.create(url)
            .flatMap { resource: Resource -> archiveFactory.create(resource, password = null) }
            .map { container -> Asset.Container(url.file, mediaType, AssetType.Archive, container) }
    }

    private suspend fun createAssetForDirectory(
        url: Url,
        mediaType: MediaType
    ): Try<Asset.Container, Exception> {
        return containerFactory.create(url)
            .map { container -> Asset.Container(url.file, mediaType, AssetType.Directory, container) }
    }

    private suspend fun createAssetForResource(
        url: Url,
        mediaType: MediaType
    ): Try<Asset.Resource, Exception> {
        return resourceFactory.create(url)
            .map { resource -> Asset.Resource(url.file, mediaType, resource) }
    }
}
