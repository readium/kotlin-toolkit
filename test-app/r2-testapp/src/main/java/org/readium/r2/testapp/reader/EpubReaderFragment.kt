/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import android.app.ProgressDialog
import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.core.content.edit
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.pager.R2EpubPageFragment
import org.readium.r2.navigator.pager.R2PagerAdapter
import org.readium.r2.shared.APPEARANCE_REF
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.presentation
import org.readium.r2.testapp.BuildConfig
import org.readium.r2.testapp.R
import org.readium.r2.testapp.epub.EpubActivity
import org.readium.r2.testapp.search.MarkJsSearchEngine
import org.readium.r2.testapp.search.SearchFragment
import org.readium.r2.testapp.search.SearchViewModel
import org.readium.r2.testapp.tts.ScreenReaderContract
import org.readium.r2.testapp.tts.ScreenReaderFragment
import org.readium.r2.testapp.utils.toggleSystemUi
import timber.log.Timber
import java.net.URL

class EpubReaderFragment : VisualReaderFragment(), EpubNavigatorFragment.Listener {

    override lateinit var model: ReaderViewModel
    override lateinit var navigator: Navigator
    private lateinit var publication: Publication
    lateinit var navigatorFragment: EpubNavigatorFragment

    private lateinit var menuScreenReader: MenuItem
    private lateinit var menuSearch: MenuItem
    lateinit var menuSearchView: SearchView

    private var isScreenReaderVisible = false
    private var isSearchViewIconified = true
    lateinit var searchResult: MutableLiveData<List<Locator>>

