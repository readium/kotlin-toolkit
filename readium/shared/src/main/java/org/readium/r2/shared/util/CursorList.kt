package org.readium.r2.shared.util

import org.readium.r2.shared.InternalReadiumApi

/**
 * A [List] with a mutable cursor index.
 */
@InternalReadiumApi
class CursorList<E>(
    private val list: List<E> = emptyList(),
    startIndex: Int = 0
) : List<E> by list {

    private var index: Int = startIndex - 1

    fun hasPrevious(): Boolean {
        return index > 0
    }

    /**
     * Moves the cursor backward and returns the element, or null when reaching the beginning.
     */
    fun previous(): E? {
        if (!hasPrevious())
            return null

        index--
        return list[index]
    }

    fun hasNext(): Boolean {
        return index + 1 < list.size
    }

    /**
     * Moves the cursor forward and returns the element, or null when reaching the end.
     */
    fun next(): E? {
        if (!hasNext())
            return null

        index++
        return list[index]
    }
}
