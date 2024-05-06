package org.readium.r2.navigator

import org.readium.r2.shared.util.Try

fun <S, F> Try<S, F>.require(): S =
    fold(
        { it },
        { throw IllegalStateException("Expected a result, got: $it") }
    )
