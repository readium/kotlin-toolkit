/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Mostapha Idoubihi, Paul Stoica
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_r2_epub.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.toast
import org.json.JSONObject
import org.readium.r2.navigator.R2EpubActivity
import org.readium.r2.shared.*
import org.readium.r2.shared.drm.DRM
import org.readium.r2.testapp.search.R2SearchActivity
import kotlin.coroutines.CoroutineContext


/**
 * R2EpubActivity : Extension of the R2EpubActivity() from navigator
 *
 * That Activity manage everything related to the menu
 *      ( Table of content, User Settings, DRM, Bookmarks )
 *
 */
class R2EpubActivity : R2EpubActivity(), CoroutineScope {

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
    private lateinit var positionsDB: PositionsDatabase

    private lateinit var screenReader: R2ScreenReader

    protected var drm: DRM? = null
    protected var menuDrm: MenuItem? = null
    protected var menuToc: MenuItem? = null
    protected var menuBmk: MenuItem? = null
    protected var menuSearch: MenuItem? = null

    protected var menuScreenReader: MenuItem? = null

    private var bookId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bookmarksDB = BookmarksDatabase(this)
        positionsDB = PositionsDatabase(this)

        Handler().postDelayed({
            bookId = intent.getLongExtra("bookId", -1)
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
            nextResource(false)
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
            previousResource(false)
            screenReader.previousResource()
            screenReader.start()
            play_pause.setImageResource(android.R.drawable.ic_media_pause)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_epub, menu)
        menuDrm = menu?.findItem(R.id.drm)
        menuToc = menu?.findItem(R.id.toc)
        menuBmk = menu?.findItem(R.id.bookmark)
        menuSearch = menu?.findItem(R.id.searchBtn)

        menuScreenReader = menu?.findItem(R.id.screen_reader)

        menuScreenReader?.isVisible = !isExploreByTouchEnabled

        menuDrm?.isVisible = false
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
                val resourceHref = resource.href?: ""
                val resourceType = resource.typeLink?: ""
                val resourceTitle = resource.title?: ""
                val locations = Locations.fromJSON(JSONObject(preferences.getString("$publicationIdentifier-documentLocations", "{}")))
                val currentPage = positionsDB.positions.getCurrentPage(bookId, resourceHref, locations.progression!!)?.let {
                    it
                }

                val bookmark = Bookmark(
                        bookId,
                        publicationIdentifier,
                        resourceIndex,
                        resourceHref,
                        resourceType,
                        resourceTitle,
                        Locations(progression = locations.progression, position = currentPage),
                        LocatorText()
                )
                
                bookmarksDB.bookmarks.insert(bookmark)?.let {
                    launch {
                        currentPage?.let {
                            toast("Bookmark added at page $currentPage")
                        } ?:run {
                            toast("Bookmark added")
                        }
                    }
                } ?:run {
                    launch {
                        toast("Bookmark already exists")
                    }
                }

                return true
            }

            R.id.searchBtn -> {

                val intent = Intent(this, R2SearchActivity::class.java)
                startActivityForResult(intent, 2)
                return true
            }

            else -> return false
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
        }
    }

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
                screenReader = R2ScreenReader(this, publication, port, epubName)
            }
        }, 500)

    }

    override fun toggleActionBar() {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        isExploreByTouchEnabled = am.isTouchExplorationEnabled

        if (!isExploreByTouchEnabled && tts_overlay.visibility == View.INVISIBLE) {
            super.toggleActionBar()
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