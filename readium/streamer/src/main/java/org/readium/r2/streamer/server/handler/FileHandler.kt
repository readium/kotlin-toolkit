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
import org.readium.r2.streamer.server.Files

/**
 * Serves files from the local file system.
 *
 * The NanoHTTPD init parameter must be an instance of `Files`.
 */
internal class FileHandler : BaseHandler() {

    override fun handle(resource: RouterNanoHTTPD.UriResource, uri: Uri, parameters: Map<String, String>?): Response {
        val files = resource.initParameter(Files::class.java)
        val file = files.find(uri) ?: return notFoundResponse
        return createResponse(mediaType = file.mediaType, body = file.file.inputStream())
    }

}
