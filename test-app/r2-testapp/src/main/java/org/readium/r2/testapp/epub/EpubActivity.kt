/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Mostapha Idoubihi, Paul Stoica
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp.epub

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.*
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_epub.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.anko.appcompat.v7.coroutines.onClose
import org.jetbrains.anko.indeterminateProgressDialog
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.toast
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.NavigatorDelegate
import org.readium.r2.navigator.epub.R2EpubActivity
import org.readium.r2.navigator.epub.Style
import org.readium.r2.navigator.pager.R2EpubPageFragment
import org.readium.r2.navigator.pager.R2PagerAdapter
import org.readium.r2.shared.*
import org.readium.r2.shared.drm.DRM
import org.readium.r2.testapp.DRMManagementActivity
import org.readium.r2.testapp.R
import org.readium.r2.testapp.db.*
import org.readium.r2.testapp.outline.R2OutlineActivity
import org.readium.r2.testapp.search.MarkJSSearchEngine
import org.readium.r2.testapp.search.SearchLocator
import org.readium.r2.testapp.search.SearchLocatorAdapter
import timber.log.Timber
import kotlin.coroutines.CoroutineContext


/**
 * EpubActivity : Extension of the EpubActivity() from navigator
 *
 * That Activity manage everything related to the menu
 *      ( Table of content, User Settings, DRM, Bookmarks )
 *
 */
class EpubActivity : R2EpubActivity(), CoroutineScope, NavigatorDelegate/*, VisualNavigatorDelegate, OutlineTableViewControllerDelegate*/ {

    override val currentLocation: Locator?
        get() {
            val resourceIndex = resourcePager.currentItem.toLong()
            val resource = publication.readingOrder[resourcePager.currentItem]
            val resourceHref = resource.href ?: ""
            val resourceType = resource.typeLink ?: ""
            val resourceTitle = resource.title ?: ""

            return booksDB.books.currentLocator(bookId)?.let {
                it
            } ?: run {
                Locator(resourceHref, resourceType, publication.metadata.title, Locations(progression = 0.0))
            }
        }

    override fun locationDidChange(navigator: Navigator?, locator: Locator) {
        booksDB.books.saveProgression(locator, bookId)
    }


    /**
     * Context of this scope.
     */
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    //UserSettings
    lateinit var userSettings: UserSettings

    //Accessibility
    private var isExploreByTouchEnabled = false
    private var pageEnded = false

    // Provide access to the Bookmarks & Positions Databases
    private lateinit var bookmarksDB: BookmarksDatabase
    private lateinit var booksDB: BooksDatabase
    private lateinit var positionsDB: PositionsDatabase
    private lateinit var highlightDB: HighligtsDatabase

    private lateinit var screenReader: R2ScreenReader

    protected var drm: DRM? = null
    protected var menuDrm: MenuItem? = null
    protected var menuToc: MenuItem? = null
    protected var menuBmk: MenuItem? = null
    protected var menuSearch: MenuItem? = null

    protected var menuScreenReader: MenuItem? = null

    private var bookId: Long = -1

    private var searchTerm = ""
    private lateinit var searchStorage: SharedPreferences
    private lateinit var searchResultAdapter: SearchLocatorAdapter
    private lateinit var searchResult: MutableList<SearchLocator>

    private var mode: ActionMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bookmarksDB = BookmarksDatabase(this)
        booksDB = BooksDatabase(this)
        positionsDB = PositionsDatabase(this)
        highlightDB = HighligtsDatabase(this)

        navigatorDelegate = this
        bookId = intent.getLongExtra("bookId", -1)

        Handler().postDelayed({
            launch {
                menuDrm?.isVisible = intent.getBooleanExtra("drm", false)
            }
        }, 100)

        val appearancePref = preferences.getInt(APPEARANCE_REF, 0)
        val backgroundsColors = mutableListOf("#ffffff", "#faf4e8", "#000000")
        val textColors = mutableListOf("#000000", "#000000", "#ffffff")
        resourcePager.setBackgroundColor(Color.parseColor(backgroundsColors[appearancePref]))
        (resourcePager.focusedChild?.findViewById(org.readium.r2.navigator.R.id.book_title) as? TextView)?.setTextColor(Color.parseColor(textColors[appearancePref]))
        toggleActionBar()

        resourcePager.offscreenPageLimit = 1

