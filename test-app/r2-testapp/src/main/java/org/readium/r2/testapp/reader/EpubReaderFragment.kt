/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.APPEARANCE_REF
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.testapp.R
import org.readium.r2.testapp.epub.EpubActivity
import org.readium.r2.testapp.search.SearchFragment
import org.readium.r2.testapp.tts.ScreenReaderContract
import org.readium.r2.testapp.tts.ScreenReaderFragment
import org.readium.r2.testapp.utils.toggleSystemUi
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
                menuSearch.collapseActionView()
                result.getParcelable<Locator>(SearchFragment::class.java.name)?.let {
                    navigatorFragment.go(it)
                }
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

        connectSearch()
        if (!isSearchViewIconified) menuSearch.expandActionView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(IS_SCREEN_READER_VISIBLE_KEY, isScreenReaderVisible)
        outState.putBoolean(IS_SEARCH_VIEW_ICONIFIED, isSearchViewIconified)
    }

    private fun connectSearch() {
        menuSearch.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {

            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                if (isSearchViewIconified) { // It is not a state restoration.
                    showSearchFragment()
                }

                isSearchViewIconified = false
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                isSearchViewIconified = true
                childFragmentManager.popBackStack()
                menuSearchView.clearFocus()

                return true
            }
        })

        menuSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String): Boolean {
                model.search(query)
                menuSearchView.clearFocus()

                return false
            }

            override fun onQueryTextChange(s: String): Boolean {
                return false
            }
        })

        menuSearchView.findViewById<ImageView>(R.id.search_close_btn).setOnClickListener {
            menuSearchView.requestFocus()
            model.cancelSearch()
            menuSearchView.setQuery("", false)

            (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).toggleSoftInput(
                InputMethodManager.SHOW_FORCED,
                InputMethodManager.HIDE_IMPLICIT_ONLY
            )
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
            childFragmentManager.findFragmentByTag(SEARCH_FRAGMENT_TAG)?.let { remove(it) }
            add(R.id.fragment_reader_container, SearchFragment::class.java, Bundle(), SEARCH_FRAGMENT_TAG)
            hide(navigatorFragment)
            addToBackStack(SEARCH_FRAGMENT_TAG)
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