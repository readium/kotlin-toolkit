/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu, Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.util

/**
 * Encapsulates a [Map] into a more limited query API.
 *
 * This is most useful as an [Enum] companion, to provide parsing of raw values.
 * ```
 * enum class Layout(val value: String) {
 *     PAGINATED("paginated"),
 *     REFLOWABLE("reflowable");
 *
 *     companion object : KeyMapper<String, Layout>(values(), Layout::value)
 * }
 *
 * val layout: Layout? = Layout("reflowable")
 * ```
 */
open class MapCompanion<K, E>(protected val map: Map<K, E>) {

    constructor(elements: Array<E>, keySelector: (E) -> K) :
        this(elements.associateBy(keySelector))

    /**
     * Returns the available [keys].
     */
    val keys: Set<K>
        get() = map.keys

    /**
     * Returns the element matching the [key], or [null] if not found.
     *
     * To be overriden in subclasses if custom retrieval is needed – for example, testing lowercase
     * keys.
     */
    open fun get(key: K?): E? =
        key?.let { map[key] }

    /**
     * Alias to [get], to be used like `keyMapper("a_key")`.
     */
    open operator fun invoke(key: K?): E? = get(key)

    @Deprecated("Use `Enum(\"value\")` instead", ReplaceWith("get(key)"))
    open fun from(key: K?): E? = get(key)
}

/**
 * Extends a [MapCompanion] by adding a [default] value as a fallback.
 */
open class MapWithDefaultCompanion<K, E>(map: Map<K, E>, val default: E) : MapCompanion<K, E>(map) {

    constructor(elements: Array<E>, keySelector: (E) -> K, default: E) :
        this(elements.associateBy(keySelector), default)

    /**
     * Returns the element matching the [key], or the [default] value as a fallback.
     */
    fun getOrDefault(key: K?): E =
        get(key) ?: default

    /**
     * Alias to [getOrDefault], to be used like `keyMapper("a_key")`.
     */
    override operator fun invoke(key: K?): E = getOrDefault(key)

    @Deprecated("Use `Enum(\"value\")` instead", ReplaceWith("getOrDefault(key)"))
    override fun from(key: K?): E? = getOrDefault(key)
}
