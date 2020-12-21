/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer

import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.fetcher.Fetcher
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.PublicationAsset

internal fun Resource.readBlocking(range: LongRange? = null) = runBlocking { read(range) }

internal fun Fetcher.readBlocking(href: String) = runBlocking { get(Link(href = href)).use {  it.readBlocking() } }

internal fun Resource.lengthBlocking(range: LongRange? = null) = runBlocking { length() }

internal fun Fetcher.lengthBlocking(href: String) = runBlocking { get(Link(href = href)).use { it.lengthBlocking() } }

internal fun Resource.linkBlocking(range: LongRange? = null) = runBlocking { link() }

internal fun Fetcher.linkBlocking(href: String) = runBlocking { get(Link(href = href)).use { it.linkBlocking() } }

internal fun PublicationParser.parseBlocking(asset: PublicationAsset, fetcher: Fetcher):
        Publication.Builder? = runBlocking { parse(asset, fetcher) }