        currentPagerPosition = publication.readingOrder.indexOfFirst { it.href == currentLocation?.href }
        resourcePager.currentItem = currentPagerPosition

        titleView.text = publication.metadata.title

        play_pause.setOnClickListener {
            if (screenReader.isPaused) {
                screenReader.resume()
                play_pause.setImageResource(android.R.drawable.ic_media_pause)
            } else {
                screenReader.pause()
                play_pause.setImageResource(android.R.drawable.ic_media_play)
            }
        }
        fast_forward.setOnClickListener {
            if (screenReader.nextSentence()) {
                play_pause.setImageResource(android.R.drawable.ic_media_pause)
            } else {
                next_chapter.callOnClick()
            }
        }
        next_chapter.setOnClickListener {
            goForward(false, completion = {})
            screenReader.nextResource()
            screenReader.start()
            play_pause.setImageResource(android.R.drawable.ic_media_pause)
        }
        fast_back.setOnClickListener {
            if (screenReader.previousSentence()) {
                play_pause.setImageResource(android.R.drawable.ic_media_pause)
            } else {
                prev_chapter.callOnClick()
            }
        }
        prev_chapter.setOnClickListener {
            goBackward(false, completion = {})
            screenReader.previousResource()
            screenReader.start()
            play_pause.setImageResource(android.R.drawable.ic_media_pause)
        }


