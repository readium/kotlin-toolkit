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

import android.content.Context
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
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.R2EpubActivity
import org.readium.r2.navigator.pager.R2EpubPageFragment
import org.readium.r2.navigator.pager.R2PagerAdapter
import org.readium.r2.shared.ReadiumCSSName
import org.readium.r2.shared.SCROLL_REF
import org.readium.r2.shared.publication.Locator
import org.readium.r2.testapp.R
import org.readium.r2.testapp.R2App
import org.readium.r2.testapp.drm.DrmManagementContract
import org.readium.r2.testapp.drm.DrmManagementFragment
import org.readium.r2.testapp.outline.OutlineContract
import org.readium.r2.testapp.outline.OutlineFragment
import org.readium.r2.testapp.reader.EpubReaderFragment
import org.readium.r2.testapp.reader.ReaderContract
import org.readium.r2.testapp.reader.ReaderViewModel
import org.readium.r2.testapp.utils.clearPadding
import org.readium.r2.testapp.utils.padSystemUi
import org.readium.r2.testapp.utils.showSystemUi
import org.readium.r2.navigator.epub.Highlight as NavigatorHighlight

class EpubActivity : R2EpubActivity() {

    private lateinit var modelFactory: ReaderViewModel.Factory
    private lateinit var readerFragment: EpubReaderFragment
    private lateinit var viewModel: ReaderViewModel

    lateinit var userSettings: UserSettings

    //Accessibility
    private var isExploreByTouchEnabled = false
    private var pageEnded = false

    // Highlights
    private var mode: ActionMode? = null
    private var popupWindow: PopupWindow? = null

    override fun navigatorFragment(): EpubNavigatorFragment =
        readerFragment.childFragmentManager.findFragmentByTag(getString(R.string.epub_navigator_tag)) as EpubNavigatorFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        check(R2App.isServerStarted)

        val inputData = ReaderContract.parseIntent(this)
        modelFactory = ReaderViewModel.Factory(applicationContext, inputData)
        super.onCreate(savedInstanceState)

        ViewModelProvider(this).get(ReaderViewModel::class.java).let { model ->
            model.channel.receive(this) {handleReaderFragmentEvent(it) }
            viewModel = model
        }

        /* FIXME: When the OutlineFragment is left by pressing the back button,
        * the Webview is not updated, so removed highlights will still be visible.
        */
        supportFragmentManager.setFragmentResultListener(
            OutlineContract.REQUEST_KEY,
            this,
            FragmentResultListener { _, result ->
                val locator = OutlineContract.parseResult(result).destination
                closeOutlineFragment(locator)
            }
        )

        supportFragmentManager.setFragmentResultListener(
            DrmManagementContract.REQUEST_KEY,
            this,
            FragmentResultListener { _, result ->
                if (DrmManagementContract.parseResult(result).hasReturned)
                    finish()
            }
        )

        supportFragmentManager.addOnBackStackChangedListener {
             updateActivityTitle()
        }

        if (savedInstanceState == null) {
            val bookId = inputData.bookId
            val baseUrl = requireNotNull(inputData.baseUrl)
            readerFragment = EpubReaderFragment.newInstance(baseUrl, bookId)

            supportFragmentManager.commitNow {
                replace(R.id.activity_container, readerFragment, READER_FRAGMENT_TAG)
            }

        } else {
            readerFragment = supportFragmentManager.findFragmentByTag(READER_FRAGMENT_TAG) as EpubReaderFragment
        }

        // Without this, activity_reader_container receives the insets only once,
        // although we need a call every time the reader is hidden
        window.decorView.setOnApplyWindowInsetsListener { view, insets ->
            val newInsets = view.onApplyWindowInsets(insets)
            findViewById<FrameLayout>(R.id.activity_container).dispatchApplyWindowInsets(newInsets)
        }

        findViewById<FrameLayout>(R.id.activity_container).setOnApplyWindowInsetsListener { container, insets ->
            updateSystemUiPadding(container, insets)
            insets
        }

