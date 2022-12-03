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
sealed class Try<out Success, out Failure : Throwable> {

    companion object {
        /** Returns an instance that encapsulates the given value as successful value. */
        fun <Success> success(success: Success): Try<Success, Nothing> = Success(success)

        /** Returns the encapsulated Throwable exception if this instance represents failure or null if it is success. */
        fun <Failure : Throwable> failure(failure: Failure): Try<Nothing, Failure> = Failure(failure)
    }

    abstract val isSuccess: Boolean
    abstract val isFailure: Boolean

    /**
     * Returns the encapsulated value if this instance represents success
     * or throws the encapsulated Throwable exception if it is failure.
     */
    abstract fun getOrThrow(): Success

    /** Returns the encapsulated value if this instance represents success or null if it is failure. */
    abstract fun getOrNull(): Success?

    /** Returns the encapsulated [Throwable] exception if this instance represents failure or null if it is success. */
    abstract fun exceptionOrNull(): Failure?

    class Success<out S, out F : Throwable>(val value: S) : Try<S, F>() {
        override val isSuccess: Boolean get() = true
        override val isFailure: Boolean get() = false
        override fun getOrThrow(): S = value
        override fun getOrNull(): S? = value
        override fun exceptionOrNull(): F? = null
    }

    class Failure<out S, out F : Throwable>(val exception: F) : Try<S, F>() {
        override val isSuccess: Boolean get() = false
        override val isFailure: Boolean get() = true
        override fun getOrThrow(): S { throw exception }
        override fun getOrNull(): S? = null
        override fun exceptionOrNull(): F? = exception
    }

    /**
     * Returns the encapsulated result of the given transform function applied to the encapsulated value
     * if this instance represents success or the original encapsulated [Throwable] exception if it is failure.
     */
    inline fun <R> map(transform: (value: Success) -> R): Try<R, Failure> =
        if (isSuccess)
            success(transform(getOrThrow()))
        else
            failure(exceptionOrNull()!!)

    /**
     * Returns the encapsulated result of the given transform function applied to the encapsulated failure
     * if this instance represents failure or the original encapsulated success value if it is a success.
     */
    inline fun <F : Throwable> mapFailure(transform: (value: Failure) -> F): Try<Success, F> =
        if (isSuccess)
            success(getOrThrow())
        else
            failure(transform(exceptionOrNull()!!))

    /**
     * Returns the result of [onSuccess] for the encapsulated value if this instance represents success or
     * the result of [onFailure] function for the encapsulated [Throwable] exception if it is failure.
     */
    inline fun <R> fold(onSuccess: (value: Success) -> R, onFailure: (exception: Throwable) -> R): R =
        if (isSuccess)
            onSuccess(getOrThrow())
        else
            onFailure(exceptionOrNull()!!)

    /**
     * Performs the given action on the encapsulated value if this instance represents success.
     * Returns the original [Try] unchanged.
     */
    inline fun onSuccess(action: (value: Success) -> Unit): Try<Success, Failure> {
        if (isSuccess) action(getOrThrow())
        return this
    }

    /**
     * Performs the given action on the encapsulated [Throwable] exception if this instance represents failure.
     * Returns the original [Try] unchanged.
     */
    inline fun onFailure(action: (exception: Failure) -> Unit): Try<Success, Failure> {
        if (isFailure) action(exceptionOrNull()!!)
        return this
    }
}

/**
 * Returns the encapsulated value if this instance represents success or the [defaultValue] if it is failure.
 */
fun <R, S : R, F : Throwable> Try<S, F>.getOrDefault(defaultValue: R): R =
    if (isSuccess)
        getOrThrow()
    else
        defaultValue

/**
 * Returns the encapsulated value if this instance represents success or the result of [onFailure] function
 * for the encapsulated [Throwable] exception if it is failure.
 */
inline fun <R, S : R, F : Throwable> Try<S, F>.getOrElse(onFailure: (exception: F) -> R): R =
    if (isSuccess)
        getOrThrow()
    else
        onFailure(exceptionOrNull()!!)

inline fun <R, S, F : Throwable> Try<S, F>.flatMap(transform: (value: S) -> Try<R, F>): Try<R, F> =
    if (isSuccess)
        transform(getOrThrow())
    else
        Try.failure(exceptionOrNull()!!)

/**
 * Returns the encapsulated result of the given transform function applied to the encapsulated |Throwable] exception
 * if this instance represents failure or the original encapsulated value if it is success.
 */
inline fun <R, S : R, F : Throwable> Try<S, F>.recover(transform: (exception: F) -> R): Try<R, Nothing> =
    if (isSuccess)
        Try.success(getOrThrow())
    else
        Try.success(transform(exceptionOrNull()!!))

/**
 * Returns the encapsulated result of the given transform function applied to the encapsulated |Throwable] exception
 * if this instance represents failure or the original encapsulated value if it is success.
 */
inline fun <R, S : R, F : Throwable> Try<S, F>.tryRecover(transform: (exception: F) -> Try<R, F>): Try<R, F> =
    if (isSuccess)
        Try.success(getOrThrow())
    else
        transform(exceptionOrNull()!!)
