package org.readium.r2.navigator.pager

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import org.readium.r2.shared.PUBLICATION_TYPE


class R2PagerAdapter (fm: FragmentManager, private val mItems: List<String>, private val title: String, private val type: PUBLICATION_TYPE, private val publicationPath: String) : R2FragmentPagerAdapter(fm) {

    private val TAG = this::class.java.simpleName

    override fun getItem(position: Int): Fragment =
            when (type) {
                PUBLICATION_TYPE.EPUB -> R2EpubPageFragment.newInstance(mItems[position], title)
                PUBLICATION_TYPE.CBZ -> R2CbzPageFragment.newInstance(publicationPath, mItems[position])
            }

    override fun getCount(): Int {
        return mItems.size
    }

}