/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.pager

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import org.readium.r2.shared.Publication


class R2PagerAdapter(fm: FragmentManager, private val resources: List<Any>, private val title: String, private val type: Publication.TYPE, private val publicationPath: String) : R2FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment =
            when (type) {
                Publication.TYPE.EPUB, Publication.TYPE.WEBPUB, Publication.TYPE.AUDIO -> {
                    val single = resources[position] as Pair<Int, String>
                    R2EpubPageFragment.newInstance(single.second, title)
                }
                Publication.TYPE.FXL -> {
                    if (resources[position] is Triple<*, *, *>) {
                        val double = resources[position] as Triple<Int, String, String>
                        R2FXLPageFragment.newInstance(double.second, double.third, title)
                    }
                    else {
                        val single = resources[position] as Pair<Int, String>
                        R2EpubPageFragment.newInstance(single.second, title)
                    }
                }
                Publication.TYPE.CBZ -> R2CbzPageFragment.newInstance(publicationPath, resources[position] as String)
            }

    override fun getCount(): Int {
        return resources.size
    }

}