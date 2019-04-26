/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.pager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import org.readium.r2.shared.Publication
import android.view.ViewGroup


class R2PagerAdapter(fm: androidx.fragment.app.FragmentManager, private val resources: List<Any>, private val title: String, private val type: Publication.TYPE, private val publicationPath: String) : R2FragmentPagerAdapter(fm) {

    private var currentFragment: androidx.fragment.app.Fragment? = null
    private var previousFragment: androidx.fragment.app.Fragment? = null
    private var nextFragment: androidx.fragment.app.Fragment? = null

    fun getCurrentFragment(): androidx.fragment.app.Fragment? {
        return currentFragment
    }

    fun getPreviousFragment(): androidx.fragment.app.Fragment? {
        return previousFragment
    }

    fun getNextFragment(): androidx.fragment.app.Fragment? {
        return nextFragment
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
        if (getCurrentFragment() !== `object`) {
            currentFragment = `object` as androidx.fragment.app.Fragment
            nextFragment = mFragments.get(getItemId(position + 1))
            previousFragment = mFragments.get(getItemId(position - 1))
        }
        super.setPrimaryItem(container, position, `object`)
    }

    override fun getItem(position: Int): androidx.fragment.app.Fragment =
            when (type) {
                Publication.TYPE.EPUB, Publication.TYPE.WEBPUB, Publication.TYPE.AUDIO -> {
                    val single = resources[position] as Pair<Int, String>
                    R2EpubPageFragment.newInstance(single.second, title)
                }
                Publication.TYPE.FXL -> {
                    if (resources[position] is Triple<*, *, *>) {
                        val double = resources[position] as Triple<Int, String, String>
                        R2FXLPageFragment.newInstance(title, double.second, double.third)
                    }
                    else {
                        val single = resources[position] as Pair<Int, String>
                        R2FXLPageFragment.newInstance(title, single.second)
                    }
                }
                Publication.TYPE.CBZ -> R2CbzPageFragment.newInstance(publicationPath, resources[position] as String)
                Publication.TYPE.DiViNa -> TODO()
            }

    override fun getCount(): Int {
        return resources.size
    }

}