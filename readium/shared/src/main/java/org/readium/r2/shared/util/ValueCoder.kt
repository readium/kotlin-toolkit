/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import kotlin.reflect.KClass
import kotlin.reflect.safeCast

/**
 * Encodes a value of type [V] into its raw representation of type [R].
 */
fun interface ValueEncoder<V, R> {
    fun encode(value: V): R
}

/**
 * Decodes a value of type [V] from its raw representation.
 */
fun interface ValueDecoder<V> {
    fun decode(rawValue: Any): V
}

/**
 * Encodes and decodes a value of type [V] into/from its raw representation of type [R].
 */
interface ValueCoder<V, R>: ValueEncoder<V, R>, ValueDecoder<V>

/**
 * Encodes/decodes values whose raw representation is themselves.
 *
 * Useful for simple types like [Boolean], [String], etc.
 */
class IdentityValueCoder<V : Any>(private val klass: KClass<V>) : ValueCoder<V?, V?> {
    override fun encode(value: V?): V? = value
    override fun decode(rawValue: Any): V? = klass.safeCast(rawValue)
}
