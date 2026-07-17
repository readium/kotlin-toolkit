/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.json

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.tryOrNull

/**
 * Lenient [Json] configuration used to parse JSON documents, mirroring the leniency of the
 * legacy `org.json` parser.
 */
@InternalReadiumApi
public val LenientJson: Json = Json {
    isLenient = true
    allowTrailingComma = true
    allowComments = true
}

/**
 * Parses a [JsonElement] from its string representation, or returns null if it is not valid JSON.
 */
@InternalReadiumApi
public fun String.toJsonElementOrNull(): JsonElement? =
    tryOrNull { LenientJson.parseToJsonElement(this) }

/**
 * Parses a [JsonObject] from its string representation, or returns null if it is not a valid
 * JSON object.
 */
@InternalReadiumApi
public fun String.toJsonObjectOrNull(): JsonObject? =
    toJsonElementOrNull() as? JsonObject

/**
 * Parses a [JsonArray] from its string representation, or returns null if it is not a valid
 * JSON array.
 */
@InternalReadiumApi
public fun String.toJsonArrayOrNull(): JsonArray? =
    toJsonElementOrNull() as? JsonArray

// Coercion helpers mirroring the behavior of the legacy `org.json` accessors.

private fun JsonElement?.coerceToString(): String? =
    when (this) {
        null, is JsonNull -> null
        is JsonPrimitive -> content
        else -> toString()
    }

private fun JsonElement?.coerceToBoolean(): Boolean? =
    (this as? JsonPrimitive)
        ?.takeUnless { it is JsonNull }
        ?.content
        ?.lowercase()
        ?.toBooleanStrictOrNull()

private fun JsonElement?.coerceToDouble(): Double? =
    (this as? JsonPrimitive)
        ?.takeUnless { it is JsonNull }
        ?.content
        ?.toDoubleOrNull()

private fun JsonElement?.coerceToInt(): Int? =
    (this as? JsonPrimitive)
        ?.takeUnless { it is JsonNull }
        ?.content
        ?.let { it.toIntOrNull() ?: it.toDoubleOrNull()?.toInt() }

private fun JsonElement?.coerceToLong(): Long? =
    (this as? JsonPrimitive)
        ?.takeUnless { it is JsonNull }
        ?.content
        ?.let { it.toLongOrNull() ?: it.toDoubleOrNull()?.toLong() }

/**
 * Returns the primitive's value when `org.json` would have parsed it as a [String]: either a
 * quoted JSON string, or an unquoted literal (allowed by [LenientJson]) which is not a boolean or
 * a number.
 */
private val JsonPrimitive.stringValueOrNull: String?
    get() = when {
        this is JsonNull -> null
        isString -> content
        // Unquoted literal: org.json parsed it as a String unless it was a boolean or a number.
        content.toBooleanStrictOrNull() == null && content.toDoubleOrNull() == null -> content
        else -> null
    }

/**
 * Returns this element's value if it is a JSON string, or `null` otherwise.
 *
 * Mirrors the legacy `value as? String` checks on `org.json` values, including for unquoted
 * literals in lenient documents.
 */
@InternalReadiumApi
public val JsonElement?.stringOrNull: String?
    get() = (this as? JsonPrimitive)?.stringValueOrNull

// Read accessors on [Map<String, JsonElement>], usable both with a [JsonObject] and with the
// `MutableMap<String, JsonElement>` copy used by parsers which consume recognized keys
// (see `toMutableMap()`).

/**
 * Returns the value mapped by [name] coerced to a string, or [fallback] if no such mapping exists.
 */
@InternalReadiumApi
public fun Map<String, JsonElement>.optString(name: String, fallback: String = ""): String =
    this[name].coerceToString() ?: fallback

/**
 * Returns the value mapped by [name] if it exists, or `null` if no such mapping exists, it is not
 * a string or it is empty.
 */
@InternalReadiumApi
public fun Map<String, JsonElement>.optNullableString(name: String): String? =
    this[name].stringOrNull
        ?.takeUnless(String::isEmpty)

/**
 * Returns the value mapped by [name] coerced to a boolean, or [fallback] otherwise.
 */
@InternalReadiumApi
public fun Map<String, JsonElement>.optBoolean(name: String, fallback: Boolean = false): Boolean =
    this[name].coerceToBoolean() ?: fallback

