/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.pager

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import org.readium.r2.shared.Publication


class R2PagerAdapter(fm: FragmentManager, private val mItems: List<String>, private val title: String, private val type: Publication.TYPE, private val publicationPath: String) : R2FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment =
            when (type) {
                Publication.TYPE.EPUB -> R2EpubPageFragment.newInstance(mItems[position], title)
                Publication.TYPE.CBZ -> R2CbzPageFragment.newInstance(publicationPath, mItems[position])
            }

    override fun getCount(): Int {
        return mItems.size
    }

}