/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.extensions

import android.os.Parcel
import kotlinx.android.parcel.Parceler
import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.JSONable
import timber.log.Timber

/**
 * Unwraps recursively the [JSONObject] to a [Map<String, Any>].
 */
fun JSONObject.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    for (key in keys()) {
        if (!isNull(key)) {
            map[key] = unwrapJSON(get(key))
        }
    }
    return map
}

/**
 * Unwraps recursively the [JSONArray] to a [List<Any>].
 */
fun JSONArray.toList(): List<Any> {
    val list = mutableListOf<Any>()
    for (i in 0 until length()) {
        list.add(unwrapJSON(get(i)))
    }
    return list
}

private fun unwrapJSON(value: Any) = when (value) {
    is JSONObject -> value.toMap()
    is JSONArray -> value.toList()
    else -> value
}

/**
 * Maps [name] to [jsonObject], clobbering any existing name/value mapping with the same name. If
 * the [JSONObject] is empty, any existing mapping for [name] is removed.
 */
fun JSONObject.putIfNotEmpty(name: String, jsonObject: JSONObject?) {
    if (jsonObject == null || jsonObject.length() == 0) {
        remove(name)
        return
    }

    put(name, jsonObject)
}

/**
 * Maps [name] to [jsonable] after converting it to a [JSONObject], clobbering any existing
 * name/value mapping with the same name. If the [JSONObject] argument is empty, any existing mapping
 * for [name] is removed.
 */
fun JSONObject.putIfNotEmpty(name: String, jsonable: JSONable?) {
    val json = jsonable?.toJSON()
    if (json == null || json.length() == 0) {
        remove(name)
        return
    }

    put(name, json)
}

/**
 * Maps [name] to [collection] after wrapping it in a [JSONArray], clobbering any existing
 * name/value mapping with the same name. If the collection is empty, any existing mapping
 * for [name] is removed.
 * If the objects in [collection] are [JSONable], then they are converted to [JSONObject] first.
 */
internal fun JSONObject.putIfNotEmpty(name: String, collection: Collection<*>) {
    @Suppress("NAME_SHADOWING")
    val collection = collection.mapNotNull {
        if (it !is JSONable) {
            return@mapNotNull it
        }

        val json = it.toJSON()
        if (json.length() == 0) {
            return@mapNotNull null
        }

        return@mapNotNull json
    }

    if (collection.isEmpty()) {
        remove(name)
        return
    }

    put(name, JSONArray(collection))
}

/**
 * Returns the value mapped by [name] if it exists and is a positive integer or can be coerced to a
 * positive integer, or [fallback] otherwise.
 * If [remove] is true, then the mapping will be removed from the [JSONObject].
 */
fun JSONObject.optPositiveInt(name: String, fallback: Int = -1, remove: Boolean = false): Int? {
    val int = optInt(name, fallback)
    val value = if (int >= 0) int else null
    if (remove) {
        this.remove(name)
    }
    return value
}

/**
 * Returns the value mapped by [name] if it exists and is a positive double or can be coerced to a
 * positive double, or [fallback] otherwise.
 * If [remove] is true, then the mapping will be removed from the [JSONObject].
 */
fun JSONObject.optPositiveDouble(name: String, fallback: Double = -1.0, remove: Boolean = false): Double? {
    val double = optDouble(name, fallback)
    val value = if (double >= 0) double else null
    if (remove) {
        this.remove(name)
    }
    return value
}

/**
 * Returns the value mapped by [name] if it exists, coercing it if necessary, or [null] if no such
 * mapping exists.
 * If [remove] is true, then the mapping will be removed from the [JSONObject].
 */
fun JSONObject.optNullableString(name: String, remove: Boolean = false): String? {
    val string = optString(name)
    val value = if (string != "") string else null
    if (remove) {
        this.remove(name)
    }
    return value
}

/**
 * Returns the value mapped by [name] if it exists, coercing it if necessary, or [null] if no such
 * mapping exists.
 * If [remove] is true, then the mapping will be removed from the [JSONObject].
 */
