@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.util

import android.content.ComponentCallbacks2
import android.content.res.Configuration
import org.readium.r2.shared.InternalReadiumApi

/**
 * Creates a [MemoryObserver.Level] from an Android `ComponentCallbacks2` trim level.
 */
public fun MemoryObserver.Level.Companion.fromLevel(level: Int): MemoryObserver.Level =
    when (level) {
        ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> MemoryObserver.Level.Background
        ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> MemoryObserver.Level.UiHidden
        else -> MemoryObserver.Level.Background
    }

/**
 * Wraps the given [observer] into a [ComponentCallbacks2] usable with Android's Context.
 */
public fun MemoryObserver.Companion.asComponentCallbacks2(observer: MemoryObserver): ComponentCallbacks2 =
    object : ComponentCallbacks2 {
        override fun onConfigurationChanged(config: Configuration) {}

        @Suppress("OVERRIDE_DEPRECATION")
        @Deprecated("Deprecated in Java")
        override fun onLowMemory() {}

        override fun onTrimMemory(level: Int) {
            observer.onTrimMemory(MemoryObserver.Level.fromLevel(level))
        }
    }
