package org.readium.r2.navigator.cbz

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.viewpager.widget.ViewPager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.R
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.extensions.layoutDirectionIsRTL
import org.readium.r2.navigator.pager.R2CbzPageFragment
import org.readium.r2.navigator.pager.R2PagerAdapter
import org.readium.r2.navigator.pager.R2ViewPager
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.services.positions
import kotlin.coroutines.CoroutineContext

class ImageNavigatorFragment(
        internal val publication: Publication,
        private val initialLocator: Locator? = null,
        internal val listener: Navigator.Listener? = null
) : Fragment(), CoroutineScope, VisualNavigator {

    /**
     * Context of this scope.
     */
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    lateinit var positions: List<Locator>
    lateinit var resourcePager: R2ViewPager

    internal lateinit var preferences: SharedPreferences

    lateinit var adapter: R2PagerAdapter
    private lateinit var currentActivity: FragmentActivity

    override val currentLocator: LiveData<Locator?> get() = _currentLocator
    private val _currentLocator = MutableLiveData<Locator?>(null)

    var currentPagerPosition: Int = 0
    var resources: List<String> = emptyList()

    private class R2CbzPageFragmentFactory(
            private val publication: Publication
    ) : FragmentFactory() {
        override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
            return when (className) {
                R2CbzPageFragment::class.java.name -> R2CbzPageFragment(publication)
                else -> super.instantiate(classLoader, className)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        currentActivity = requireActivity()
        val view = inflater.inflate(R.layout.activity_r2_viewpager, container, false)

        preferences = requireContext().getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)
        resourcePager = view.findViewById(R.id.resourcePager)
        resourcePager.type = Publication.TYPE.CBZ

        positions = runBlocking { publication.positions() }

        val supportFragmentManager = currentActivity.supportFragmentManager
        supportFragmentManager.fragmentFactory = R2CbzPageFragmentFactory(publication)

        resourcePager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                notifyCurrentLocation()
            }
        })

        adapter = R2PagerAdapter(supportFragmentManager, publication.readingOrder, publication.metadata.title, Publication.TYPE.CBZ)

        resourcePager.adapter = adapter

        if (currentPagerPosition == 0) {
            if (requireActivity().layoutDirectionIsRTL()) {
                // The view has RTL layout
                resourcePager.currentItem = resources.size - 1
            } else {
                // The view has LTR layout
                resourcePager.currentItem = currentPagerPosition
            }
        } else {
            resourcePager.currentItem = currentPagerPosition
        }

        if (initialLocator != null) {
            go(initialLocator)
        }

        return view
    }

    override fun onStart() {
        super.onStart()

        // OnPageChangeListener.onPageSelected is not called on the first page of the book, so we
        // trigger the locationDidChange event manually.
        notifyCurrentLocation()
    }

    fun nextResource(v: View?) {
        launch {
            if (requireActivity().layoutDirectionIsRTL()) {
                // The view has RTL layout
                resourcePager.currentItem = resourcePager.currentItem - 1
            } else {
                // The view has LTR layout
                resourcePager.currentItem = resourcePager.currentItem + 1
            }

            notifyCurrentLocation()
        }
    }

    fun previousResource(v: View?) {
        launch {
            if (requireActivity().layoutDirectionIsRTL()) {
                // The view has RTL layout
                resourcePager.currentItem = resourcePager.currentItem + 1
            } else {
                // The view has LTR layout
                resourcePager.currentItem = resourcePager.currentItem - 1
            }

            notifyCurrentLocation()
        }
    }

    private fun notifyCurrentLocation() {
        val locator = positions[resourcePager.currentItem]
        if (locator == currentLocator.value) {
            return
        }

        _currentLocator.postValue(locator)
    }

    override val readingProgression: ReadingProgression
        get() = TODO("Not yet implemented")

    override fun goLeft(animated: Boolean, completion: () -> Unit): Boolean {
        TODO("Not yet implemented")
    }

    override fun goRight(animated: Boolean, completion: () -> Unit): Boolean {
        TODO("Not yet implemented")
    }

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean {
        val resourceIndex = publication.readingOrder.indexOfFirstWithHref(locator.href)
                ?: return false

        currentPagerPosition = resourceIndex
        resourcePager.currentItem = currentPagerPosition

        return true
    }

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean {
        TODO("Not yet implemented")
    }

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean {
        TODO("Not yet implemented")
    }

    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean {
        TODO("Not yet implemented")
    }

}