fun JSONObject.optNullableBoolean(name: String, remove: Boolean = false): Boolean? {
    if (!has(name)) {
        return null
    }
    val value = optBoolean(name)
    if (remove) {
        this.remove(name)
    }
    return value
}

/**
 * Returns the value mapped by [name] if it exists, coercing it if necessary, or [null] if no such
 * mapping exists.
 * If [remove] is true, then the mapping will be removed from the [JSONObject].
 */
fun JSONObject.optNullableInt(name: String, remove: Boolean = false): Int? {
    if (!has(name)) {
        return null
    }
    val value = optInt(name)
    if (remove) {
        this.remove(name)
    }
    return value
}

/**
 * Returns the value mapped by [name] if it exists, coercing it if necessary, or [null] if no such
 * mapping exists.
 * If [remove] is true, then the mapping will be removed from the [JSONObject].
 */
fun JSONObject.optNullableLong(name: String, remove: Boolean = false): Long? {
    if (!has(name)) {
        return null
    }
    val value = optLong(name)
    if (remove) {
        this.remove(name)
    }
    return value
}

/**
 * Returns the value mapped by [name] if it exists, coercing it if necessary, or [null] if no such
 * mapping exists.
 * If [remove] is true, then the mapping will be removed from the [JSONObject].
 */
fun JSONObject.optNullableDouble(name: String, remove: Boolean = false): Double? {
    if (!has(name)) {
        return null
    }
    val value = optDouble(name)
    if (remove) {
        this.remove(name)
    }
    return value
}

/**
 * Returns the value mapped by [name] if it exists and is either a [JSONArray] of [String] or a
 * single [String] value, or an empty list otherwise.
 * If [remove] is true, then the mapping will be removed from the [JSONObject].
 *
 * E.g. ["a", "b"] or "a"
 */
fun JSONObject.optStringsFromArrayOrSingle(name: String, remove: Boolean = false): List<String> {
    val value = if (remove) this.remove(name) else opt(name)

    return when (value) {
        is JSONArray -> value.toList().filterIsInstance(String::class.java)
        is String -> listOf(value)
        else -> emptyList()
    }
}

/**
 * Returns a list containing the results of applying the given transform function to each element
 * in the original [JSONObject].
 * If the tranform returns [null], it is not included in the output list.
 */
fun <T> JSONObject.mapNotNull(transform: (Pair<String, Any>) -> T?): List<T> {
    val result = mutableListOf<T>()
    for (key in keys()) {
        val transformedValue = transform(Pair(key, get(key)))
        if (transformedValue != null) {
            result.add(transformedValue)
        }
    }
    return result
}

/**
 * Returns a list containing the results of applying the given transform function to each element
 * in the original [JSONArray].
 * If the tranform returns [null], it is not included in the output list.
 */
fun <T> JSONArray.mapNotNull(transform: (Any) -> T?): List<T> {
    val result = mutableListOf<T>()
    for (i in 0 until length()) {
        val transformedValue = transform(get(i))
        if (transformedValue != null) {
            result.add(transformedValue)
        }
    }
    return result
}

/**
 * Parses a [JSONArray] of [JSONObject] into a [List] of models using the given [factory].
 */
internal fun <T> JSONArray?.parseObjects(factory: (Any) -> T?): List<T> {
    this ?: return emptyList()

    val models = mutableListOf<T>()
    for (i in 0 until length()) {
        val model = factory(get(i))
        if (model != null) {
            models.add(model)
        }
    }
    return models
}

/**
 * Implementation of a [Parceler] to be used with [@Parcelize] to serialize JSON objects.
 */
object JSONParceler : Parceler<Map<String, Any>> {

    override fun create(parcel: Parcel): Map<String, Any> =
        try {
            parcel.readString()?.let {
                JSONObject(it).toMap()
            } ?: emptyMap()
        } catch (e: Exception) {
            Timber.e(e, "Failed to read a JSON map from a Parcel")
            emptyMap()
        }

    override fun Map<String, Any>.write(parcel: Parcel, flags: Int) {
        try {
            parcel.writeString(JSONObject(this).toString())
        } catch (e: Exception) {
            Timber.e(e, "Failed to write a JSON map into a Parcel")
        }
    }

}
