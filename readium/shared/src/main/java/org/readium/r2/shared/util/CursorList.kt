/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import org.readium.r2.shared.InternalReadiumApi

/**
 * A [List] with a mutable cursor index.
 *
 * [next] and [previous] refer to the last element returned by a previous call
 * to any of both methods.
 *
 * @param list the content of the [CursorList]
 * @param index the index of the element that will initially be considered
 *   as the last returned element. May be -1 or the size of the list as well.
 */
@InternalReadiumApi
public class CursorList<E>(
    private val list: List<E> = emptyList(),
    private var index: Int = -1,
) : List<E> by list {

    init {
        check(index in -1..list.size)
    }

    public fun hasPrevious(): Boolean {
        return index > 0
    }

    /**
     * Moves the cursor backward and returns the element, or null when reaching the beginning.
     */
    public fun previous(): E? {
        if (!hasPrevious()) {
            return null
        }

        index--
        return list[index]
    }

    public fun hasNext(): Boolean {
        return index + 1 < list.size
    }

    /**
     * Moves the cursor forward and returns the element, or null when reaching the end.
     */
    public fun next(): E? {
        if (!hasNext()) {
            return null
        }

        index++
        return list[index]
    }
}
