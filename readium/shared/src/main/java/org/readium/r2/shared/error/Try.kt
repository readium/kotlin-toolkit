/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.error

/** A [Result] type which can be used as a return type. */
sealed class Try<out Success, out Failure> {

    companion object {
        /** Returns an instance that encapsulates the given value as successful value. */
        fun <Success> success(success: Success): Try<Success, Nothing> = Success(success)

        /** Returns the encapsulated Throwable exception if this instance represents failure or null if it is success. */
        fun <Failure> failure(failure: Failure): Try<Nothing, Failure> = Failure(failure)
    }

    abstract val isSuccess: Boolean
    abstract val isFailure: Boolean

    /** Returns the encapsulated value if this instance represents success or null if it is failure. */
    abstract fun getOrNull(): Success?

    /** Returns the encapsulated [Throwable] exception if this instance represents failure or null if it is success. */
    abstract fun failureOrNull(): Failure?

    class Success<out S, out F>(val value: S) : Try<S, F>() {
        override val isSuccess: Boolean get() = true
        override val isFailure: Boolean get() = false
        override fun getOrNull(): S? = value
        override fun failureOrNull(): F? = null
    }

    class Failure<out S, out F>(val value: F) : Try<S, F>() {
        override val isSuccess: Boolean get() = false
        override val isFailure: Boolean get() = true
        override fun getOrNull(): S? = null
        override fun failureOrNull(): F = value
    }

    /**
     * Returns the encapsulated result of the given transform function applied to the encapsulated value
     * if this instance represents success or the original encapsulated [Throwable] exception if it is failure.
     */
    inline fun <R> map(transform: (value: Success) -> R): Try<R, Failure> =
        when (this) {
            is Try.Success -> success(transform(value))
            is Try.Failure -> failure(value)
        }

    /**
     * Returns the encapsulated result of the given transform function applied to the encapsulated failure
     * if this instance represents failure or the original encapsulated success value if it is a success.
     */
    inline fun <F> mapFailure(transform: (value: Failure) -> F): Try<Success, F> =
        when (this) {
            is Try.Success -> success(value)
            is Try.Failure -> failure(transform(failureOrNull()!!))
        }

    /**
     * Returns the result of [onSuccess] for the encapsulated value if this instance represents success or
     * the result of [onFailure] function for the encapsulated value if it is failure.
     */
    inline fun <R> fold(onSuccess: (value: Success) -> R, onFailure: (exception: Failure) -> R): R =
        when (this) {
            is Try.Success -> onSuccess(value)
            is Try.Failure -> onFailure(failureOrNull()!!)
        }

    /**
     * Performs the given action on the encapsulated value if this instance represents success.
     * Returns the original [Try] unchanged.
     */
    inline fun onSuccess(action: (value: Success) -> Unit): Try<Success, Failure> {
        if (this is Try.Success) action(value)
        return this
    }

    /**
     * Performs the given action on the encapsulated value if this instance represents failure.
     * Returns the original [Try] unchanged.
     */
    inline fun onFailure(action: (exception: Failure) -> Unit): Try<Success, Failure> {
        if (this is Try.Failure) action(failureOrNull()!!)
        return this
    }
}

/**
 * Returns the encapsulated value if this instance represents success
 * or throws the encapsulated Throwable exception if it is failure.
 */
fun <S, F : Throwable> Try<S, F>.getOrThrow(): S =
    when (this) {
        is Try.Success -> value
        is Try.Failure -> throw value
    }

/**
 * Returns the encapsulated value if this instance represents success or the [defaultValue] if it is failure.
 */
fun <R, S : R, F> Try<S, F>.getOrDefault(defaultValue: R): R =
    when (this) {
        is Try.Success -> value
        is Try.Failure -> defaultValue
    }

/**
 * Returns the encapsulated value if this instance represents success or the result of [onFailure] function
 * for the encapsulated value if it is failure.
 */
inline fun <R, S : R, F> Try<S, F>.getOrElse(onFailure: (exception: F) -> R): R =
    when (this) {
        is Try.Success -> value
        is Try.Failure -> onFailure(value)
    }

/**
 * Returns the encapsulated result of the given transform function applied to the encapsulated
 * value if this instance represents success or the original encapsulated value if it is failure.
 */
inline fun <R, S, F> Try<S, F>.flatMap(transform: (value: S) -> Try<R, F>): Try<R, F> =
    when (this) {
        is Try.Success -> transform(value)
        is Try.Failure -> Try.failure(value)
    }

/**
 * Returns the encapsulated result of the given transform function applied to the encapsulated value
 * if this instance represents failure or the original encapsulated value if it is success.
 */
inline fun <R, S : R, F> Try<S, F>.tryRecover(transform: (exception: F) -> Try<R, F>): Try<R, F> =
    when (this) {
        is Try.Success -> Try.success(value)
        is Try.Failure -> transform(value)
    }
