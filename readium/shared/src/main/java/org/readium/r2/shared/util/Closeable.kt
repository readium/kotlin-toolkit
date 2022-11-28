/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

/**
 * A [Closeable] is an object holding closeable resources, such as open files or streams.
 */
interface Closeable {
    /**
     * Closes this object and releases any resources associated with it.
     * If the object is already closed then invoking this method has no effect.
     */
    fun close()
}

/**
 * A [SuspendingCloseable] is an object holding closeable resources, such as open files or streams.
 */
interface SuspendingCloseable {

    /**
     * Closes this object and releases any resources associated with it.
     * If the object is already closed then invoking this method has no effect.
     */
    suspend fun close()
}

/**
 * Executes the given block function on this resource and then closes it down correctly whether
 * an exception is thrown or not.
 */
suspend inline fun <T : SuspendingCloseable?, R> T.use(block: (T) -> R): R {
    var exception: Throwable? = null
    try {
        return block(this)
    } catch (e: Throwable) {
        exception = e
        throw e
    } finally {
        if (exception == null)
            this?.close()
        else
            try {
                this?.close()
            } catch (closeException: Throwable) {
                exception.addSuppressed(closeException)
            }
    }
}
