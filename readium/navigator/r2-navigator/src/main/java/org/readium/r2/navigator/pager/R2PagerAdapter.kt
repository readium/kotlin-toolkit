package org.readium.r2.navigator.pager

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import org.readium.r2.shared.PUBLICATION_TYPE


class R2PagerAdapter (fm: FragmentManager, private val mItems: List<String>, val title: String, val type: PUBLICATION_TYPE) : R2FragmentPagerAdapter(fm) {

    private val TAG = this::class.java.simpleName

    override fun getItem(position: Int): Fragment =
            when (type) {
                PUBLICATION_TYPE.EPUB -> R2PageFragment.newInstance(mItems[position], title)
                PUBLICATION_TYPE.CBZ -> R2CbzPageFragment.newInstance(mItems[position], title)
            }

    override fun getCount(): Int {
        return mItems.size
    }

}