/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.pager

import android.content.Context
import android.util.AttributeSet
import org.readium.r2.shared.InternalReadiumApi

// See https://youtrack.jetbrains.com/issue/KTLC-271 for visibility issue.
@InternalReadiumApi
public class R2ViewPager : R2RTLViewPager2 {

    internal enum class PublicationType {
        EPUB,
        CBZ,
        FXL,
        WEBPUB,
        AUDIO,
        DiViNa,
    }

    internal lateinit var publicationType: PublicationType

    internal constructor(context: Context) : super(context)
    internal constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    /**
     * Set adapter with automatic state restoration
     */
    internal fun setAdapter(newAdapter: R2PagerAdapter?) {
        // Use parent's adapter property instead of direct assignment
        super.adapter = newAdapter

        if (newAdapter != null) {
            // Post to next frame to ensure ViewPager2 is fully initialized
            post {
                restoreAdapterState()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Restore state when view is attached to window (e.g., after configuration change)
        post {
            restoreAdapterState()
        }
    }

    // Cast adapter to R2PagerAdapter for convenience
    internal val r2Adapter: R2PagerAdapter?
        get() = adapter as? R2PagerAdapter

    /**
     * Restore pending state after configuration changes or app restoration
     */
    internal fun restoreAdapterState() {
        r2Adapter?.restoreState()
    }
}
