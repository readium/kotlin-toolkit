/*
 * Module: r2-streamer-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.server.handler

import android.net.Uri
import org.nanohttpd.protocols.http.response.Response
import org.nanohttpd.router.RouterNanoHTTPD
import org.readium.r2.shared.format.Format
import org.readium.r2.shared.format.MediaType
import org.readium.r2.streamer.server.Resources


/**
 * Serves in-memory resources.
 *
 * The NanoHTTPD init parameter must be an instance of `Resources`.
 */
internal class ResourceHandler : BaseHandler() {

    override fun handle(resource: RouterNanoHTTPD.UriResource, uri: Uri, parameters: Map<String, String>?): Response {
        val resources = resource.initParameter(Resources::class.java)
        val href = uri.path?.substringAfterLast("/") ?: return notFoundResponse
        val body = resources.get(href) ?: return notFoundResponse
        val format = Format.of(fileExtension = href.substringAfterLast(".", ""))
        return createResponse(mediaType = format?.mediaType ?: MediaType.BINARY, body = body)
    }

}