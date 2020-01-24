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
import org.readium.r2.shared.JSONable

/**
 * Unwraps recursively the [JSONObject] to a [MutableMap<String, Any>].
 */
fun JSONObject.toMutableMap(): MutableMap<String, Any> {
    val map = mutableMapOf<String, Any>()
    for (key in keys()) {
        map[key] = unwrapJSON(get(key))
    }
    return map
}

/**
 * Unwraps recursively the [JSONArray] to a [MutableList<Any>].
 */
fun JSONArray.toMutableList(): MutableList<Any> {
    val list = mutableListOf<Any>()
    for (i in 0 until length()) {
        list.add(unwrapJSON(get(i)))
    }
    return list
}

private fun unwrapJSON(value: Any) = when (value) {
    is JSONObject -> value.toMutableMap()
    is JSONArray -> value.toMutableList()
    else -> value
}

/**
 * Maps [name] to [collection] after wrapping it in a [JSONArray], clobbering any existing
 * name/value mapping with the same name. If the collection is empty, any existing mapping
 * for [name] is removed.
 */
internal fun JSONObject.putIfNotEmpty(name: String, collection: Collection<*>) {
    if (collection.isEmpty()) {
        remove(name)
        return
    }

    put(name, JSONArray(collection))
}

/**
 * Maps [name] to [jsonable] after converting it to a [JSONObject], clobbering any existing
 * name/value mapping with the same name. If the [JSONObject] is empty, any existing mapping
 * for [name] is removed.
 */
fun JSONObject.putIfNotEmpty(name: String, jsonable: JSONable) {
    val json = jsonable.toJSON()
    if (json.length() == 0) {
        remove(name)
        return
    }

    put(name, json)
}

/**
 * Returns the value mapped by [name] if it exists and is a positive integer or can be coerced to a
 * positive integer, or [fallback] otherwise.
 */
fun JSONObject.optPositiveInt(name: String, fallback: Int = -1): Int? {
    val int = optInt(name, fallback)
    return if (int > 0) int else null
}

/**
 * Returns the value mapped by [name] if it exists and is a positive double or can be coerced to a
 * positive double, or [fallback] otherwise.
 */
fun JSONObject.optPositiveDouble(name: String, fallback: Double = -1.0): Double? {
    val double = optDouble(name, fallback)
    return if (double > 0) double else null
}

/**
 * Returns the value mapped by [name] if it exists, coercing it if necessary, or [null] if no such
 * mapping exists.
 */
fun JSONObject.optNullableString(name: String): String? {
    val string = optString(name)
    return if (string != "") string else null
}

/**
 * Returns the value mapped by [name] if it exists and is either a [JSONArray] of [String] or a
 * single [String] value, or an empty list otherwise.
 *
 * E.g. ["a", "b"] or "a"
 */
fun JSONObject.optStringsFromArrayOrSingle(name: String): List<String> {
    val array = optJSONArray(name)
    if (array != null) {
        return array.toMutableList().filterIsInstance(String::class.java)
    }

    val string = optNullableString(name)
    if (string != null) {
        return listOf(string)
    }

    return emptyList()
}
