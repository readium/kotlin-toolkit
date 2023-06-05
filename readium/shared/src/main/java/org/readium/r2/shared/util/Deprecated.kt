// ktlint-disable filename

/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

/**
 * Returns the encapsulated result of the given transform function applied to the encapsulated |Throwable] exception
 * if this instance represents failure or the original encapsulated value if it is success.
 */
@Suppress("Unused_parameter")
@Deprecated(message = "Use getOrElse instead.", level = DeprecationLevel.ERROR, replaceWith = ReplaceWith("getOrElse"))
inline fun <R, S : R, F : Throwable> Try<S, F>.recover(transform: (exception: F) -> R): Try<R, Nothing> =
    TODO()
