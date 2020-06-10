/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.util

/** A [Result] type which can be used as a return type. */
class Try<out Success, out Failure: Throwable> private constructor(private val success: Success?, private val failure: Failure?) {

    companion object {
        /** Returns an instance that encapsulates the given value as successful value. */
        fun <Success> success(success: Success) = Try(success, null)

        /** Returns the encapsulated Throwable exception if this instance represents failure or null if it is success. */
        fun <Failure: Throwable> failure(failure: Failure) = Try(null, failure)
    }

    val isSuccess get() = success != null

    val isFailure get() = failure != null

    /**
     * Returns the encapsulated value if this instance represents success
     * or throws the encapsulated Throwable exception if it is failure.
     */
    fun getOrThrow() = success
        ?: throw failure!!

    /** Returns the encapsulated value if this instance represents success or null if it is failure. */
    fun getOrNull(): Success? = success

    /** Returns the encapsulated [Throwable] exception if this instance represents failure or null if it is success. */
    fun exceptionOrNull(): Failure? = failure

    /**
     * Returns the encapsulated result of the given transform function applied to the encapsulated value
     * if this instance represents success or the original encapsulated [Throwable] exception if it is failure.
     */
    fun <R> map(transform: (value: Success) -> R): Try<R, Failure> =
        when {
            isSuccess -> Try.success(transform(success!!))
            else -> Try.failure(failure!!)
        }

    /**
     * Returns the result of onSuccess for the encapsulated value if this instance represents success or
     * the result of onFailure function for the encapsulated Throwable exception if it is failure.
     */
    fun <R> fold(onSuccess: (value: Success) -> R, onFailure: (exception: Throwable) -> R): R =
        if (isSuccess) onSuccess(success!!) else onFailure(failure!!)

    /**
     * Performs the given action on the encapsulated value if this instance represents success.
     * Returns the original Result unchanged.
     */
    fun onSuccess(action: (value: Success) -> Unit): Try<Success, Failure> {
        if (isSuccess) action(success!!)
        return this
    }

    /**
     * Returns the encapsulated value if this instance represents success or the result of onFailure function
     * for the encapsulated [Throwable] exception if it is failure.
     */
    fun <R, S : R, F : Throwable> Try<S, F>.getOrElse(onFailure: (exception: F) -> R): R =
        if (isSuccess) success!! else onFailure(failure!!)

    fun <R, S, F: Throwable> Try<S, F>.flatMap(transform: (value: S) -> Try<R, F>): Try<R, F> =
        when {
            isSuccess -> transform(success!!)
            else -> Try.failure(failure!!)
        }
}

