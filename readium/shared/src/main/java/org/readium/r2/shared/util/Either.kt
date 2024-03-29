/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

/**
 * Generic wrapper to store two mutually exclusive types.
 */
public sealed class Either<out A, out B> {
    public data class Left<A, B>(val value: A) : Either<A, B>()
    public data class Right<A, B>(val value: B) : Either<A, B>()

    public companion object {
        public inline operator fun <reified A, reified B> invoke(value: Any): Either<A, B> =
            when (value) {
                is A -> Left(value)
                is B -> Right(value)
                else -> throw IllegalArgumentException(
                    "Provided value must be an instance of ${A::class.simpleName} or ${B::class.simpleName}"
                )
            }
    }

    public val left: A?
        get() = (this as? Left)?.value

    public val right: B?
        get() = (this as? Right)?.value

    public inline fun onLeft(action: (value: A) -> Unit): Either<A, B> {
        (this as? Left)?.let {
            action(it.value)
        }
        return this
    }

    public inline fun onRight(action: (value: B) -> Unit): Either<A, B> {
        (this as? Right)?.let {
            action(it.value)
        }
        return this
    }
}
