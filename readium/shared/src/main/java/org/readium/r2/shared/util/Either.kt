package org.readium.r2.shared.util

sealed class Either<A, B> {
    class Left<A, B>(val left: A) : Either<A, B>()
    class Right<A, B>(val right: B) : Either<A, B>()

    inline fun onLeft(action: (value: A) -> Unit): Either<A, B> {
        (this as? Left)?.let {
            action(it.left)
        }
        return this
    }

    inline fun onRight(action: (value: B) -> Unit): Either<A, B> {
        (this as? Right)?.let {
            action(it.right)
        }
        return this
    }
}
