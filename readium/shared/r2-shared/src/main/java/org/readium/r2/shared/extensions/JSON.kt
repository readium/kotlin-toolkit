/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.extensions

import org.json.JSONArray
import org.json.JSONObject

/**
 * Unpacks recursively the JSONObject to a MutableMap<String, Any>.
 */
internal fun JSONObject.toMutableMap(): MutableMap<String, Any> {
    val map = mutableMapOf<String, Any>()
    for (key in keys()) {
        map[key] = unpackJSONValue(get(key))
    }
    return map
}

/**
 * Unpacks recursively the JSONArray to a MutableList<Any>.
 */
internal fun JSONArray.toMutableList(): MutableList<Any> {
    val list = mutableListOf<Any>()
    for (i in 0 until length()) {
        list.add(unpackJSONValue(get(i)))
    }
    return list
}

private fun unpackJSONValue(value: Any) = when (value) {
    is JSONObject -> value.toMutableMap()
    is JSONArray -> value.toMutableList()
    else -> value
}
