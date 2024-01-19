/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.server

import android.content.Context
import org.nanohttpd.router.RouterNanoHTTPD

@Suppress("Unused_parameter")
@Deprecated(
    "The HTTP server is not needed anymore (see migration guide)",
    level = DeprecationLevel.ERROR
)
public class Server(
    port: Int,
    context: Context,
    enableReadiumNavigatorSupport: Boolean = true
)

@Suppress("Unused_parameter")
@Deprecated(
    "The HTTP server is not needed anymore (see migration guide)",
    level = DeprecationLevel.ERROR
)
public abstract class AbstractServer(
    port: Int,
    context: Context,
    enableReadiumNavigatorSupport: Boolean = true
) : RouterNanoHTTPD("127.0.0.1", port)