        // SEARCH
        searchStorage = getSharedPreferences("org.readium.r2.search", Context.MODE_PRIVATE)
        searchResult = mutableListOf()
        searchResultAdapter = SearchLocatorAdapter(this, searchResult, object : SearchLocatorAdapter.RecyclerViewClickListener {
            override fun recyclerViewListClicked(v: View, position: Int) {

                search_overlay.visibility = View.INVISIBLE
                val searchView = menuSearch?.getActionView() as SearchView

                searchView.clearFocus()
                if (searchView.isShown) {
                    menuSearch?.collapseActionView();
                    resourcePager.offscreenPageLimit = 1
                }

                val locator = searchResult[position]
                val intent = Intent()
                intent.putExtra("publicationPath", publicationPath)
                intent.putExtra("epubName", publicationFileName)
                intent.putExtra("publication", publication)
                intent.putExtra("bookId", bookId)
                intent.putExtra("locator", locator)
                onActivityResult(2, Activity.RESULT_OK, intent)
            }

        })
        search_listView.adapter = searchResultAdapter
        search_listView.layoutManager = LinearLayoutManager(this)

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_epub, menu)
        menuDrm = menu?.findItem(R.id.drm)
        menuToc = menu?.findItem(R.id.toc)
        menuBmk = menu?.findItem(R.id.bookmark)
        menuSearch = menu?.findItem(R.id.search)

        menuScreenReader = menu?.findItem(R.id.screen_reader)

        menuScreenReader?.isVisible = !isExploreByTouchEnabled

        menuDrm?.isVisible = false

        val searchView = menuSearch?.getActionView() as SearchView

        searchView.isFocusable = false
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {

                searchResult.clear()
                searchResultAdapter.notifyDataSetChanged()

                query?.let {
                    search_overlay.visibility = View.VISIBLE
                    resourcePager.offscreenPageLimit = publication.readingOrder.size

                    //Saving searched term
                    searchTerm = query
                    //Initializing our custom search interfaces
                    val progress = indeterminateProgressDialog(getString(R.string.progress_wait_while_searching_book))
                    progress.show()

                    val markJSSearchInteface = MarkJSSearchEngine(this@EpubActivity)
                    Handler().postDelayed({
                        markJSSearchInteface.search(query) { (last, result) ->
                            searchResult.clear()
                            searchResult.addAll(result)
                            searchResultAdapter.notifyDataSetChanged()

                            //Saving results + keyword only when JS is fully executed on all resources
                            val editor = searchStorage.edit()
                            val stringResults = Gson().toJson(result)
                            editor.putString("result", stringResults)
                            editor.putString("term", searchTerm)
                            editor.putLong("book", bookId)
                            editor.apply()

                            if (last) {
                                progress.dismiss()
                            }
                        }
                    }, 500)


                }
                return false
            }

            override fun onQueryTextChange(s: String): Boolean {
                return false
            }
        })
        searchView.setOnQueryTextFocusChangeListener { view, b ->
            if (!b) {
                search_overlay.visibility = View.INVISIBLE
            } else {
                search_overlay.visibility = View.VISIBLE
                resourcePager.offscreenPageLimit = publication.readingOrder.size
            }
        }
        searchView.onClose {
            search_overlay.visibility = View.INVISIBLE

        }
        searchView.setOnCloseListener {
            if (searchView.isShown) {
                menuSearch?.collapseActionView();
            }
            search_overlay.visibility = View.INVISIBLE

            true
        }
        searchView.setOnSearchClickListener {
            val previouslySearchBook = searchStorage.getLong("book", -1)
            if (previouslySearchBook == bookId) {
                //Loading previous results + keyword
                val tmp = searchStorage.getString("result", null)
                if (tmp != null) {
                    val gson = Gson()
                    searchResult.clear()
                    searchResult.addAll(gson.fromJson(tmp, Array<SearchLocator>::class.java).asList().toMutableList())
                    searchResultAdapter.notifyDataSetChanged()

                    val keyword = searchStorage.getString("term", null)
                    searchView.setQuery(keyword, false)
                    searchView.clearFocus()
                }
                searchView.setQuery(searchStorage.getString("term", null), false);
            }

            search_overlay.visibility = View.VISIBLE
            resourcePager.offscreenPageLimit = publication.readingOrder.size
        }

        menuSearch?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                search_overlay.visibility = View.VISIBLE
                resourcePager.offscreenPageLimit = publication.readingOrder.size
                return true;
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                search_overlay.visibility = View.INVISIBLE
                return true;
            }
        })

        val closeButton = searchView.findViewById(R.id.search_close_btn) as ImageView
        closeButton.setOnClickListener {
            searchResult.clear()
            searchResultAdapter.notifyDataSetChanged()

            searchView.setQuery("", false);

            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

            val editor = searchStorage.edit()
            editor.remove("result")
            editor.remove("term")
            editor.apply()
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.toc -> {
                if (screenReader.isSpeaking) {
                    dismissScreenReader(menuScreenReader!!)
                }
                val intent = Intent(this, R2OutlineActivity::class.java)
                intent.putExtra("publication", publication)
                intent.putExtra("bookId", bookId)
                startActivityForResult(intent, 2)
                return true
            }
            R.id.settings -> {
                if (screenReader.isSpeaking) {
                    dismissScreenReader(menuScreenReader!!)
                }
                userSettings.userSettingsPopUp().showAsDropDown(this.findViewById(R.id.settings), 0, 0, Gravity.END)
                return true
            }
            R.id.screen_reader -> {
                if (!screenReader.isSpeaking && !screenReader.isPaused && item.title == resources.getString(R.string.epubactivity_read_aloud_start)) {

                    screenReader.goTo(resourcePager.currentItem)
                    screenReader.start()

                    item.title = resources.getString(R.string.epubactivity_read_aloud_stop)

                    tts_overlay.visibility = View.VISIBLE
                    play_pause.setImageResource(android.R.drawable.ic_media_pause)
                    allowToggleActionBar = false

                } else {

                    dismissScreenReader(item)

                }

                return true
            }
            R.id.drm -> {
                if (screenReader.isSpeaking) {
                    dismissScreenReader(menuScreenReader!!)
                }
                startActivityForResult(intentFor<DRMManagementActivity>("publication" to publicationPath), 1)
                return true
            }
            R.id.bookmark -> {
                val resourceIndex = resourcePager.currentItem.toLong()
                val resource = publication.readingOrder[resourcePager.currentItem]
                val resourceHref = resource.href ?: ""
                val resourceType = resource.typeLink ?: ""
                val resourceTitle = resource.title ?: ""
                val currentPage = positionsDB.positions.getCurrentPage(bookId, resourceHref, currentLocation?.locations?.progression!!)?.let {
                    it
                }

                val bookmark = Bookmark(
                        bookId,
                        publicationIdentifier,
                        resourceIndex,
                        resourceHref,
                        resourceType,
                        resourceTitle,
                        Locations(progression = currentLocation?.locations?.progression, position = currentPage),
                        LocatorText()
                )

                bookmarksDB.bookmarks.insert(bookmark)?.let {
                    launch {
                        currentPage?.let {
                            toast("Bookmark added at page $currentPage")
                        } ?: run {
                            toast("Bookmark added")
                        }
                    }
                } ?: run {
                    launch {
                        toast("Bookmark already exists")
                    }
                }

                return true
            }
            R.id.search -> {
                search_overlay.visibility = View.VISIBLE
                resourcePager.offscreenPageLimit = publication.readingOrder.size

                val searchView = menuSearch?.getActionView() as SearchView

                searchView.clearFocus()

                return super.onOptionsItemSelected(item)
            }

            android.R.id.home -> {
                search_overlay.visibility = View.INVISIBLE
                resourcePager.offscreenPageLimit = 1
                val searchView = menuSearch?.getActionView() as SearchView
                searchView.clearFocus()
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }

    }

    fun dismissScreenReader(item: MenuItem) {
        screenReader.stop()
        item.title = resources.getString(R.string.epubactivity_read_aloud_start)
        tts_overlay.visibility = View.INVISIBLE
        play_pause.setImageResource(android.R.drawable.ic_media_play)
        allowToggleActionBar = true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getBooleanExtra("returned", false)) {
                finish()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == 2 && resultCode == Activity.RESULT_OK && data != null) {
                val locator = data.getSerializableExtra("locator") as Locator
                locator.locations?.fragment?.let { fragment ->

                    // TODO handle fragment anchors (id=) instead of catching the json exception
                    try {
                        val fragments = JSONArray(fragment).getString(0).split(",").associate {
                            val (left, right) = it.split("=")
                            left to right.toInt()
                        }

                        val index = fragments.getValue("i").toInt()
                        val searchStorage = getSharedPreferences("org.readium.r2.search", Context.MODE_PRIVATE)
                        Handler().postDelayed({
                            if (publication.metadata.rendition.layout == RenditionLayout.Reflowable) {
                                val currentFragent = (resourcePager.adapter as R2PagerAdapter).getCurrentFragment() as R2EpubPageFragment
                                val resource = publication.readingOrder[resourcePager.currentItem]
                                val resourceHref = resource.href ?: ""
                                val resourceType = resource.typeLink ?: ""
                                val resourceTitle = resource.title ?: ""

                                currentFragent.webView.runJavaScript("markSearch('${searchStorage.getString("term", null)}', null, '$resourceHref', '$resourceType', '$resourceTitle', '$index')") { result ->

                                    Timber.d("###### $result")

                                }
                            }
                        }, 1200)
                    } catch (e: Exception) {
                    }
                }
            }
        }
    }

    override fun onActionModeStarted(mode: ActionMode?) {

        mode?.menu?.run {
            menuInflater.inflate(R.menu.menu_action_mode, this)
            findItem(R.id.highlight).setOnMenuItemClickListener {
                val currentFragment = ((resourcePager.adapter as R2PagerAdapter).mFragments.get((resourcePager.adapter as R2PagerAdapter).getItemId(resourcePager.currentItem))) as? R2EpubPageFragment

                currentFragment?.webView?.getCurrentSelectionRect {
                    val rect = JSONObject(it).run {
                        try {
                            val display = windowManager.defaultDisplay
                            val metrics = DisplayMetrics()
                            display.getMetrics(metrics)
                            val left = getDouble("left")
                            val width = getDouble("width")
                            val top = getDouble("top") * metrics.density
                            val height = getDouble("height") * metrics.density
                            Rect(left.toInt(), top.toInt(), width.toInt() + left.toInt(), top.toInt() + height.toInt())
                        } catch (e: JSONException) {
                            null
                        }
                    }
                    showHighlightPopup(size = rect) {
                        mode.finish()
                    }
                }
                true
            }
            findItem(R.id.memo).setOnMenuItemClickListener {
                showAnnotationPopup()
                true
            }
        }
        this.mode = mode
        super.onActionModeStarted(mode)
    }

    private fun showHighlightPopup(highlightID: String? = null, size: Rect?, dismissCallback: () -> Unit) {
        var highlight: org.readium.r2.navigator.epub.Highlight? = null

        highlightID?.let { id ->
            highlightDB.highlights.list(id).forEach {
                highlight = convertHighlight2NavigationHighlight(it)
            }
        }
        val currentFragment = ((resourcePager.adapter as R2PagerAdapter).mFragments.get((resourcePager.adapter as R2PagerAdapter).getItemId(resourcePager.currentItem))) as? R2EpubPageFragment

        val display = windowManager.defaultDisplay
        val rect = size ?: Rect()

        val mDisplaySize = Point()
        display.getSize(mDisplaySize)

        val popupView = layoutInflater.inflate(
                if (rect.top > rect.height()) R.layout.view_action_mode_reverse else R.layout.view_action_mode,
                null,
                false
        )
        popupView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))

        val popupWindow = PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        popupWindow.isFocusable = true

        val x = rect.left
        val y = if (rect.top > rect.height()) rect.top - rect.height() - 80 else rect.bottom

        popupWindow.showAtLocation(popupView, Gravity.NO_GRAVITY, x  , y)

        popupView.run {
            findViewById<View>(R.id.notch).run {
                setX((rect.left*2).toFloat())
            }
            findViewById<View>(R.id.red).setOnClickListener {
                changeHighlightColor(highlight, Color.rgb(247, 124,124))
            }
            findViewById<View>(R.id.green).setOnClickListener {
                changeHighlightColor(highlight, Color.rgb(173, 247,123))
            }
            findViewById<View>(R.id.blue).setOnClickListener {
                changeHighlightColor(highlight, Color.rgb(124,198,247))
            }
            findViewById<View>(R.id.yellow).setOnClickListener {
                changeHighlightColor(highlight, Color.rgb(249, 239, 125))
            }
            findViewById<View>(R.id.purple).setOnClickListener {
                changeHighlightColor(highlight, Color.rgb(182, 153, 255))
            }
            findViewById<View>(R.id.annotation).setOnClickListener {
                showAnnotationPopup(highlight)
            }
            findViewById<View>(R.id.del).run {
                visibility = if (highlight != null) View.VISIBLE else View.GONE
                setOnClickListener {
                    deleteHighlight(highlight)
                    popupWindow.dismiss()
                }
            }
        }

    }

    private fun changeHighlightColor(highlight: org.readium.r2.navigator.epub.Highlight? = null, color: Int) {
        if (highlight != null) {
            val navigatorHighlight = org.readium.r2.navigator.epub.Highlight(
                    highlight.id,
                    highlight.locator,
                    color,
                    highlight.style,
                    highlight.annotationMarkStyle
            )
            showHighlight(navigatorHighlight)
            addHighlight(navigatorHighlight)
        }
        else {
            createHighlight(color) {
                addHighlight(it)
            }
        }
    }

    private fun addHighlight(highlight: org.readium.r2.navigator.epub.Highlight) {
        val annotation = highlightDB.highlights.list(highlight.id).run {
            if (isNotEmpty()) first().annotation
            else ""
        }

        highlightDB.highlights.insert(
                convertNavigationHighlight2Highlight(
                        highlight,
                        annotation,
                        highlight.annotationMarkStyle
                )
        )
    }

    private fun deleteHighlight(highlight: org.readium.r2.navigator.epub.Highlight?) {
        highlight?.let {
            highlightDB.highlights.delete(it.id)
            hideHighlightWithID(it.id)
        }
    }

    private fun addAnnotation(highlight: org.readium.r2.navigator.epub.Highlight, annotation: String) {
        highlightDB.highlights.insert(
                convertNavigationHighlight2Highlight(highlight, annotation, "annotation")
        )
    }

    private fun drawHighlight() {
        highlightDB.highlights.listAll().forEach {
            val resource = publication.readingOrder[resourcePager.currentItem]

            if (it.resourceHref == resource.href) {
                val highlight = convertHighlight2NavigationHighlight(it)
                showHighlight(highlight)
            }
        }
    }

    private fun showAnnotationPopup(highlight: org.readium.r2.navigator.epub.Highlight? = null) {
        val view = layoutInflater.inflate(R.layout.popup_memo, null, false)
        val alert = AlertDialog.Builder(this)
                .setView(view)
                .create()

        val annotation = highlight?.run {
            highlightDB.highlights.list(id).first().run {
                if (annotation.isEmpty() and annotationMarkStyle.isEmpty()) ""
                else annotation
            }
        }

        with(view) {
            val memo = findViewById<EditText>(R.id.memo)
            findViewById<TextView>(R.id.positive).setOnClickListener {
                if (memo.text.isEmpty().not()) {
                    createAnnotation(highlight) {
                        addAnnotation(it, memo.text.toString())
                    }
                }
                alert.dismiss()
            }
            findViewById<TextView>(R.id.negative).setOnClickListener {
                alert.dismiss()
            }
            if (highlight != null) {
                findViewById<TextView>(R.id.select_text).text = highlight.locator.text?.highlight
                memo.setText(annotation)
            }
            else {
                currentSelection {
                    findViewById<TextView>(R.id.select_text).text = it?.text?.highlight
                }
            }
        }
        alert.show()
    }

    override fun onPageLoaded() {
        super.onPageLoaded()
        drawHighlight()
    }

    override fun highlightActivated(id: String) {
        rectangleForHighlightWithID(id) {
            showHighlightPopup(id, it) {
            }
        }
    }

    override fun highlightAnnotationMarkActivated(id: String) {
        val highlight = highlightDB.highlights.list(id.replace("ANNOTATION", "HIGHLIGHT")).first()
        showAnnotationPopup(convertHighlight2NavigationHighlight(highlight))
    }

    private fun convertNavigationHighlight2Highlight(highlight: org.readium.r2.navigator.epub.Highlight, annotation: String? = null, annotationMarkStyle: String? = null): Highlight {
        val resourceIndex = resourcePager.currentItem.toLong()
        val resource = publication.readingOrder[resourcePager.currentItem]
        val resourceHref = resource.href?: ""
        val resourceType = resource.typeLink?: ""
        val resourceTitle = resource.title?: ""
        val currentPage = positionsDB.positions.getCurrentPage(bookId, resourceHref, currentLocation?.locations?.progression!!)?.let {
            it
        }

        val highlightLocations = highlight.locator.locations?.apply {
            progression = currentLocation?.locations?.progression
            position = currentPage
        } ?: Locations()
        val locationText = highlight.locator.text ?: LocatorText()

        return Highlight(
                highlight.id,
                publicationIdentifier,
                "style",
                highlight.color,
                annotation ?: "",
                annotationMarkStyle ?: "",
                resourceIndex,
                resourceHref,
                resourceType,
                resourceTitle,
                highlightLocations,
                locationText
        )
    }

    private fun convertHighlight2NavigationHighlight(highlight: Highlight) = org.readium.r2.navigator.epub.Highlight(
            highlight.highlightID,
            Locator(
                    highlight.resourceHref,
                    highlight.resourceType,
                    locations = highlight.locations,
                    text = highlight.locatorText
            ),
            highlight.color,
            Style.highlight,
            highlight.annotationMarkStyle
    )


    override fun onResume() {
        super.onResume()

        /*
         * If TalkBack or any touch exploration service is activated
         * we force scroll mode (and override user preferences)
         */
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        isExploreByTouchEnabled = am.isTouchExplorationEnabled

        if (isExploreByTouchEnabled) {

            //Preset & preferences adapted
            publication.userSettingsUIPreset[ReadiumCSSName.ref(SCROLL_REF)] = true
            preferences.edit().putBoolean(SCROLL_REF, true).apply() //overriding user preferences

            userSettings = UserSettings(preferences, this, publication.userSettingsUIPreset)
            userSettings.saveChanges()

            Handler().postDelayed({
                userSettings.resourcePager = resourcePager
                userSettings.updateViewCSS(SCROLL_REF)
            }, 500)
        } else {
            if (publication.cssStyle != ContentLayoutStyle.cjkv.name) {
                publication.userSettingsUIPreset.remove(ReadiumCSSName.ref(SCROLL_REF))
            }

            userSettings = UserSettings(preferences, this, publication.userSettingsUIPreset)
            userSettings.resourcePager = resourcePager
        }


        /*
         * Initialisation of the screen reader
         */
        Handler().postDelayed({
            val port = preferences.getString("$publicationIdentifier-publicationPort", 0.toString())?.toInt()
            port?.let {
                screenReader = R2ScreenReader(this, publication, port, publicationFileName)
            }
        }, 500)

    }

    override fun toggleActionBar() {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        isExploreByTouchEnabled = am.isTouchExplorationEnabled

        if (!isExploreByTouchEnabled && tts_overlay.visibility == View.INVISIBLE) {
            super.toggleActionBar()
        }
        launch(coroutineContext) {
            mode?.finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        screenReader.release()
    }


    override fun onPageEnded(end: Boolean) {
        if (isExploreByTouchEnabled) {
            if (!pageEnded == end && end) {
                toast("End of chapter")
            }

            pageEnded = end
        }
    }

}