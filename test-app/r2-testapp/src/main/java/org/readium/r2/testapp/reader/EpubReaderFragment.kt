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
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import org.readium.r2.navigator.ExperimentalDecorator
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.html.HtmlDecorationTemplate
import org.readium.r2.navigator.html.HtmlDecorationTemplates
import org.readium.r2.navigator.html.toCss
import org.readium.r2.shared.APPEARANCE_REF
import org.readium.r2.shared.ReadiumCSSName
import org.readium.r2.shared.SCROLL_REF
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.testapp.R
import org.readium.r2.testapp.R2App
import org.readium.r2.testapp.epub.UserSettings
import org.readium.r2.testapp.search.SearchFragment
import org.readium.r2.testapp.tts.ScreenReaderContract
import org.readium.r2.testapp.tts.ScreenReaderFragment
import org.readium.r2.testapp.utils.extensions.toDataUrl
import org.readium.r2.testapp.utils.toggleSystemUi
import java.net.URL

@OptIn(ExperimentalDecorator::class)
class EpubReaderFragment : VisualReaderFragment(), EpubNavigatorFragment.Listener {

    override lateinit var model: ReaderViewModel
    override lateinit var navigator: Navigator
    private lateinit var publication: Publication
    lateinit var navigatorFragment: EpubNavigatorFragment

    private lateinit var menuScreenReader: MenuItem
    private lateinit var menuSearch: MenuItem
    lateinit var menuSearchView: SearchView

    private lateinit var userSettings: UserSettings
    private var isScreenReaderVisible = false
    private var isSearchViewIconified = true

    // Accessibility
    private var isExploreByTouchEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        check(R2App.isServerStarted)

        val activity = requireActivity()

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
            EpubNavigatorFragment.createFactory(
                publication = publication,
                baseUrl = baseUrl,
                initialLocator = model.initialLocation,
                listener = this,
                config = EpubNavigatorFragment.Configuration().apply {
                    // Register the HTML template for our custom [DecorationStyleAnnotationMark].
                    decorationTemplates[DecorationStyleAnnotationMark::class] = annotationMarkTemplate(activity)
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

        val activity = requireActivity()
        userSettings = UserSettings(navigatorFragment.preferences, activity, publication.userSettingsUIPreset)

       // This is a hack to draw the right background color on top and bottom blank spaces
        navigatorFragment.lifecycleScope.launchWhenStarted {
            val appearancePref = navigatorFragment.preferences.getInt(APPEARANCE_REF, 0)
            val backgroundsColors = mutableListOf("#ffffff", "#faf4e8", "#000000")
            navigatorFragment.resourcePager.setBackgroundColor(Color.parseColor(backgroundsColors[appearancePref]))
        }
    }

    override fun onResume() {
        super.onResume()
        val activity = requireActivity()

        userSettings.resourcePager = navigatorFragment.resourcePager

        // If TalkBack or any touch exploration service is activated we force scroll mode (and
        // override user preferences)
        val am = activity.getSystemService(AppCompatActivity.ACCESSIBILITY_SERVICE) as AccessibilityManager
        isExploreByTouchEnabled = am.isTouchExplorationEnabled

        if (isExploreByTouchEnabled) {
            // Preset & preferences adapted
            publication.userSettingsUIPreset[ReadiumCSSName.ref(SCROLL_REF)] = true
            navigatorFragment.preferences.edit().putBoolean(SCROLL_REF, true).apply() //overriding user preferences
            userSettings.saveChanges()

            lifecycleScope.launchWhenResumed {
                delay(500)
                userSettings.updateViewCSS(SCROLL_REF)
            }
        } else {
            if (publication.cssStyle != "cjk-vertical") {
                publication.userSettingsUIPreset.remove(ReadiumCSSName.ref(SCROLL_REF))
            }
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

            (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.toggleSoftInput(
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
               userSettings.userSettingsPopUp().showAsDropDown(requireActivity().findViewById(R.id.settings), 0, 0, Gravity.END)
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

/**
 * Example of an HTML template for a custom Decoration Style.
 *
 * This one will display a tinted "pen" icon in the page margin to show that a highlight has an
 * associated note.
 */
@OptIn(ExperimentalDecorator::class)
private fun annotationMarkTemplate(context: Context, @ColorInt defaultTint: Int = Color.YELLOW): HtmlDecorationTemplate {
    // Converts the pen icon to a base 64 data URL, to be embedded in the decoration stylesheet.
    // Alternatively, serve the image with the local HTTP server and use its URL.
    val imageUrl = ContextCompat.getDrawable(context, R.drawable.ic_pen)
        ?.toBitmap()?.toDataUrl()
    requireNotNull(imageUrl)

    val className = "testapp-annotation-mark"
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
                background: url('$imageUrl') no-repeat center;
                background-size: auto 50%;
                opacity: 0.8;
            }
            """
    )
}
