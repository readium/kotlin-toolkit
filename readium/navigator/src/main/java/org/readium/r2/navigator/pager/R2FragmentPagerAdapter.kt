package org.readium.r2.navigator.pager

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.support.v4.util.LongSparseArray
import android.support.v4.view.PagerAdapter
import android.util.Log
import android.view.View
import android.view.ViewGroup


abstract class R2FragmentPagerAdapter(private val mFragmentManager: FragmentManager) : PagerAdapter() {
    private val mFragments = LongSparseArray<Fragment>()
    private val mSavedStates = LongSparseArray<Fragment.SavedState>()
    private var mCurTransaction: FragmentTransaction? = null
    private var mCurrentPrimaryItem: Fragment? = null

    abstract fun getItem(position: Int): Fragment

    override fun startUpdate(container: ViewGroup) {
        if (container.id == View.NO_ID) {
            throw IllegalStateException("ViewPager with adapter " + this + " requires a view id")
        }
    }

    @SuppressLint("CommitTransaction")
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val tag = getItemId(position)
        var fragment: Fragment? = mFragments.get(tag)

        if (fragment != null) {
            return fragment
        }

        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction()
        }

        fragment = getItem(position)

        val savedState = mSavedStates.get(tag)
        if (savedState != null) {
            fragment.setInitialSavedState(savedState)
        }
        fragment.setMenuVisibility(false)
        fragment.userVisibleHint = false
        mFragments.put(tag, fragment)
        mCurTransaction!!.add(container.id, fragment, "f" + tag)

        return fragment
    }

    @SuppressLint("CommitTransaction")
    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        val fragment = `object` as Fragment
        val currentPosition = getItemPosition(fragment)

        val index = mFragments.indexOfValue(fragment)
        var fragmentKey: Long = -1
        if (index != -1) {
            fragmentKey = mFragments.keyAt(index)
            mFragments.removeAt(index)
        }


        if (fragment.isAdded && currentPosition != PagerAdapter.POSITION_NONE) {
            mSavedStates.put(fragmentKey, mFragmentManager.saveFragmentInstanceState(fragment))
        } else {
            mSavedStates.remove(fragmentKey)
        }

        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction()
        }

        mCurTransaction!!.remove(fragment)
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any?) {
        val fragment = `object` as Fragment?
        if (fragment !== mCurrentPrimaryItem) {
            if (mCurrentPrimaryItem != null) {
                mCurrentPrimaryItem!!.setMenuVisibility(false)
                mCurrentPrimaryItem!!.userVisibleHint = false
            }
            if (fragment != null) {
                fragment.setMenuVisibility(true)
                fragment.userVisibleHint = true
            }
            mCurrentPrimaryItem = fragment
        }
    }

    override fun finishUpdate(container: ViewGroup) {
        if (mCurTransaction != null) {
            mCurTransaction!!.commitNowAllowingStateLoss()
            mCurTransaction = null
        }
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return (`object` as Fragment).view === view
    }

    override fun saveState(): Parcelable? {
        var state: Bundle? = null
        if (mSavedStates.size() > 0) {

            state = Bundle()
            val stateIds = LongArray(mSavedStates.size())
            for (i in 0 until mSavedStates.size()) {
                val entry = mSavedStates.valueAt(i)
                stateIds[i] = mSavedStates.keyAt(i)
                state.putParcelable(java.lang.Long.toString(stateIds[i]), entry)
            }
            state.putLongArray("states", stateIds)
        }
        for (i in 0 until mFragments.size()) {
            val f = mFragments.valueAt(i)
            if (f != null && f.isAdded) {
                if (state == null) {
                    state = Bundle()
                }
                val key = "f" + mFragments.keyAt(i)
                mFragmentManager.putFragment(state, key, f)
            }
        }
        return state
    }

    override fun restoreState(state: Parcelable?, loader: ClassLoader?) {
        if (state != null) {
            val bundle = state as Bundle?
            bundle!!.classLoader = loader
            val fss = bundle.getLongArray("states")
            mSavedStates.clear()
            mFragments.clear()
            if (fss != null) {
                for (fs in fss) {
                    mSavedStates.put(fs, bundle.getParcelable<Parcelable>(java.lang.Long.toString(fs)) as Fragment.SavedState)
                }
            }
            val keys = bundle.keySet()
            for (key in keys) {
                if (key.startsWith("f")) {
                    val f = mFragmentManager.getFragment(bundle, key)
                    if (f != null) {
                        f.setMenuVisibility(false)
                        mFragments.put(java.lang.Long.parseLong(key.substring(1)), f)
                    } else {
                        Log.w(TAG, "Bad fragment at key " + key)
                    }
                }
            }
        }
    }


    fun getItemId(position: Int): Long {
        return position.toLong()
    }

    companion object {
        private val TAG = "FragmentPagerAdapter"
    }
}
