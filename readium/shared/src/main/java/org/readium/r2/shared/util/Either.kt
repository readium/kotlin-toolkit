package org.readium.r2.shared.util

import org.readium.r2.shared.InternalReadiumApi

@InternalReadiumApi
sealed class Either<A, B> {
    class Left<A, B>(val left: A) : Either<A, B>()
    class Right<A, B>(val right: B) : Either<A, B>()
}
