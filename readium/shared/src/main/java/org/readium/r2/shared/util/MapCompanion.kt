/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import org.readium.r2.shared.InternalReadiumApi

/**
 * Encapsulates a [Map] into a more limited query API.
 *
 * This is most useful as an [Enum] companion, to provide parsing of raw strings.
 * ```
 * enum class Layout(val value: String) {
 *     PAGINATED("paginated"),
 *     REFLOWABLE("reflowable");
 *
 *     companion object : MapCompanion<Layout>(values(), Layout::value)
 * }
 *
 * val layout: Layout? = Layout("reflowable")
 * ```
 */
@InternalReadiumApi
open class MapCompanion<E>(
    protected val map: Map<String, E>,
    private val keySelector: (E) -> String
) : ValueCoder<E?, String?> {

    constructor(elements: Array<E>, keySelector: (E) -> String):
        this(elements.associateBy(keySelector), keySelector)

    /**
     * Returns the available [keys].
     */
    val keys: Set<String>
        get() = map.keys

    /**
     * Returns the element matching the [key], or null if not found.
     *
     * To be overridden in subclasses if custom retrieval is needed – for example, testing lowercase
     * keys.
     */
    open fun get(key: String?): E? =
        key?.let { map[key] }

    /**
     * Returns the key matching the given [element].
     */
    open fun getKey(element: E): String = keySelector(element)

    /**
     * Alias to [get], to be used like `keyMapper("a_key")`.
     */
    open operator fun invoke(key: String?): E? = get(key)

    @Deprecated("Use `Enum(\"value\")` instead", ReplaceWith("get(key)"))
    open fun from(key: String?): E? = get(key)

    override fun decode(rawValue: Any): E? =
        get(rawValue as? String)

    override fun encode(value: E?): String? =
        value?.let { getKey(it) }
}

/**
 * Extends a [MapCompanion] by adding a [default] value as a fallback.
 */
@InternalReadiumApi
open class MapWithDefaultCompanion<E>(map: Map<String, E>, keySelector: (E) -> String, val default: E) : MapCompanion<E>(map, keySelector) {

    constructor(elements: Array<E>, keySelector: (E) -> String, default: E):
        this(elements.associateBy(keySelector), keySelector, default)

    /**
     * Returns the element matching the [key], or the [default] value as a fallback.
     */
    fun getOrDefault(key: String?): E =
        get(key) ?: default

    /**
     * Alias to [getOrDefault], to be used like `keyMapper("a_key")`.
     */
    override operator fun invoke(key: String?): E = getOrDefault(key)

    @Deprecated("Use `Enum(\"value\")` instead", ReplaceWith("getOrDefault(key)"))
    override fun from(key: String?): E? = getOrDefault(key)

}
