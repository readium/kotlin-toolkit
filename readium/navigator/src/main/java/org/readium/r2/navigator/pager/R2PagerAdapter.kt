/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.pager

import android.os.Bundle
import androidx.collection.LongSparseArray
import androidx.fragment.app.Fragment
import org.readium.r2.navigator.extensions.let
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url

internal class R2PagerAdapter internal constructor(
    fragment: Fragment,
    private val resources: List<PageResource>,
) : R2FragmentPagerAdapter(fragment) {

    internal interface Listener {
        fun onCreatePageFragment(fragment: Fragment) {}
    }

    internal var listener: Listener? = null

    internal sealed class PageResource {
        data class EpubReflowable(val link: Link, val url: AbsoluteUrl, val positionCount: Int) : PageResource()
        data class EpubFxl(
            val leftLink: Link? = null,
            val leftUrl: Url? = null,
            val rightLink: Link? = null,
            val rightUrl: Url? = null,
        ) : PageResource()
        data class Cbz(val link: Link) : PageResource()
    }

    private var currentFragment: Fragment? = null
    private var previousFragment: Fragment? = null
    private var nextFragment: Fragment? = null

    fun getCurrentFragment(): Fragment? = currentFragment
    fun getPreviousFragment(): Fragment? = previousFragment
    fun getNextFragment(): Fragment? = nextFragment

    internal fun getResource(position: Int): PageResource? = resources.getOrNull(position)

    override fun getItem(position: Int): Fragment {
        val locator = popPendingLocatorAt(getItemId(position))

        val resource = resources[position]

        val fragment = when (resource) {
            is PageResource.EpubReflowable -> {
                R2EpubPageFragment.newInstance(
                    resource.url,
                    resource.link,
                    initialLocator = locator,
                    positionCount = resource.positionCount
                )
            }
            is PageResource.EpubFxl -> {
                R2FXLPageFragment.newInstance(
                    left = let(resource.leftLink, resource.leftUrl) { l, u -> Pair(l, u) },
                    right = let(resource.rightLink, resource.rightUrl) { l, u -> Pair(l, u) }
                )
            }
            is PageResource.Cbz -> {
                mFragmentManager.fragmentFactory
                    .instantiate(
                        ClassLoader.getSystemClassLoader(),
                        R2CbzPageFragment::class.java.name
                    ).apply {
                        arguments = Bundle().apply {
                            putParcelable("link", resource.link)
                        }
                    }
            }
        }

        listener?.onCreatePageFragment(fragment)
        return fragment
    }

    override fun getItemCount(): Int = resources.size

    private val pendingLocators = LongSparseArray<Locator>()

    override fun onRecyclerViewAttached() {
        super.onRecyclerViewAttached()
        // Restore pending locators to fragments that are now available
        restorePendingLocators()
    }

    private fun restorePendingLocators() {
        // Process all pending locators and apply them to existing fragments
        for (i in 0 until pendingLocators.size()) {
            val id = pendingLocators.keyAt(i)
            val locator = pendingLocators.valueAt(i)
            val fragment = mFragments.get(id)
            if (fragment != null) {
                (fragment as? R2EpubPageFragment)?.loadLocator(locator)
            }
        }
        // Don't clear here as fragments might not be ready yet
    }

    internal fun loadLocatorAt(position: Int, locator: Locator) {
        val id = getItemId(position)
        val fragment = mFragments.get(id)
        if (fragment == null) {
            pendingLocators.put(id, locator)
        } else {
            (fragment as? R2EpubPageFragment)?.loadLocator(locator)
            // Remove from pending since it's been applied
            pendingLocators.remove(id)
        }
    }

    /**
     * Force restoration of pending locators - useful after configuration changes
     */
    internal fun restoreState() {
        restorePendingLocators()
        // Clear pending locators that have been successfully applied
        val toRemove = mutableListOf<Long>()
        for (i in 0 until pendingLocators.size()) {
            val id = pendingLocators.keyAt(i)
            val fragment = mFragments[id]
            if (fragment != null) {
                toRemove.add(id)
            }
        }
        toRemove.forEach { pendingLocators.remove(it) }
    }

    private fun popPendingLocatorAt(id: Long): Locator? =
        pendingLocators[id]
            .also { pendingLocators.remove(id) }
}