    private val activity: EpubActivity
        get() = requireActivity() as EpubActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            isScreenReaderVisible = savedInstanceState.getBoolean(IS_SCREEN_READER_VISIBLE_KEY)
            isSearchViewIconified = savedInstanceState.getBoolean(IS_SEARCH_VIEW_ICONIFIED)
        }

        ViewModelProvider(requireActivity()).get(ReaderViewModel::class.java).let {
            model = it
            publication = it.publication
        }

        val baseUrl = checkNotNull(requireArguments().getString(BASE_URL_ARG))

        childFragmentManager.fragmentFactory =
            EpubNavigatorFragment.createFactory(publication, baseUrl, model.initialLocation, this)

        childFragmentManager.setFragmentResultListener(
            SearchFragment::class.java.name,
            this,
            FragmentResultListener { _, result ->
                val locator = result.getParcelable<Locator>(SearchFragment::class.java.name)!!
                closeSearchFragment(locator)
            }
        )

        childFragmentManager.setFragmentResultListener(
            ScreenReaderContract.REQUEST_KEY,
            this,
            FragmentResultListener { _, result ->
                val locator = ScreenReaderContract.parseResult(result).locator
                if (locator.href != navigator.currentLocator.value.href) {
                    navigator.go(locator)
                }
            }
        )

        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        val navigatorFragmentTag = getString(R.string.epub_navigator_tag)

        if (savedInstanceState == null) {
            childFragmentManager.commitNow {
                add(R.id.fragment_reader_container, EpubNavigatorFragment::class.java, Bundle(), navigatorFragmentTag)
            }
        }
        navigator = childFragmentManager.findFragmentByTag(navigatorFragmentTag) as Navigator
        navigatorFragment = navigator as EpubNavigatorFragment
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

       // This is a hack to draw the right background color on top and bottom blank spaces
        navigatorFragment.lifecycleScope.launchWhenStarted {
            val appearancePref = activity.preferences.getInt(APPEARANCE_REF, 0)
            val backgroundsColors = mutableListOf("#ffffff", "#faf4e8", "#000000")
            navigatorFragment.resourcePager.setBackgroundColor(Color.parseColor(backgroundsColors[appearancePref]))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater)
        menuInflater.inflate(R.menu.menu_epub, menu)

        menuScreenReader = menu.findItem(R.id.screen_reader)
        menuSearch = menu.findItem(R.id.search)
        menuSearchView = menuSearch.actionView as SearchView

        ViewModelProvider(this).get(SearchViewModel::class.java).let {
            searchResult = it.result
        }
        connectSearch()
        if (!isSearchViewIconified) menuSearch.expandActionView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(IS_SCREEN_READER_VISIBLE_KEY, isScreenReaderVisible)
        outState.putBoolean(IS_SEARCH_VIEW_ICONIFIED, isSearchViewIconified)
    }

    private fun connectSearch() {
        val searchStorage = requireActivity().getSharedPreferences("org.readium.r2.search", Context.MODE_PRIVATE)
        val markJsSearchEngine = MarkJsSearchEngine(activity)
        val bookId = checkNotNull(requireArguments().getLong(BOOK_ID_ARG))

        menuSearch.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {

            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                if (isSearchViewIconified) { // It is not a state restoration.
                    showSearchFragment()
                }

                isSearchViewIconified = false
                // Try to preload all resources once the SearchView has been expanded
                // Delay to allow the keyboard to be shown immediately
                Handler(Looper.getMainLooper()).postDelayed({
                    navigatorFragment.resourcePager.offscreenPageLimit = publication.readingOrder.size
                }, 100)

                val previouslySearchBook = searchStorage.getLong("book", -1)
                if (previouslySearchBook == bookId) {
                    // Load previous research
                    searchStorage.getString("term", null)?.let {
                        // SearchView.setQuery doesn't work until the SearchView has been expanded
                        Handler(Looper.getMainLooper()).post {
                            menuSearchView.setQuery(it, false)
                            menuSearchView.clearFocus()
                        }
                    }
                    // Load previous result
                    searchStorage.getString("result", null)?.let {
                        searchResult.value = (Gson().fromJson(it, Array<Locator>::class.java)).toList()
                    }
                }
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                isSearchViewIconified = true
                childFragmentManager.popBackStack()
                Handler(Looper.getMainLooper()).postDelayed({
                    navigatorFragment.resourcePager.offscreenPageLimit = 1
                }, 100)
                menuSearchView.clearFocus()

                return true
            }
        })

        menuSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String): Boolean {
                val progress = ProgressDialog(requireContext()).apply {
                    setMessage(getString(R.string.progress_wait_while_searching_book))
                    isIndeterminate = true
                    show()
                }

                // Ensure all resources are loaded
                navigatorFragment.resourcePager.offscreenPageLimit = publication.readingOrder.size

                searchResult.value = emptyList()
                // FIXME: the search should be somehow tied to the lifecycle of the SearchResultFragment
                Handler(Looper.getMainLooper()).postDelayed({
                    markJsSearchEngine.search(query) { (last, result) ->
                        searchResult.value = result
                        progress.dismiss()

                        if (last) {
                            // Save query and result
                            val stringResults = Gson().toJson(result)
                            searchStorage.edit {
                                putString("result", stringResults)
                                putString("term", query)
                                putLong("book", bookId)
                            }
                        }
                    }
                }, 500)
                menuSearchView.clearFocus()

                return false
            }

            override fun onQueryTextChange(s: String): Boolean {
                return false
            }
        })

        menuSearchView.findViewById<ImageView>(R.id.search_close_btn).setOnClickListener {
            menuSearchView.requestFocus()
            searchResult.value = emptyList()
            menuSearchView.setQuery("", false)

            (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).toggleSoftInput(
                InputMethodManager.SHOW_FORCED,
                InputMethodManager.HIDE_IMPLICIT_ONLY
            )

           searchStorage.edit {
                remove("result")
                remove("term")
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (super.onOptionsItemSelected(item)) {
            return true
        }

       return when (item.itemId) {
           R.id.settings -> {
               activity.userSettings.userSettingsPopUp().showAsDropDown(
                   requireActivity().findViewById(R.id.settings),
                   0,
                   0,
                   Gravity.END
               )
               true
           }
           R.id.search -> {
               super.onOptionsItemSelected(item)
           }

           android.R.id.home -> {
               menuSearch.collapseActionView()
               true
           }

           R.id.screen_reader -> {
               if (isScreenReaderVisible) {
                   closeScreenReaderFragment()
               } else {
                   showScreenReaderFragment()
               }
               true
           }
            else -> false
        }
    }

    override fun onTap(point: PointF): Boolean {
        requireActivity().toggleSystemUi()
        return true
    }

    private fun showSearchFragment() {
        childFragmentManager.commit {
            add(R.id.fragment_reader_container, SearchFragment::class.java, Bundle(), SEARCH_FRAGMENT_TAG)
            hide(navigatorFragment)
            addToBackStack(SEARCH_FRAGMENT_TAG)
        }
    }

    private fun closeSearchFragment(locator: Locator) {
        menuSearch.collapseActionView()
        locator.locations.fragments.firstOrNull()?.let { fragment ->
            val fragments = fragment.split(",")
                .map { it.split("=") }
                .filter { it.size == 2 }
                .associate { it[0] to it[1] }

            val index = fragments["i"]?.toInt()
            if (index != null) {
                val searchStorage = activity.getSharedPreferences("org.readium.r2.search", Context.MODE_PRIVATE)
                Handler(Looper.getMainLooper()).postDelayed({
                    if (publication.metadata.presentation.layout == EpubLayout.REFLOWABLE) {
                        val currentFragment = (navigatorFragment.resourcePager.adapter as R2PagerAdapter).getCurrentFragment() as R2EpubPageFragment
                        val resource = publication.readingOrder[navigatorFragment.resourcePager.currentItem]
                        val resourceHref = resource.href
                        val resourceType = resource.type ?: ""
                        val resourceTitle = resource.title ?: ""

                        currentFragment.webView?.runJavaScript("markSearch('${searchStorage.getString("term", null)}', null, '$resourceHref', '$resourceType', '$resourceTitle', '$index')") { result ->
                            if (BuildConfig.DEBUG) Timber.d("###### $result")

                        }
                    }
                }, 1200)
            }
        }
    }

    private fun showScreenReaderFragment() {
        menuScreenReader.title = resources.getString(R.string.epubactivity_read_aloud_stop)
        isScreenReaderVisible = true
        val arguments = ScreenReaderContract.createArguments(navigator.currentLocator.value)
        childFragmentManager.commit {
            add(R.id.fragment_reader_container, ScreenReaderFragment::class.java, arguments)
            hide(navigatorFragment)
            addToBackStack(null)
        }
    }

    private fun closeScreenReaderFragment() {
        menuScreenReader.title = resources.getString(R.string.epubactivity_read_aloud_start)
        isScreenReaderVisible = false
        childFragmentManager.popBackStack()
    }

    companion object {

        private const val BASE_URL_ARG = "baseUrl"
        private const val BOOK_ID_ARG = "bookId"

        private const val SEARCH_FRAGMENT_TAG = "search"

        private const val IS_SCREEN_READER_VISIBLE_KEY = "isScreenReaderVisible"

        private const val IS_SEARCH_VIEW_ICONIFIED = "isSearchViewIconified"

        fun newInstance(baseUrl: URL, bookId: Long): EpubReaderFragment {
            return EpubReaderFragment().apply {
                arguments = Bundle().apply {
                    putString(BASE_URL_ARG, baseUrl.toString())
                    putLong(BOOK_ID_ARG, bookId)
                }
            }
        }
    }
}