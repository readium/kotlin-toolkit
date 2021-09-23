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
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import org.readium.r2.shared.publication.Link


class R2PagerAdapter internal constructor(val fm: FragmentManager, private val resources: List<PageResource>) : R2FragmentPagerAdapter(fm) {

    internal sealed class PageResource {
        data class EpubReflowable(val link: Link, val url: String) : PageResource()
        data class EpubFxl(val url1: String, val url2: String? = null) : PageResource()
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

    override fun getItem(position: Int): Fragment =
        when (val resource = resources[position]) {
            is PageResource.EpubReflowable -> {
                R2EpubPageFragment.newInstance(resource.url, resource.link)
            }
            is PageResource.EpubFxl -> {
                R2FXLPageFragment.newInstance(resource.url1, resource.url2)
            }
            is PageResource.Cbz -> {
                fm.fragmentFactory
                    .instantiate(ClassLoader.getSystemClassLoader(), R2CbzPageFragment::class.java.name)
                    .also {
                        it.arguments = Bundle().apply {
                            putParcelable("link", resource.link)
                        }
                    }
            }
        }

    override fun getCount(): Int {
        return resources.size
    }

}
