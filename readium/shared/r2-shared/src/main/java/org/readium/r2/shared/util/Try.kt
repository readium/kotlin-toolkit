/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.util

class Try<out Success, out Failure> private constructor(private val _success: Success?, private val _failure: Failure?) {

    companion object {
        fun <Success> success(success: Success) = Try(success, null)

        fun <Failure> failure(failure: Failure) = Try(null, failure)
    }

    val isSuccess get() = _success != null

    val isFailure get() = _failure != null

    val success
        get() = _success!!

    val failure
        get() = _failure!!

    fun successOrNull() = _success

    fun failureOrNull() = _failure

    fun <R> map(transform: (value: Success) -> R): Try<R, Failure> =
        when {
            isSuccess -> Try.success(transform(success))
            else -> Try.failure(failure)
        }
}