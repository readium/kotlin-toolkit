package org.readium.r2.shared.util

import org.readium.r2.shared.InternalReadiumApi

/**
 * A [List] with a mutable cursor index.
 */
@InternalReadiumApi
class CursorList<E>(
    private val list: List<E> = emptyList(),
    private val startIndex: Int = 0
) : List<E> by list {
    private var index: Int? = null

    /**
     * Returns the current element.
     */
    fun current(): E? =
        moveAndGet(index ?: startIndex)

    /**
     * Moves the cursor backward and returns the element, or null when reaching the beginning.
     */
    fun previous(): E? =
        moveAndGet(index
            ?.let { it - 1}
            ?: startIndex
        )

    /**
     * Moves the cursor forward and returns the element, or null when reaching the end.
     */
    fun next(): E? =
        moveAndGet(index?.let { it + 1}
            ?: startIndex
        )

    private fun moveAndGet(index: Int): E? {
        if (!list.indices.contains(index)) {
            return null
        }
        this.index = index
        return get(index)
    }
}
