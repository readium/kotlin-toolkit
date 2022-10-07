/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.readium.r2.navigator.ExperimentalDecorator
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.html.HtmlDecorationTemplate
import org.readium.r2.navigator.html.toCss
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.testapp.R
import org.readium.r2.testapp.search.SearchFragment

@OptIn(ExperimentalReadiumApi::class, ExperimentalDecorator::class)
class EpubReaderFragment : VisualReaderFragment(), EpubNavigatorFragment.Listener {

    override lateinit var navigator: Navigator
    private lateinit var navigatorFragment: EpubNavigatorFragment

    private lateinit var menuSearch: MenuItem
    lateinit var menuSearchView: SearchView

    private var isSearchViewIconified = true

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            isSearchViewIconified = savedInstanceState.getBoolean(IS_SEARCH_VIEW_ICONIFIED)
        }

        val readerData = model.readerInitData as VisualReaderInitData

        childFragmentManager.fragmentFactory =
            EpubNavigatorFragment.createFactory(
                publication = publication,
                initialLocator = readerData.initialLocation,
                listener = this,
                config = EpubNavigatorFragment.Configuration(
                    preferences = model.settings.preferences.value,
                    // App assets which will be accessible from the EPUB resources.
                    // You can use simple glob patterns, such as "images/.*" to allow several
                    // assets in one go.
                    servedAssets = listOf(
                        /** Icon for the annotation side mark, see [annotationMarkTemplate]. */
                        "annotation-icon.svg"
                    )
                ).apply {
                    // Register the HTML template for our custom [DecorationStyleAnnotationMark].
                    decorationTemplates[DecorationStyleAnnotationMark::class] = annotationMarkTemplate()
                    selectionActionModeCallback = customSelectionActionModeCallback
                }
            )

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

        setHasOptionsMenu(true)

        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.settings.theme
                    .onEach { theme ->
                        navigatorFragment.resourcePager.setBackgroundColor(theme.backgroundColor)
                    }
                    .launchIn(this)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater)

        menuSearch = menu.findItem(R.id.search).apply {
            isVisible = true
            menuSearchView = actionView as SearchView
        }

        connectSearch()
        if (!isSearchViewIconified) menuSearch.expandActionView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(IS_SEARCH_VIEW_ICONIFIED, isSearchViewIconified)
    }

    private fun connectSearch() {
        menuSearch.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {

            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                if (isSearchViewIconified) { // It is not a state restoration.
                    showSearchFragment()
                }

                isSearchViewIconified = false
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
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

            (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.showSoftInput(
                this.view, InputMethodManager.SHOW_FORCED
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.search -> {
                super.onOptionsItemSelected(item)
            }
            android.R.id.home -> {
                menuSearch.collapseActionView()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    private fun showSearchFragment() {
        childFragmentManager.commit {
            childFragmentManager.findFragmentByTag(SEARCH_FRAGMENT_TAG)?.let { remove(it) }
            add(R.id.fragment_reader_container, SearchFragment::class.java, Bundle(), SEARCH_FRAGMENT_TAG)
            hide(navigatorFragment)
            addToBackStack(SEARCH_FRAGMENT_TAG)
        }
    }

    companion object {
        private const val SEARCH_FRAGMENT_TAG = "search"
        private const val IS_SEARCH_VIEW_ICONIFIED = "isSearchViewIconified"
    }
}

/**
 * Example of an HTML template for a custom Decoration Style.
 *
 * This one will display a tinted "pen" icon in the page margin to show that a highlight has an
 * associated note.
 *
 * Note that the icon is served from the app assets folder.
 */
@OptIn(ExperimentalDecorator::class)
private fun annotationMarkTemplate(@ColorInt defaultTint: Int = Color.YELLOW): HtmlDecorationTemplate {
    val className = "testapp-annotation-mark"
    val iconUrl = EpubNavigatorFragment.assetUrl("annotation-icon.svg")
    return HtmlDecorationTemplate(
        layout = HtmlDecorationTemplate.Layout.BOUNDS,
        width = HtmlDecorationTemplate.Width.PAGE,
        element = { decoration ->
            val style = decoration.style as? DecorationStyleAnnotationMark
            val tint = style?.tint ?: defaultTint
            // Using `data-activable=1` prevents the whole decoration container from being
            // clickable. Only the icon will respond to activation events.
            """
            <div><div data-activable="1" class="$className" style="background-color: ${tint.toCss()} !important"/></div>"
            """
        },
        stylesheet = """
            .$className {
                float: left;
                margin-left: 8px;
                width: 30px;
                height: 30px;
                border-radius: 50%;
                background: url('$iconUrl') no-repeat center;
                background-size: auto 50%;
                opacity: 0.8;
            }
            """
    )
}
