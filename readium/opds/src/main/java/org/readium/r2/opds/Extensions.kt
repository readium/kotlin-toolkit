/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.opds

import kotlinx.coroutines.runBlocking
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.http.HttpFetchResponse
import org.readium.r2.shared.util.http.HttpRequest

internal fun HttpClient.fetchPromise(request: HttpRequest): Promise<HttpFetchResponse, Exception> {
    return task { runBlocking { fetch(request).getOrThrow() } }
}