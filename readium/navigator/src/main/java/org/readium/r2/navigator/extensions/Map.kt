/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.extensions

/**
 * Returns a new [Map] after merging the values of [this] with [other].
 */
internal fun <K, V> Map<K, V>.merge(other: Map<K, V>): Map<K, V> =
    (other.entries + this.entries)
        .groupBy({ it.key }, { it.value })
        .mapValues { (_, value) -> value.first() }

/**
 * Returns a new [Map] after merging the values of [this] with the given list of [values].
 */
internal fun <K, V> Map<K, V>.merge(vararg values: Pair<K, V>): Map<K, V> =
    merge(mapOf(*values))
