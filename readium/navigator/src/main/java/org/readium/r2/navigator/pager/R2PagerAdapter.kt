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
import android.os.Parcelable
import android.view.ViewGroup
import androidx.collection.LongSparseArray
import androidx.collection.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import org.readium.r2.navigator.extensions.let
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url

internal class R2PagerAdapter internal constructor(
    val fm: FragmentManager,
    private val resources: List<PageResource>,
) : R2FragmentPagerAdapter(fm) {

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

    fun getCurrentFragment(): Fragment? {
        return currentFragment
    }

    fun getPreviousFragment(): Fragment? {
        return previousFragment
    }

    fun getNextFragment(): Fragment? {
        return nextFragment
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
        if (getCurrentFragment() !== `object`) {
            currentFragment = `object` as Fragment
            nextFragment = mFragments.get(getItemId(position + 1))
            previousFragment = mFragments.get(getItemId(position - 1))
        }
        super.setPrimaryItem(container, position, `object`)
    }

    internal fun getResource(position: Int): PageResource? =
        resources.getOrNull(position)

    override fun getItem(position: Int): Fragment {
        val locator = popPendingLocatorAt(getItemId(position))
        val fragment = when (val resource = resources[position]) {
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
                fm.fragmentFactory
                    .instantiate(
                        ClassLoader.getSystemClassLoader(),
                        R2CbzPageFragment::class.java.name
                    )
                    .also {
                        it.arguments = Bundle().apply {
                            putParcelable("link", resource.link)
                        }
                    }
            }
        }
        listener?.onCreatePageFragment(fragment)
        return fragment
    }

    override fun getCount(): Int {
        return resources.size
    }

    override fun restoreState(state: Parcelable?, loader: ClassLoader?) {
        super.restoreState(state, loader)

        pendingLocators.forEach { i, locator ->
            (mFragments.get(i) as? R2EpubPageFragment)?.loadLocator(locator)
        }
        pendingLocators.clear()
    }

    private val pendingLocators = LongSparseArray<Locator>()

    /**
     * Loads the given [Locator] in the page fragment at the given position. If not loaded, it
     * will be used when the fragment will be created.
     */
    internal fun loadLocatorAt(position: Int, locator: Locator) {
        val id = getItemId(position)
        val fragment = mFragments.get(id)
        if (fragment == null) {
            pendingLocators.put(id, locator)
        } else {
            (fragment as? R2EpubPageFragment)?.loadLocator(locator)
        }
    }

    private fun popPendingLocatorAt(id: Long): Locator? =
        pendingLocators.get(id)
            .also { pendingLocators.remove(id) }
}
