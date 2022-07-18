/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

/**
 * Encodes a value of type [V] into its raw representation of type [R].
 */
fun interface ValueEncoder<V, R> {
    fun encode(value: V): R
}

/**
 * Decodes a value of type [V] from its raw representation of type [R].
 */
fun interface ValueDecoder<V, R> {
    fun decode(rawValue: R): V
}

/**
 * Encodes and decodes a value of type [V] into/from its raw representation of type [R].
 */
interface ValueCoder<V, R>: ValueEncoder<V, R>, ValueDecoder<V, R>

/**
 * Encodes/decodes values whose raw representation is themselves.
 *
 * Useful for simple types like [Boolean], [String], etc.
 */
class IdentityValueCoder<V> : ValueCoder<V, V> {
    override fun encode(value: V): V = value
    override fun decode(rawValue: V): V = rawValue
}
