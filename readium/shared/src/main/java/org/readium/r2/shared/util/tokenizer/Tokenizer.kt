/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.tokenizer

import org.readium.r2.shared.ExperimentalReadiumApi

/** A tokenizer splits a piece of data [D] into a list of [T] tokens. */
@ExperimentalReadiumApi
fun interface Tokenizer<D, T> {
    fun tokenize(data: D): List<T>
}
