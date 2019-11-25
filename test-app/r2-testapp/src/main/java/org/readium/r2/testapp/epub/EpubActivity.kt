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
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.NavigatorDelegate
import org.readium.r2.navigator.epub.R2EpubActivity
import org.readium.r2.navigator.pager.R2EpubPageFragment
import org.readium.r2.navigator.pager.R2PagerAdapter
import org.readium.r2.shared.APPEARANCE_REF
import org.readium.r2.shared.ContentLayoutStyle
import org.readium.r2.shared.Locations
import org.readium.r2.shared.Locator
import org.readium.r2.shared.LocatorText
import org.readium.r2.shared.ReadiumCSSName
import org.readium.r2.shared.RenditionLayout
import org.readium.r2.shared.SCROLL_REF
import org.readium.r2.shared.drm.DRM
import org.readium.r2.testapp.DRMManagementActivity
import org.readium.r2.testapp.R
import org.readium.r2.testapp.db.Bookmark
import org.readium.r2.testapp.db.BookmarksDatabase
import org.readium.r2.testapp.db.BooksDatabase
import org.readium.r2.testapp.db.PositionsDatabase
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

    override fun navigator(navigator: Navigator?, locator: Locator) {
        booksDB.books.saveProgression(locator, bookId)

        if (locator.locations?.progression == 0.toDouble()) {
            booksDB.books.saveCurrentUtterance(bookId, 0)
            screenReader.currentUtterance = 0
        }
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

    /**
     * Manage activity creation.
     *   - Load data from the database
     *   - Set background and text colors
     *   - Set onClickListener callbacks for the [screenReader] buttons
     *   - Initialize search.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bookmarksDB = BookmarksDatabase(this)
        booksDB = BooksDatabase(this)
        positionsDB = PositionsDatabase(this)

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
                screenReader.resumeReading()
                play_pause.setImageResource(android.R.drawable.ic_media_pause)
            } else {
                screenReader.pauseReading()
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
            if (goForward(false, completion = {})) {
                screenReader.nextResource()
                play_pause.setImageResource(android.R.drawable.ic_media_pause)
            }
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

    /**
     * @param currentUtterance: Long - The utterance index inside the current resource to save inside the database for
     *   [bookId].
     */
    fun saveCurrentUtterance(currentUtterance: Long) {
        booksDB.books.saveCurrentUtterance(bookId, currentUtterance)
    }

    /**
     * @return: Int? - Returns the first value from the column utterances for which the line's bookId matches [bookId],
     *   or null if not found.
     */
    fun getCurrentUtterance(): Int? {
        return booksDB.books.getSavedUtterance(bookId)?.toInt()
    }

    /**
     * Pause the screenReader if view is paused.
     */
    override fun onPause() {
        super.onPause()
        screenReader.pauseReading()
    }

    /**
     * Stop the screenReader if app is view is stopped.
     */
    override fun onStop() {
        super.onStop()
        screenReader.stopReading()
    }

    /**
     * The function allows to access the [R2ScreenReader] instance and set the [TextToSpeech] speech speed.
     * Values are limited between 0.25 and 3.0 included.
     *
     * @param speed: Float - The speech speed we wish to use with Android's [TextToSpeech].
     */
    fun updateScreenReaderSpeed(speed: Float) {
        var rSpeed = speed

        if (speed < 0.25) {
            rSpeed = 0.25.toFloat()
        } else if (speed > 3.0) {
            rSpeed = 3.0.toFloat()
        }
        screenReader.setSpeechSpeed(rSpeed)
    }

    /**
     * Override Android's option menu by inflating a custom view instead.
     *   - Initialize the search component.
     *
     * @param menu: Menu? - The menu view.
     * @return Boolean - return true.
     */
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

    /**
     * Management of the menu bar.
     *
     * When (TOC):
     *   - Open TOC activity for current publication.
     *
     * When (Settings):
     *   - Show settings view as a dropdown menu starting from the clicked button
     *
     * When (Screen Reader):
     *   - Switch screen reader on or off.
     *   - If screen reader was off, get reading speed from preferences, update reading speed and sync it with the
     *       active section in the webView.
     *   - If screen reader was on, dismiss it.
     *
     * When (DRM):
     *   - Dismiss screen reader if it was on
     *   - Start the DRM management activity.
     *
     * When (Bookmark):
     *   - Create a bookmark marking the current page and insert it inside the database.
     *
     * When (Search):
     *   - Make the search overlay visible.
     *
     * When (Home):
     *   - Make the search view invisible.
     *
     * @param item: MenuItem - The button that was pressed.
     * @return Boolean - Return true if the button has a switch case. Return false otherwise.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.toc -> {
                val intent = Intent(this, R2OutlineActivity::class.java)
                intent.putExtra("publication", publication)
                intent.putExtra("bookId", bookId)
                startActivityForResult(intent, 2)
                return true
            }
            R.id.settings -> {
                userSettings.userSettingsPopUp().showAsDropDown(this.findViewById(R.id.settings), 0, 0, Gravity.END)
                return true
            }
            R.id.screen_reader -> {
                if (!screenReader.isSpeaking && !screenReader.isPaused && item.title == resources.getString(R.string.epubactivity_read_aloud_start)) {

                    //Get user settings speed when opening the screen reader. Get a neutral percentage (corresponding to
                    //the normal speech speed) if no user settings exist.
                    val speed = preferences.getInt("reader_TTS_speed",
                        (2.75 * 3.toDouble() / 11.toDouble() * 100).toInt())
                    //Convert percentage to a float value between 0.25 and 3.0
                    val ttsSpeed = 0.25.toFloat() + (speed.toFloat() / 100.toFloat()) * 2.75.toFloat()

                    updateScreenReaderSpeed(ttsSpeed)

                    if (screenReader.goTo(resourcePager.currentItem)) {
                        item.title = resources.getString(R.string.epubactivity_read_aloud_stop)
                        tts_overlay.visibility = View.VISIBLE
                        play_pause.setImageResource(android.R.drawable.ic_media_pause)
                        allowToggleActionBar = false
                    }
                    else {
                        Toast.makeText(applicationContext, "No further chapter contains text to read", Toast.LENGTH_LONG).show()
                    }

                } else {
                    dismissScreenReader()
                }

                return true
            }
            R.id.drm -> {
                if (screenReader.isSpeaking) {
                    dismissScreenReader()
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

    /**
     * - Stop screenReader's reading.
     * - Set the title of the menu's button for launching TTS to a value that indicates it was closed.
     * - Make the TTS view invisible.
     * - Update the TTS play/pause button to show the good resource picture.
     * - Enable toggling the scrollbar which was previously disable for TTS.
     *
     * @param item: MenuItem - The Screen Reader menu button.
     */
    fun dismissScreenReader() {
        screenReader.stopReading()
        menuScreenReader?.title = resources.getString(R.string.epubactivity_read_aloud_start)
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

    /**
     * Manage what happens when the focus is put back on the EpubActivity.
     *  - Synchronize the [R2ScreenReader] with the webView if the [R2ScreenReader] exists.
     *  - Create a [R2ScreenReader] instance if it was uninitialized.
     */
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

        if (this::screenReader.isInitialized) {
            if (tts_overlay.visibility == View.VISIBLE) {
                if (screenReader.currentResource != resourcePager.currentItem) {
                    screenReader.goTo(resourcePager.currentItem)
                }

                if (screenReader.isPaused) {
                    screenReader.resumeReading()
                    play_pause.setImageResource(android.R.drawable.ic_media_pause)
                } else {
                    screenReader.pauseReading()
                    play_pause.setImageResource(android.R.drawable.ic_media_play)
                }
                screenReader.onResume()
            }
        } else {
            Handler().postDelayed({
                val port = preferences.getString("$publicationIdentifier-publicationPort", 0.toString())?.toInt()
                port?.let {
                    screenReader = R2ScreenReader(this, publication, port, publicationFileName, resourcePager.currentItem)
                }
            }, 500)
        }
    }

    /**
     * Determine whether the touch exploration is enabled (i.e. that description of touched elements is orally
     * fed back to the user) and toggle the ActionBar if it is disabled and if the text to speech is invisible.
     */
    override fun toggleActionBar() {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        isExploreByTouchEnabled = am.isTouchExplorationEnabled

        if (!isExploreByTouchEnabled && tts_overlay.visibility == View.INVISIBLE) {
            super.toggleActionBar()
        }

    }

    /**
     * Manage activity destruction.
     */
    override fun onDestroy() {
        super.onDestroy()
        screenReader.shutdown()
    }

    /**
     * Communicate with the user using a toast if touch exploration is enabled, to indicate the end of a chapter.
     */
    override fun onPageEnded(end: Boolean) {
        if (isExploreByTouchEnabled) {
            if (!pageEnded == end && end) {
                toast("End of chapter")
            }
            pageEnded = end
        }
    }

}