        supportFragmentManager.addOnBackStackChangedListener {
            updateSystemUiVisibility()
        }
    }

    override fun onStart() {
        super.onStart()
        updateSystemUiVisibility()
        updateActivityTitle()
    }

    private fun updateSystemUiVisibility() {
        if (readerFragment.isHidden)
            showSystemUi()
        else
            readerFragment.updateSystemUiVisibility()

        // Seems to be required to adjust padding when transitioning from the outlines to the screen reader
        findViewById<FrameLayout>(R.id.activity_container).requestApplyInsets()
    }

    private fun updateSystemUiPadding(container: View, insets: WindowInsets) {
        if (readerFragment.isHidden)
            container.padSystemUi(insets, this)
        else
            container.clearPadding()
    }

    private fun updateActivityTitle() {
        title = when (supportFragmentManager.fragments.last()) {
            is OutlineFragment -> publication.metadata.title
            is DrmManagementFragment -> getString(R.string.title_fragment_drm_management)
            else -> null
        }
    }

    override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
        return modelFactory
    }

    override fun onActionModeStarted(mode: ActionMode?) {
        super.onActionModeStarted(mode)
        mode?.menu?.run {
            menuInflater.inflate(R.menu.menu_action_mode, this)
            findItem(R.id.highlight).setOnMenuItemClickListener {
                val currentFragment = ((resourcePager.adapter as R2PagerAdapter).mFragments.get((resourcePager.adapter as R2PagerAdapter).getItemId(resourcePager.currentItem))) as? R2EpubPageFragment

                currentFragment?.webView?.getCurrentSelectionRect {
                    val rect =
                        try {
                            with(JSONObject(it)) {
                                val display = windowManager.defaultDisplay
                                val metrics = DisplayMetrics()
                                display.getMetrics(metrics)
                                val left = getDouble("left")
                                val width = getDouble("width")
                                val top = getDouble("top") * metrics.density
                                val height = getDouble("height") * metrics.density
                                Rect(
                                    left.toInt(),
                                    top.toInt(),
                                    width.toInt() + left.toInt(),
                                    top.toInt() + height.toInt()
                                )
                            }
                        } catch (e: JSONException) {
                            null
                        }
                    if (rect != null) {
                        showHighlightPopup(size = rect)
                    }
                }
                true
            }
            findItem(R.id.note).setOnMenuItemClickListener {
                showAnnotationPopup()
                true
            }
        }
        this.mode = mode
    }

    private fun showHighlightPopup(highlightID: String? = null, size: Rect) {
        popupWindow?.let {
            if (it.isShowing) {
                return
            }
        }

        launch {
            val highlight: NavigatorHighlight? = highlightID?.let { viewModel.getHighlightByHighlightId(it)?.toNavigatorHighlight() }
//        val highlight: org.readium.r2.navigator.epub.Highlight? = try {
//            highlightID
//                ?.let { persistence.getHighlight(it).toNavigatorHighlight() }
//        } catch (e: IllegalStateException) {
//            return // FIXME: This is required as long as the navigator highlight is not deleted
//            // when the corresponding highlight in DB is.
//        }

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

            popupWindow = PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            popupWindow?.isFocusable = true

            val x = rect.left
            val y = if (rect.top > rect.height()) rect.top - rect.height() - 80 else rect.bottom

            popupWindow?.showAtLocation(popupView, Gravity.NO_GRAVITY, x, y)

            popupView.run {
                findViewById<View>(R.id.notch).run {
                    setX((rect.left * 2).toFloat())
                }
                findViewById<View>(R.id.red).setOnClickListener {
                    changeHighlightColor(highlight, Color.rgb(247, 124, 124))
                }
                findViewById<View>(R.id.green).setOnClickListener {
                    changeHighlightColor(highlight, Color.rgb(173, 247, 123))
                }
                findViewById<View>(R.id.blue).setOnClickListener {
                    changeHighlightColor(highlight, Color.rgb(124, 198, 247))
                }
                findViewById<View>(R.id.yellow).setOnClickListener {
                    changeHighlightColor(highlight, Color.rgb(249, 239, 125))
                }
                findViewById<View>(R.id.purple).setOnClickListener {
                    changeHighlightColor(highlight, Color.rgb(182, 153, 255))
                }
                findViewById<View>(R.id.annotation).setOnClickListener {
                    popupWindow?.dismiss()
                    showAnnotationPopup(highlight)
                }
                findViewById<View>(R.id.del).run {
                    visibility = if (highlight != null) View.VISIBLE else View.GONE
                    setOnClickListener {
                        highlight?.let {
                            viewModel.deleteHighlightByHighlightId(highlight.id)
                            hideHighlightWithID(highlight.id)
                        }
                        popupWindow?.dismiss()
                        mode?.finish()
                    }
                }
            }
        }
    }

    private fun changeHighlightColor(highlight: org.readium.r2.navigator.epub.Highlight? = null, color: Int) {
        if (highlight != null) {
            showHighlight(highlight.copy(color = color))
            viewModel.updateHighlight(highlight.id, color = color)
        } else {
            createHighlight(color) {
                viewModel.insertHighlight(it, currentLocator.value.locations.progression!!)
            }
        }
        popupWindow?.dismiss()
        mode?.finish()
    }

    private fun showAnnotationPopup(highlight: org.readium.r2.navigator.epub.Highlight? = null) {
        launch {
            val view = layoutInflater.inflate(R.layout.popup_note, null, false)
            val alert = AlertDialog.Builder(this@EpubActivity)
                    .setView(view)
                    .create()

            val annotation = highlight
                    ?.let { viewModel.getHighlightByHighlightId(highlight.id)?.annotation }
                    .orEmpty()

            val note = view.findViewById<EditText>(R.id.note)

            fun dismiss() {
                alert.dismiss()
                mode?.finish()
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .hideSoftInputFromWindow(note.applicationWindowToken, InputMethodManager.HIDE_NOT_ALWAYS)
            }

            with(view) {
                if (highlight != null) {
                    findViewById<TextView>(R.id.select_text).text = highlight.locator.text.highlight
                    note.setText(annotation)
                } else {
                    currentSelection {
                        findViewById<TextView>(R.id.select_text).text = it?.text?.highlight
                    }
                }

                findViewById<TextView>(R.id.positive).setOnClickListener {
                    val text = note.text.toString()
                    val markStyle = if (text.isNotBlank()) "annotation" else ""

                    if (highlight != null) {
                        viewModel.updateHighlight(highlight.id, annotation = text, markStyle = markStyle)
                        launch {
                            val updatedHighlight = viewModel.getHighlightByHighlightId(highlight.id)
                            val navHighlight = updatedHighlight?.toNavigatorHighlight()
                            //FIXME: the annotation mark is not destroyed before reloading when the text becomes blank,
                            // probably because of an ID mismatch
                            if (navHighlight != null) {
                                showHighlight(navHighlight)
                            }
                        }
                    } else {
                        val progression = currentLocator.value.locations.progression!!
                        createAnnotation(highlight) {
                            val navHighlight = it.copy(annotationMarkStyle = markStyle)
                            viewModel.insertHighlight(navHighlight, progression, text)
                            showHighlight(navHighlight)
                        }
                    }
                    dismiss()
                }
                findViewById<TextView>(R.id.negative).setOnClickListener {
                    dismiss()
                }
            }

            alert.show()
        }
    }

    override fun onPageLoaded() {
        super.onPageLoaded()
        drawHighlight()
    }

    private fun drawHighlight() {
        val href = currentLocator.value.href
        viewModel.getHighlights(href).observe(this, {
            it.forEach { highlight ->
                showHighlight(highlight.toNavigatorHighlight())
            }
        })
    }

    override fun highlightActivated(id: String) {
        rectangleForHighlightWithID(id) { rect ->
            if (rect != null) {
                showHighlightPopup(id, rect)
            }
        }
    }

    override fun highlightAnnotationMarkActivated(id: String) {
        launch {
            val realId = id.replace("ANNOTATION", "HIGHLIGHT")
            val highlight = try {
                viewModel.getHighlightByHighlightId(realId)
            } catch (e: IllegalStateException) {
                return@launch // FIXME: This is required as long as the navigator highlight is not deleted
                // when the corresponding highlight in DB is.
            }
            if (highlight != null) {
                showAnnotationPopup(highlight.toNavigatorHighlight())
            }
        }
    }

    /**
     * Manage what happens when the focus is put back on the EpubActivity.
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
            if (publication.cssStyle != "cjk-vertical") {
                publication.userSettingsUIPreset.remove(ReadiumCSSName.ref(SCROLL_REF))
            }

            userSettings = UserSettings(preferences, this, publication.userSettingsUIPreset)
            userSettings.resourcePager = resourcePager
        }
    }

    /**
     * Communicate with the user using a toast if touch exploration is enabled, to indicate the end of a chapter.
     */
    override fun onPageEnded(end: Boolean) {
        if (isExploreByTouchEnabled) {
            if (!pageEnded == end && end) {
                Toast.makeText(this, getString(R.string.end_of_chapter), Toast.LENGTH_SHORT).show()
            }
            pageEnded = end
        }
    }

    private fun handleReaderFragmentEvent(event: ReaderViewModel.Event) {
        when(event) {
            is ReaderViewModel.Event.OpenOutlineRequested -> showOutlineFragment()
            is ReaderViewModel.Event.OpenDrmManagementRequested -> showDrmManagementFragment()
        }
    }

    private fun showOutlineFragment() {
        supportFragmentManager.commit {
            add(R.id.activity_container, OutlineFragment::class.java, Bundle(), OUTLINE_FRAGMENT_TAG)
            hide(readerFragment)
            addToBackStack(null)
        }
    }

    private fun closeOutlineFragment(locator: Locator) {
        readerFragment.go(locator, true)
        supportFragmentManager.popBackStack()
    }

    private fun showDrmManagementFragment() {
        supportFragmentManager.commit {
            add(R.id.activity_container, DrmManagementFragment::class.java, Bundle(), DRM_FRAGMENT_TAG)
            hide(readerFragment)
            addToBackStack(null)
        }
    }

    companion object {
        const val READER_FRAGMENT_TAG = "reader"
        const val OUTLINE_FRAGMENT_TAG = "outline"
        const val DRM_FRAGMENT_TAG = "drm"
    }
}
