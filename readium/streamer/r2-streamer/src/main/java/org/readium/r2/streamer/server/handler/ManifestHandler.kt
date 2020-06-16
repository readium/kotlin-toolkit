/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mickaël Menu
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.server.handler

import android.net.Uri
import org.nanohttpd.protocols.http.response.Response
import org.nanohttpd.router.RouterNanoHTTPD
import org.readium.r2.shared.format.MediaType
import org.readium.r2.streamer.fetcher.Fetcher

internal class ManifestHandler : BaseHandler() {

    override fun handle(resource: RouterNanoHTTPD.UriResource, uri: Uri, parameters: Map<String, String>?): Response {
        val fetcher = resource.initParameter(Fetcher::class.java)
        return createResponse(mediaType = MediaType.READIUM_WEBPUB_MANIFEST, body = fetcher.publication.manifest.toByteArray())
    }

}
