/*
 * Module: r2-shared-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared

import org.json.JSONArray
import org.json.JSONObject

interface JSONable {

    /**
     * Serializes the object to its JSON representation.
     */
    fun toJSON(): JSONObject

}

/**
 * Serializes a list of [JSONable] into a [JSONArray].
 */
fun List<JSONable>.toJSON(): JSONArray =
    JSONArray(map(JSONable::toJSON))
