/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mostapha Idoubihi, Paul Stoica
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.pager

import androidx.collection.LongSparseArray
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter

@Suppress("PROPERTY_HIDES_JAVA_FIELD")
internal abstract class R2FragmentPagerAdapter(
    private val fragment: Fragment,
) : FragmentStateAdapter(fragment) {

    // Keep FragmentManager for backward compatibility
    protected val mFragmentManager: FragmentManager = fragment.childFragmentManager

    val mFragments = LongSparseArray<Fragment>()

    abstract fun getItem(position: Int): Fragment

    override fun createFragment(position: Int): Fragment {
        val fragment = getItem(position)
        val itemId = getItemId(position)
        mFragments.put(itemId, fragment)

        return fragment
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        // Allow subclasses to handle state restoration after RecyclerView is attached
        onRecyclerViewAttached()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
    }

    /**
     * Called when the adapter is attached to RecyclerView.
     * Subclasses can override this to handle state restoration.
     */
    protected open fun onRecyclerViewAttached() {
        // Default implementation does nothing
    }

    /**
     * Utility method to get fragment by position if it exists
     */
    protected fun getFragmentAtPosition(position: Int): Fragment? {
        val id = getItemId(position)
        return mFragments[id]
    }
}
