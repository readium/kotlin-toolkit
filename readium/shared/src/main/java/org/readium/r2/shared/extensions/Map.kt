/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.extensions

internal fun Map<String, List<String>>.lowerCaseKeys(): Map<String, List<String>> {
    val normalizedMap = mutableMapOf<String, MutableList<String>>()
    for ((k, v) in this) {
        normalizedMap.getOrPut(
            k.lowercase(),
            mutableListOf()
        ).addAll(v)
    }
    return normalizedMap
}

internal fun <K, V> MutableMap<K, V>.getOrPut(key: K, fallbackValue: V): V =
    get(key) ?: run {
        put(key, fallbackValue)
        fallbackValue
    }

internal fun Map<String, List<String>>.joinValues(separator: CharSequence): Map<String, String> =
    mapValues { it.value.joinToString(separator) }

internal fun <K, V> Map<K, List<V>>.toMutable() =
    mapValues { it.value.toMutableList() }.toMutableMap()
