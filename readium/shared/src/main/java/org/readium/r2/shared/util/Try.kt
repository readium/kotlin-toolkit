/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import org.readium.r2.shared.util.Try.Failure
import org.readium.r2.shared.util.Try.Success

/** A [Result] type which can be used as a return type. */
public sealed class Try<out Success, out Failure> {

    public companion object {
        /** Returns an instance that encapsulates the given value as successful value. */
        public fun <Success> success(success: Success): Try<Success, Nothing> = Success(success)

        /** Returns the encapsulated Throwable exception if this instance represents failure or null if it is success. */
        public fun <Failure> failure(failure: Failure): Try<Nothing, Failure> = Failure(failure)
    }

    public abstract val isSuccess: Boolean
    public abstract val isFailure: Boolean

    /** Returns the encapsulated value if this instance represents success or null if it is failure. */
    public abstract fun getOrNull(): Success?

    /** Returns the encapsulated [Throwable] exception if this instance represents failure or null if it is success. */
    public abstract fun failureOrNull(): Failure?

    public class Success<out S, out F>(public val value: S) : Try<S, F>() {
        override val isSuccess: Boolean get() = true
        override val isFailure: Boolean get() = false
        override fun getOrNull(): S? = value
        override fun failureOrNull(): F? = null
    }

    public class Failure<out S, out F>(public val value: F) : Try<S, F>() {
        override val isSuccess: Boolean get() = false
        override val isFailure: Boolean get() = true
        override fun getOrNull(): S? = null
        override fun failureOrNull(): F = value
    }

    /**
     * Returns the encapsulated result of the given transform function applied to the encapsulated value
     * if this instance represents success or the original encapsulated [Throwable] exception if it is failure.
     */
    public inline fun <R> map(transform: (value: Success) -> R): Try<R, Failure> =
        when (this) {
            is Try.Success -> success(transform(value))
            is Try.Failure -> failure(value)
        }

    /**
     * Returns the encapsulated result of the given transform function applied to the encapsulated failure
     * if this instance represents failure or the original encapsulated success value if it is a success.
     */
    public inline fun <F> mapFailure(transform: (value: Failure) -> F): Try<Success, F> =
        when (this) {
            is Try.Success -> success(value)
            is Try.Failure -> failure(transform(failureOrNull()))
        }

    /**
     * Returns the result of [onSuccess] for the encapsulated value if this instance represents success or
     * the result of [onFailure] function for the encapsulated value if it is failure.
     */
    public inline fun <R> fold(
        onSuccess: (value: Success) -> R,
        onFailure: (exception: Failure) -> R,
    ): R =
        when (this) {
            is Try.Success -> onSuccess(value)
            is Try.Failure -> onFailure(failureOrNull())
        }

    /**
     * Performs the given action on the encapsulated value if this instance represents success.
     * Returns the original [Try] unchanged.
     */
    public inline fun onSuccess(action: (value: Success) -> Unit): Try<Success, Failure> {
        if (this is Try.Success) action(value)
        return this
    }

    /**
     * Performs the given action on the encapsulated value if this instance represents failure.
     * Returns the original [Try] unchanged.
     */
    public inline fun onFailure(action: (exception: Failure) -> Unit): Try<Success, Failure> {
        if (this is Try.Failure) action(failureOrNull())
        return this
    }
}

/**
 * Returns the encapsulated value if this instance represents success
 * or throws the encapsulated Throwable exception if it is failure.
 */
public fun <S, F : Throwable> Try<S, F>.getOrThrow(): S =
    when (this) {
        is Success -> value
        is Failure -> throw value
    }

/**
 * Returns the encapsulated value if this instance represents success or the [defaultValue] if it is failure.
 */
public fun <R, S : R, F> Try<S, F>.getOrDefault(defaultValue: R): R =
    when (this) {
        is Success -> value
        is Failure -> defaultValue
    }

/**
 * Returns the encapsulated value if this instance represents success or the result of [onFailure] function
 * for the encapsulated value if it is failure.
 */
public inline fun <R, S : R, F> Try<S, F>.getOrElse(onFailure: (exception: F) -> R): R =
    when (this) {
        is Success -> value
        is Failure -> onFailure(value)
    }

/**
 * Returns the encapsulated result of the given transform function applied to the encapsulated
 * value if this instance represents success or the original encapsulated value if it is failure.
 */
public inline fun <R, S, F> Try<S, F>.flatMap(transform: (value: S) -> Try<R, F>): Try<R, F> =
    when (this) {
        is Success -> transform(value)
        is Failure -> Try.failure(value)
    }

/**
 * Returns the encapsulated result of the given transform function applied to the encapsulated value
 * if this instance represents failure or the original encapsulated value if it is success.
 */
public inline fun <R, S : R, F, T> Try<S, F>.tryRecover(transform: (exception: F) -> Try<R, T>): Try<R, T> =
    when (this) {
        is Success -> Try.success(value)
        is Failure -> transform(value)
    }

/**
 * Returns value in case of success and throws an [IllegalStateException] in case of failure.
 */
public fun <S, F> Try<S, F>.checkSuccess(): S =
    when (this) {
        is Success ->
            value
        is Failure -> {
            throw IllegalStateException(
                "Try was excepted to contain a success.",
                value as? Throwable ?: (value as? Error)?.let { ErrorException(it) }
            )
        }
    }
