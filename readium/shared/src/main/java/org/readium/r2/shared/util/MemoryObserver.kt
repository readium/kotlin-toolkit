package org.readium.r2.shared.util

import android.content.ComponentCallbacks2
import android.content.res.Configuration
import org.readium.r2.shared.InternalReadiumApi

/**
 * A memory observer reacts to a device reclaiming memory by releasing unused resources.
 */
@InternalReadiumApi
interface MemoryObserver {

    /**
     * Level of memory trim.
     */
    enum class Level {
        Moderate, Low, Critical;

        companion object {
            fun fromLevel(level: Int): Level =
                when {
                    level <= ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> Moderate
                    level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> Low
                    else -> Critical
                }
        }
    }

    /**
     * Called by the system when memory needs to be freed.
     *
     * Release unused resources according to the level.
     */
    fun onTrimMemory(level: Level)

    companion object {
        /**
         * Wraps the given [observer] into a [ComponentCallbacks2] usable with Android's [Context].
         */
        fun asComponentCallbacks2(observer: MemoryObserver): ComponentCallbacks2 =
            object : ComponentCallbacks2 {
                override fun onConfigurationChanged(config: Configuration) {}
                override fun onLowMemory() {}

                override fun onTrimMemory(level: Int) {
                    observer.onTrimMemory(Level.fromLevel(level))
                }
            }
    }
}