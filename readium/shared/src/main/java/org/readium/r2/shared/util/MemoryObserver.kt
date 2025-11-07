package org.readium.r2.shared.util

import android.content.ComponentCallbacks2
import android.content.res.Configuration
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

        public companion object {
            public fun fromLevel(level: Int): Level =
                when (level) {
                    ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> Background
                    ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> UiHidden
                    else -> Background
                }
        }
    }

    /**
     * Called by the system when memory needs to be freed.
     *
     * Release unused resources according to the level.
     */
    public fun onTrimMemory(level: Level)

    public companion object {
        /**
         * Wraps the given [observer] into a [ComponentCallbacks2] usable with Android's Context.
         */
        public fun asComponentCallbacks2(observer: MemoryObserver): ComponentCallbacks2 =
            object : ComponentCallbacks2 {
                override fun onConfigurationChanged(config: Configuration) {}

                @Suppress("OVERRIDE_DEPRECATION")
                @Deprecated("Deprecated in Java")
                override fun onLowMemory() {}

                override fun onTrimMemory(level: Int) {
                    observer.onTrimMemory(Level.fromLevel(level))
                }
            }
    }
}
