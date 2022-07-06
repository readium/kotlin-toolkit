package org.readium.r2.shared.util

sealed class Either<A, B> {
    class Left<A, B>(val value: A) : Either<A, B>()
    class Right<A, B>(val value: B) : Either<A, B>()

    val left: A?
        get() = (this as? Left)?.value

    val right: B?
        get() = (this as? Right)?.value

    inline fun onLeft(action: (value: A) -> Unit): Either<A, B> {
        (this as? Left)?.let {
            action(it.value)
        }
        return this
    }

    inline fun onRight(action: (value: B) -> Unit): Either<A, B> {
        (this as? Right)?.let {
            action(it.value)
        }
        return this
    }
}