/**
 * Returns the value mapped by [name] coerced to a boolean, or `null` if no such mapping exists.
 */
@InternalReadiumApi
public fun Map<String, JsonElement>.optNullableBoolean(name: String): Boolean? =
    if (name in this) optBoolean(name) else null

/**
 * Returns the value mapped by [name] coerced to an integer, or [fallback] otherwise.
 */
@InternalReadiumApi
public fun Map<String, JsonElement>.optInt(name: String, fallback: Int = 0): Int =
    this[name].coerceToInt() ?: fallback

/**
 * Returns the value mapped by [name] coerced to an integer, or `null` if no such mapping exists.
 */
@InternalReadiumApi
public fun Map<String, JsonElement>.optNullableInt(name: String): Int? =
    if (name in this) optInt(name) else null

/**
 * Returns the value mapped by [name] coerced to a long, or [fallback] otherwise.
 */
@InternalReadiumApi
public fun Map<String, JsonElement>.optLong(name: String, fallback: Long = 0): Long =
    this[name].coerceToLong() ?: fallback

/**
 * Returns the value mapped by [name] coerced to a long, or `null` if no such mapping exists.
 */
@InternalReadiumApi
public fun Map<String, JsonElement>.optNullableLong(name: String): Long? =
    if (name in this) optLong(name) else null

/**
 * Returns the value mapped by [name] coerced to a double, or [fallback] otherwise.
 */
@InternalReadiumApi
public fun Map<String, JsonElement>.optDouble(name: String, fallback: Double = Double.NaN): Double =
    this[name].coerceToDouble() ?: fallback

/**
 * Returns the value mapped by [name] coerced to a double, or `null` if no such mapping exists.
 */
@InternalReadiumApi
public fun Map<String, JsonElement>.optNullableDouble(name: String): Double? =
    if (name in this) optDouble(name) else null

/**
 * Returns the value mapped by [name] if it exists and is a positive integer or can be coerced to
 * a positive integer, or `null` otherwise.
 */
@InternalReadiumApi
public fun Map<String, JsonElement>.optPositiveInt(name: String, fallback: Int = -1): Int? =
    optInt(name, fallback).takeIf { it >= 0 }

/**
 * Returns the value mapped by [name] if it exists and is a positive double or can be coerced to
 * a positive double, or `null` otherwise.
 */
@InternalReadiumApi
public fun Map<String, JsonElement>.optPositiveDouble(name: String, fallback: Double = -1.0): Double? =
    optDouble(name, fallback).takeIf { it >= 0 }

/**
 * Returns the value mapped by [name] if it exists and is either a [JsonArray] of [String] or a
 * single [String] value, or an empty list otherwise.
 *
 * E.g. ["a", "b"] or "a"
 */
@InternalReadiumApi
public fun Map<String, JsonElement>.optStringsFromArrayOrSingle(name: String): List<String> =
    when (val value = this[name]) {
        is JsonArray -> value.mapNotNull { it.stringOrNull }
        is JsonPrimitive -> listOfNotNull(value.stringValueOrNull)
        else -> emptyList()
    }

/**
 * Returns the [JsonObject] mapped by [name], or `null` otherwise.
 */
@InternalReadiumApi
public fun Map<String, JsonElement>.optJsonObject(name: String): JsonObject? =
    this[name] as? JsonObject

/**
 * Returns the [JsonArray] mapped by [name], or `null` otherwise.
 */
@InternalReadiumApi
public fun Map<String, JsonElement>.optJsonArray(name: String): JsonArray? =
    this[name] as? JsonArray

// Removal variants, for parsers consuming recognized keys from a `MutableMap<String, JsonElement>`
// copy of the source [JsonObject] (`json.toMutableMap()`), keeping the leftover for extensions.

/**
 * Returns the value mapped by [name] if it exists, or `null` if no such mapping exists, it is not
 * a string or it is empty.
 * If [remove] is true, then the mapping will be removed from the map.
 */
@InternalReadiumApi
public fun MutableMap<String, JsonElement>.optNullableString(name: String, remove: Boolean): String? =
    optNullableString(name)
        .also { if (remove) remove(name) }

/**
 * Returns the value mapped by [name] coerced to an integer, or `null` if no such mapping exists.
 * If [remove] is true, then the mapping will be removed from the map.
 */
