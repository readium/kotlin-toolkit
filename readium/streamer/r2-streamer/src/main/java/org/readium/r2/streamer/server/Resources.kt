/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.server

import org.readium.r2.shared.Injectable

class Resources {
    val resources: MutableMap<String, Any> = mutableMapOf()

    fun add(key: String, body: String, injectable: Injectable? = null) {
        injectable?.let {
            resources[key] = Pair(body, injectable.rawValue)
        } ?: run {
            resources[key] = body
        }
    }

    fun get(key: String): String? =
        when (val resource = resources[key]) {
            is Pair<*, *> -> resource.first as? String
            else -> resource as? String
        }

}