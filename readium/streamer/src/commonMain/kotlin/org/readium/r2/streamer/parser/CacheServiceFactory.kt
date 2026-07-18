/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.streamer.parser

import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.CacheService

/**
 * Platform default for the `cacheServiceFactory` parameter of the parsers.
 *
 * On Android, this is an `InMemoryCacheService` without memory-pressure callbacks — the same
 * behavior as the pre-KMP parsers when given a null `Context`. Pass an Android `Context` through
 * the `androidMain` parser entry points to get an `InMemoryCacheService` releasing its content on
 * memory-pressure signals.
 *
 * On iOS, there is no default cache service.
 */
internal expect fun defaultCacheServiceFactory(): ((Publication.Service.Context) -> CacheService)?