@InternalReadiumApi
public fun MutableMap<String, JsonElement>.optNullableInt(name: String, remove: Boolean): Int? =
    optNullableInt(name)
        .also { if (remove) remove(name) }

/**
 * Returns the value mapped by [name] coerced to a double, or `null` if no such mapping exists.
 * If [remove] is true, then the mapping will be removed from the map.
 */
@InternalReadiumApi
public fun MutableMap<String, JsonElement>.optNullableDouble(name: String, remove: Boolean): Double? =
    optNullableDouble(name)
        .also { if (remove) remove(name) }

/**
 * Returns the value mapped by [name] if it exists and is a positive integer or can be coerced to
 * a positive integer, or `null` otherwise.
 * If [remove] is true, then the mapping will be removed from the map.
 */
@InternalReadiumApi
public fun MutableMap<String, JsonElement>.optPositiveInt(
    name: String,
    fallback: Int = -1,
    remove: Boolean,
): Int? =
    optPositiveInt(name, fallback)
        .also { if (remove) remove(name) }

/**
 * Returns the value mapped by [name] if it exists and is a positive double or can be coerced to
 * a positive double, or `null` otherwise.
 * If [remove] is true, then the mapping will be removed from the map.
 */
@InternalReadiumApi
public fun MutableMap<String, JsonElement>.optPositiveDouble(
    name: String,
    fallback: Double = -1.0,
    remove: Boolean,
): Double? =
    optPositiveDouble(name, fallback)
        .also { if (remove) remove(name) }

/**
 * Returns the value mapped by [name] if it exists and is either a [JsonArray] of [String] or a
 * single [String] value, or an empty list otherwise.
 * If [remove] is true, then the mapping will be removed from the map.
 */
@InternalReadiumApi
public fun MutableMap<String, JsonElement>.optStringsFromArrayOrSingle(
    name: String,
    remove: Boolean,
): List<String> =
    optStringsFromArrayOrSingle(name)
        .also { if (remove) remove(name) }

// Unwrapping to plain Kotlin values.

/**
 * Unwraps recursively the [JsonObject] to a [Map<String, Any>], ignoring null values.
 *
 * Numbers are unwrapped to [Int], [Long] or [Double], whichever fits first.
 */
@InternalReadiumApi
public fun JsonObject.toMap(): Map<String, Any> =
    unwrapJsonObject(this)

/**
 * Unwraps recursively the [JsonArray] to a [List<Any>], ignoring null values.
 *
 * Numbers are unwrapped to [Int], [Long] or [Double], whichever fits first.
 */
@InternalReadiumApi
public fun JsonArray.toList(): List<Any> =
    unwrapJsonArray(this)

private fun unwrapJsonObject(value: JsonObject): Map<String, Any> =
    buildMap {
        for ((key, element) in value) {
            unwrapJson(element)?.let { put(key, it) }
        }
    }

private fun unwrapJsonArray(value: JsonArray): List<Any> =
    value.mapNotNull { unwrapJson(it) }

private fun unwrapJson(value: JsonElement): Any? =
    when (value) {
        is JsonNull -> null
        is JsonObject -> unwrapJsonObject(value)
        is JsonArray -> unwrapJsonArray(value)
        is JsonPrimitive ->
            if (value.isString) {
                value.content
            } else {
                value.content.toBooleanStrictOrNull()
                    ?: value.content.toIntOrNull()
                    ?: value.content.toLongOrNull()
                    ?: value.content.toDoubleOrNull()
                    ?: value.content
            }
    }

// Wrapping of plain Kotlin values into [JsonElement]s.

/**
 * Wraps recursively a plain Kotlin value ([String], [Boolean], [Number], [Map], [Collection] or
 * [JsonElement]) into its [JsonElement] representation.
 *
 * Returns null for null values. When [dropEmpty] is true (the default), empty maps, collections
 * and [JSONable] representations are dropped as well, mirroring the legacy `putIfNotEmpty`
 * serialization behavior. Use `dropEmpty = false` to mirror the legacy `JSONObject(Map)`
 * conversion, which kept empty nested objects and arrays.
 */
