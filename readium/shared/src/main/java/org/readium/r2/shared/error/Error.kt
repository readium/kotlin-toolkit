/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.error

interface Error {

    val message: String
    val cause: Error?
}

class ThrowableError(
    val throwable: Throwable
) : Error {

    override val message: String =
        throwable.message ?: "Exception"

    override val cause: Error? =
        null
}

class SimpleError(
    override val message: String,
    override val cause: Error? = null
) : Error
