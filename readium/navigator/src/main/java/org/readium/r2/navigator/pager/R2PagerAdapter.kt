package org.readium.r2.navigator.pager

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager


class R2PagerAdapter internal constructor(fm: FragmentManager, private val mItems: List<String>) : R2FragmentPagerAdapter(fm) {


    override fun getItem(position: Int): Fragment {
        return R2PageFragment.newInstance(mItems[position])
    }

    override fun getCount(): Int {
        return mItems.size
    }

}