@InternalReadiumApi
public fun wrapJson(value: Any?, dropEmpty: Boolean = true): JsonElement? =
    when (value) {
        null -> null
        is JsonElement -> value
        is JSONable ->
            value.toJSON()
                .takeIf { it.isNotEmpty() || !dropEmpty }
        is Boolean -> JsonPrimitive(value)
        is Int -> JsonPrimitive(value)
        is Long -> JsonPrimitive(value)
        is Double -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is String -> JsonPrimitive(value)
        is Map<*, *> ->
            value
                .takeIf { it.isNotEmpty() || !dropEmpty }
                ?.let { map ->
                    buildJsonObject {
                        for ((key, element) in map) {
                            if (key is String) {
                                wrapJson(element, dropEmpty)?.let { put(key, it) }
                            }
                        }
                    }
                }
        is Collection<*> ->
            value
                .takeIf { it.isNotEmpty() || !dropEmpty }
                ?.let { collection ->
                    JsonArray(collection.mapNotNull { wrapJson(it, dropEmpty) })
                }
        else -> JsonPrimitive(value.toString())
    }

/**
 * Wraps recursively this map into its [JsonObject] representation, or returns null if it holds
 * non-[String] keys only.
 *
 * Mirrors the legacy `JSONObject(Map)` conversion: empty nested objects and arrays are kept.
 */
@InternalReadiumApi
public fun Map<*, *>.toJsonObject(): JsonObject? =
    wrapJson(this, dropEmpty = false) as? JsonObject

// Builder helpers, mirroring the `org.json` mutation-style `put` helpers with a
// [buildJsonObject] builder.

/**
 * Maps [name] to [value] if it is not null. A null [value] is dropped, like with `org.json`.
 */
@InternalReadiumApi
public fun JsonObjectBuilder.putIfNotNull(name: String, value: String?) {
    value?.let { put(name, JsonPrimitive(it)) }
}

/**
 * Maps [name] to [value] if it is not null. A null [value] is dropped, like with `org.json`.
 */
@InternalReadiumApi
public fun JsonObjectBuilder.putIfNotNull(name: String, value: Boolean?) {
    value?.let { put(name, JsonPrimitive(it)) }
}

/**
 * Maps [name] to [value] if it is not null. A null [value] is dropped, like with `org.json`.
 */
@InternalReadiumApi
public fun JsonObjectBuilder.putIfNotNull(name: String, value: Number?) {
    value?.let { put(name, JsonPrimitive(it)) }
}

/**
 * Maps [name] to [jsonable] after converting it to a [JsonObject], unless the resulting object
 * is empty.
 */
@InternalReadiumApi
public fun JsonObjectBuilder.putIfNotEmpty(name: String, jsonable: JSONable?) {
    val json = jsonable?.toJSON()
    if (json == null || json.isEmpty()) {
        return
    }
    put(name, json)
}

/**
 * Maps [name] to [collection] after wrapping it in a [JsonArray], unless the wrapped collection
 * is empty.
 *
 * This also accepts a [JsonArray], since it is a [Collection] of [JsonElement].
 */
@InternalReadiumApi
public fun JsonObjectBuilder.putIfNotEmpty(name: String, collection: Collection<*>?) {
    val wrapped = collection?.mapNotNull { wrapJson(it) }
    if (wrapped.isNullOrEmpty()) {
        return
    }
    put(name, JsonArray(wrapped))
}

/**
 * Maps [name] to [map] after wrapping it in a [JsonObject], unless it is empty.
 *
 * This also accepts a [JsonObject], since it is a [Map] of [JsonElement].
 */
@InternalReadiumApi
public fun JsonObjectBuilder.putIfNotEmpty(name: String, map: Map<String, *>?) {
    if (map == null || map.isEmpty()) {
        return
    }
    val wrapped = (map as? JsonObject) ?: (wrapJson(map) as? JsonObject)
    if (wrapped == null || wrapped.isEmpty()) {
        return
    }
    put(name, wrapped)
}

/**
 * Maps every entry of [map] after wrapping its value, dropping null and unwrappable values.
 *
 * Empty nested objects and arrays are kept, like with the legacy `JSONObject(Map)` conversion.
 */
@InternalReadiumApi
public fun JsonObjectBuilder.putAll(map: Map<String, *>) {
    for ((key, value) in map) {
        wrapJson(value, dropEmpty = false)?.let { put(key, it) }
    }
}

/**
 * Parses a [JsonArray] of [JsonElement] into a [List] of models using the given [factory].
 */
@InternalReadiumApi
public inline fun <T> JsonArray?.parseObjects(factory: (JsonElement) -> T?): List<T> =
    this?.mapNotNull(factory) ?: emptyList()
