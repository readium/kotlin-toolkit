/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.server

@Deprecated(
    message = "Moved to a new module readium-adapter-nanohttpd",
    replaceWith = ReplaceWith("org.readium.adapters.nanohttpd.Server")
)
typealias Server = org.readium.adapters.nanohttpd.Server

@Deprecated(
    message = "Moved to a new module readium-adapter-nanohttpd",
    replaceWith = ReplaceWith("org.readium.adapters.nanohttpd.AbstractServer")
)
typealias AbstractServer = org.readium.adapters.nanohttpd.AbstractServer
