package org.readium.r2.shared.util

import org.readium.r2.shared.InternalReadiumApi

/**
 * A memory observer reacts to a device reclaiming memory by releasing unused resources.
 */
@InternalReadiumApi
public interface MemoryObserver {

    /**
     * Level of memory trim.
     */
    public enum class Level {
        /**
         * The process has gone on to the LRU list. This is a good opportunity to clean up resources
         * that can efficiently and quickly be re-built if the user returns to the app.
         */
        Background,

        /**
         * The process had been showing a user interface, and is no longer doing so. Large
         * allocations with the UI should be released at this point to allow memory to be better
         * managed.
         */
        UiHidden,

        ;

        public companion object
    }

    /**
     * Called by the system when memory needs to be freed.
     *
     * Release unused resources according to the level.
     */
    public fun onTrimMemory(level: Level)

    public companion object
}
