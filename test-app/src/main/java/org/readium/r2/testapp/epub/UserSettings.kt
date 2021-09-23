/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mostapha Idoubihi, Paul Stoica
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp.epub

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.readium.r2.navigator.R2BasicWebView
import org.readium.r2.navigator.R2WebView
import org.readium.r2.navigator.epub.fxl.R2FXLLayout
import org.readium.r2.navigator.pager.R2EpubPageFragment
import org.readium.r2.navigator.pager.R2PagerAdapter
import org.readium.r2.navigator.pager.R2ViewPager
import org.readium.r2.shared.*
import org.readium.r2.testapp.R
import org.readium.r2.testapp.databinding.PopupWindowUserSettingsBinding
import org.readium.r2.testapp.utils.extensions.color
import java.io.File

class UserSettings(var preferences: SharedPreferences, val context: Context, private val UIPreset: MutableMap<ReadiumCSSName, Boolean>) {

    lateinit var resourcePager: R2ViewPager

    private val appearanceValues = listOf("readium-default-on", "readium-sepia-on", "readium-night-on")
    private val fontFamilyValues = listOf("Original", "PT Serif", "Roboto", "Source Sans Pro", "Vollkorn", "OpenDyslexic", "AccessibleDfA", "IA Writer Duospace")
    private val textAlignmentValues = listOf("justify", "start")
    private val columnCountValues = listOf("auto", "1", "2")

    private var fontSize = 100f
    private var fontOverride = false
    private var fontFamily = 0
    private var appearance = 0
    private var verticalScroll = false

    //Advanced settings
    private var publisherDefaults = false
    private var textAlignment = 0
    private var columnCount = 0
    private var wordSpacing = 0f
    private var letterSpacing = 0f
    private var pageMargins = 2f
    private var lineHeight = 1f

    private var userProperties: UserProperties

    init {
        appearance = preferences.getInt(APPEARANCE_REF, appearance)
        verticalScroll = preferences.getBoolean(SCROLL_REF, verticalScroll)
        fontFamily = preferences.getInt(FONT_FAMILY_REF, fontFamily)
        if (fontFamily != 0) {
            fontOverride = true
        }
        publisherDefaults = preferences.getBoolean(PUBLISHER_DEFAULT_REF, publisherDefaults)
        textAlignment = preferences.getInt(TEXT_ALIGNMENT_REF, textAlignment)
        columnCount = preferences.getInt(COLUMN_COUNT_REF, columnCount)


        fontSize = preferences.getFloat(FONT_SIZE_REF, fontSize)
        wordSpacing = preferences.getFloat(WORD_SPACING_REF, wordSpacing)
        letterSpacing = preferences.getFloat(LETTER_SPACING_REF, letterSpacing)
        pageMargins = preferences.getFloat(PAGE_MARGINS_REF, pageMargins)
        lineHeight = preferences.getFloat(LINE_HEIGHT_REF, lineHeight)
        userProperties = getUserSettings()

        //Setting up screen brightness
        val backLightValue = preferences.getInt("reader_brightness", 50).toFloat() / 100
        val layoutParams = (context as AppCompatActivity).window.attributes
        layoutParams.screenBrightness = backLightValue
        context.window.attributes = layoutParams
    }

    private fun getUserSettings(): UserProperties {

        val userProperties = UserProperties()
        // Publisher default system
        userProperties.addSwitchable("readium-advanced-off", "readium-advanced-on", publisherDefaults, PUBLISHER_DEFAULT_REF, PUBLISHER_DEFAULT_NAME)
        // Font override
        userProperties.addSwitchable("readium-font-on", "readium-font-off", fontOverride, FONT_OVERRIDE_REF, FONT_OVERRIDE_NAME)
        // Column count
        userProperties.addEnumerable(columnCount, columnCountValues, COLUMN_COUNT_REF, COLUMN_COUNT_NAME)
        // Appearance
        userProperties.addEnumerable(appearance, appearanceValues, APPEARANCE_REF, APPEARANCE_NAME)
        // Page margins
        userProperties.addIncremental(pageMargins, 0.5f, 4f, 0.25f, "", PAGE_MARGINS_REF, PAGE_MARGINS_NAME)
        // Text alignment
        userProperties.addEnumerable(textAlignment, textAlignmentValues, TEXT_ALIGNMENT_REF, TEXT_ALIGNMENT_NAME)
        // Font family
        userProperties.addEnumerable(fontFamily, fontFamilyValues, FONT_FAMILY_REF, FONT_FAMILY_NAME)
        // Font size
        userProperties.addIncremental(fontSize, 100f, 300f, 25f, "%", FONT_SIZE_REF, FONT_SIZE_NAME)
        // Line height
        userProperties.addIncremental(lineHeight, 1f, 2f, 0.25f, "", LINE_HEIGHT_REF, LINE_HEIGHT_NAME)
        // Word spacing
        userProperties.addIncremental(wordSpacing, 0f, 0.5f, 0.25f, "rem", WORD_SPACING_REF, WORD_SPACING_NAME)
        // Letter spacing
        userProperties.addIncremental(letterSpacing, 0f, 0.5f, 0.0625f, "em", LETTER_SPACING_REF, LETTER_SPACING_NAME)
        // Scroll
        userProperties.addSwitchable("readium-scroll-on", "readium-scroll-off", verticalScroll, SCROLL_REF, SCROLL_NAME)

        return userProperties
    }

    private fun makeJson(): JSONArray {
        val array = JSONArray()
        for (userProperty in userProperties.properties) {
            array.put(userProperty.getJson())
        }
        return array
    }


    fun saveChanges() {
        val json = makeJson()
        val dir = File(context.filesDir.path + "/" + Injectable.Style.rawValue + "/")
        dir.mkdirs()
        val file = File(dir, "UserProperties.json")
        file.printWriter().use { out ->
            out.println(json)
        }
    }

    private fun updateEnumerable(enumerable: Enumerable) {
        preferences.edit().putInt(enumerable.ref, enumerable.index).apply()
        saveChanges()
    }


    private fun updateSwitchable(switchable: Switchable) {
        preferences.edit().putBoolean(switchable.ref, switchable.on).apply()
        saveChanges()
    }

    private fun updateIncremental(incremental: Incremental) {
        preferences.edit().putFloat(incremental.ref, incremental.value).apply()
        saveChanges()
    }

    fun updateViewCSS(ref: String) {
        for (i in 0 until resourcePager.childCount) {
            val webView = resourcePager.getChildAt(i).findViewById(R.id.webView) as? R2WebView
            webView?.let {
                applyCSS(webView, ref)
            } ?: run {
                val zoomView = resourcePager.getChildAt(i).findViewById(R.id.r2FXLLayout) as R2FXLLayout
                val webView1 = zoomView.findViewById(R.id.firstWebView) as? R2BasicWebView
                val webView2 = zoomView.findViewById(R.id.secondWebView) as? R2BasicWebView
                val webViewSingle = zoomView.findViewById(R.id.webViewSingle) as? R2BasicWebView

                webView1?.let {
                    applyCSS(webView1, ref)
                }
                webView2?.let {
                    applyCSS(webView2, ref)
                }
                webViewSingle?.let {
                    applyCSS(webViewSingle, ref)
                }
            }
        }
    }

    private fun applyCSS(view: R2BasicWebView, ref: String) {
        val userSetting = userProperties.getByRef<UserProperty>(ref)
        view.setProperty(userSetting.name, userSetting.toString())
    }

    // There isn't an easy way to migrate from TabHost/TabWidget to TabLayout
    @Suppress("DEPRECATION")
    fun userSettingsPopUp(): PopupWindow {

        val layoutInflater = LayoutInflater.from(context)
        val layout = PopupWindowUserSettingsBinding.inflate(layoutInflater)
        val userSettingsPopup = PopupWindow(context)
        userSettingsPopup.contentView = layout.root
        userSettingsPopup.width = ListPopupWindow.WRAP_CONTENT
        userSettingsPopup.height = ListPopupWindow.WRAP_CONTENT
        userSettingsPopup.isOutsideTouchable = true
        userSettingsPopup.isFocusable = true

        val host = layout.tabhost
        host.setup()

        //Tab 1
        var spec: TabHost.TabSpec = host.newTabSpec("Settings")
        spec.setContent(R.id.SettingsTab)
        spec.setIndicator("Settings")
        host.addTab(spec)

        //Tab 2
        spec = host.newTabSpec("Advanced")
        spec.setContent(R.id.Advanced)
        spec.setIndicator("Advanced")
        host.addTab(spec)

        val tw = host.findViewById(android.R.id.tabs) as TabWidget
        (tw.getChildTabViewAt(0).findViewById(android.R.id.title) as TextView).textSize = 10f
        (tw.getChildTabViewAt(1).findViewById(android.R.id.title) as TextView).textSize = 10f

        val fontFamily = (userProperties.getByRef<Enumerable>(FONT_FAMILY_REF))
        val fontOverride = (userProperties.getByRef<Switchable>(FONT_OVERRIDE_REF))
        val appearance = userProperties.getByRef<Enumerable>(APPEARANCE_REF)
        val fontSize = userProperties.getByRef<Incremental>(FONT_SIZE_REF)
        val publisherDefault = userProperties.getByRef<Switchable>(PUBLISHER_DEFAULT_REF)
        val scrollMode = userProperties.getByRef<Switchable>(SCROLL_REF)
        val alignment = userProperties.getByRef<Enumerable>(TEXT_ALIGNMENT_REF)
        val columnsCount = userProperties.getByRef<Enumerable>(COLUMN_COUNT_REF)
        val pageMargins = userProperties.getByRef<Incremental>(PAGE_MARGINS_REF)
        val wordSpacing = userProperties.getByRef<Incremental>(WORD_SPACING_REF)
        val letterSpacing = userProperties.getByRef<Incremental>(LETTER_SPACING_REF)
        val lineHeight = userProperties.getByRef<Incremental>(LINE_HEIGHT_REF)

        val fontSpinner: Spinner = layout.spinnerActionSettingsIntervallValues

        val fonts = context.resources.getStringArray(R.array.font_list)

        val dataAdapter = object : ArrayAdapter<String>(context, R.layout.item_spinner_font, fonts) {

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v: View? = super.getDropDownView(position, null, parent)
                // Makes the selected font appear in dark
                // If this is the selected item position
                if (position == fontFamily.index) {
                    v!!.setBackgroundColor(context.color(R.color.colorPrimaryDark))
                    v.findViewById<TextView>(android.R.id.text1).setTextColor(Color.WHITE)

                } else {
                    // for other views
                    v!!.setBackgroundColor(Color.WHITE)
                    v.findViewById<TextView>(android.R.id.text1).setTextColor(Color.BLACK)

                }
                return v
            }
        }

        fun findIndexOfId(id: Int, list: MutableList<RadioButton>): Int {
            for (i in 0..list.size) {
                if (list[i].id == id) {
                    return i
                }
            }
            return 0
        }


        // Font family
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fontSpinner.adapter = dataAdapter
        fontSpinner.setSelection(fontFamily.index)
        fontSpinner.contentDescription = "Font Family"
        fontSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
                fontFamily.index = pos
                fontOverride.on = (pos != 0)
                updateSwitchable(fontOverride)
                updateEnumerable(fontFamily)
                updateViewCSS(FONT_OVERRIDE_REF)
                updateViewCSS(FONT_FAMILY_REF)
            }

            override fun onNothingSelected(parent: AdapterView<out Adapter>?) {
                // fontSpinner.setSelection(selectedFontIndex)
            }
        }


        // Appearance
        val appearanceGroup = layout.appearance
        val appearanceRadios = mutableListOf<RadioButton>()
        appearanceRadios.add(layout.appearanceDefault)
        layout.appearanceDefault.contentDescription = "Appearance Default"
        appearanceRadios.add(layout.appearanceSepia)
        layout.appearanceSepia.contentDescription = "Appearance Sepia"
        appearanceRadios.add(layout.appearanceNight)
        layout.appearanceNight.contentDescription = "Appearance Night"

        UIPreset[ReadiumCSSName.appearance]?.let {
            appearanceGroup.isEnabled = false
            for (appearanceRadio in appearanceRadios) {
                appearanceRadio.isEnabled = false
            }
        } ?: run {
            appearanceRadios[appearance.index].isChecked = true

            appearanceGroup.setOnCheckedChangeListener { _, id ->
                val i = findIndexOfId(id, list = appearanceRadios)
                appearance.index = i
                when (i) {
                    0 -> {
                        resourcePager.setBackgroundColor(Color.parseColor("#ffffff"))
                        //(resourcePager.focusedChild?.findViewById(R.id.book_title) as? TextView)?.setTextColor(Color.parseColor("#000000"))
                    }
                    1 -> {
                        resourcePager.setBackgroundColor(Color.parseColor("#faf4e8"))
                        //(resourcePager.focusedChild?.findViewById(R.id.book_title) as? TextView)?.setTextColor(Color.parseColor("#000000"))
                    }
                    2 -> {
                        resourcePager.setBackgroundColor(Color.parseColor("#000000"))
                        //(resourcePager.focusedChild?.findViewById(R.id.book_title) as? TextView)?.setTextColor(Color.parseColor("#ffffff"))
                    }
                }
                updateEnumerable(appearance)
                updateViewCSS(APPEARANCE_REF)
            }
        }


        // Font size
        val fontDecreaseButton = layout.fontDecrease
        val fontIncreaseButton = layout.fontIncrease

        UIPreset[ReadiumCSSName.fontSize]?.let {
            fontDecreaseButton.isEnabled = false
            fontIncreaseButton.isEnabled = false
        } ?: run {
            fontDecreaseButton.setOnClickListener {
                fontSize.decrement()
                updateIncremental(fontSize)
                updateViewCSS(FONT_SIZE_REF)
            }

            fontIncreaseButton.setOnClickListener {
                fontSize.increment()
                updateIncremental(fontSize)
                updateViewCSS(FONT_SIZE_REF)
            }
        }


        // Publisher defaults
        val publisherDefaultSwitch = layout.publisherDefault
        publisherDefaultSwitch.contentDescription = "\u00A0"

        publisherDefaultSwitch.isChecked = publisherDefault.on
        publisherDefaultSwitch.setOnCheckedChangeListener { _, b ->
            publisherDefault.on = b
            updateSwitchable(publisherDefault)
            updateViewCSS(PUBLISHER_DEFAULT_REF)
        }


        // Vertical scroll
        val scrollModeSwitch = layout.scrollMode
        UIPreset[ReadiumCSSName.scroll]?.let { isSet ->
            scrollModeSwitch.isChecked = isSet
            scrollModeSwitch.isEnabled = false
        } ?: run {
            scrollModeSwitch.isChecked = scrollMode.on
            scrollModeSwitch.setOnCheckedChangeListener { _, b ->
                scrollMode.on = scrollModeSwitch.isChecked

                updateSwitchable(scrollMode)
                updateViewCSS(SCROLL_REF)

                val currentFragment = (resourcePager.adapter as R2PagerAdapter).getCurrentFragment()
                val previousFragment = (resourcePager.adapter as R2PagerAdapter).getPreviousFragment()
                val nextFragment = (resourcePager.adapter as R2PagerAdapter).getNextFragment()
                if (currentFragment is R2EpubPageFragment) {
                    currentFragment.webView?.let { webView ->
                        webView.scrollToPosition(webView.progression)
                        (previousFragment as? R2EpubPageFragment)?.webView?.scrollToEnd()
                        (nextFragment as? R2EpubPageFragment)?.webView?.scrollToStart()
                        webView.setScrollMode(b)
                        (previousFragment as? R2EpubPageFragment)?.webView?.setScrollMode(b)
                        (nextFragment as? R2EpubPageFragment)?.webView?.setScrollMode(b)
                    }
                }
            }
        }


        // Text alignment
        val alignmentGroup = layout.TextAlignment
        val alignmentRadios = mutableListOf<RadioButton>()
        alignmentRadios.add(layout.alignmentLeft)
        (layout.alignmentLeft).contentDescription = "Alignment Left"

        alignmentRadios.add(layout.alignmentJustify)
        layout.alignmentJustify.contentDescription = "Alignment Justified"

        UIPreset[ReadiumCSSName.textAlignment]?.let {
            alignmentGroup.isEnabled = false
            alignmentGroup.isActivated = false
            for (alignmentRadio in alignmentRadios) {
                alignmentRadio.isEnabled = false
            }
        } ?: run {
            alignmentRadios[alignment.index].isChecked = true

            alignmentGroup.setOnCheckedChangeListener { _, i ->
                alignment.index = findIndexOfId(i, alignmentRadios)
                publisherDefaultSwitch.isChecked = false
                updateEnumerable(alignment)
                updateViewCSS(TEXT_ALIGNMENT_REF)
            }
        }


        // Column count
        val columnsCountGroup = layout.columns
        val columnsRadios = mutableListOf<RadioButton>()
        columnsRadios.add(layout.columnAuto)
        layout.columnAuto.contentDescription = "Columns Auto"

        columnsRadios.add(layout.columnOne)
        layout.columnOne.contentDescription = "Columns 1"

        columnsRadios.add(layout.columnTwo)
        layout.columnTwo.contentDescription = "Columns 2"

        UIPreset[ReadiumCSSName.columnCount]?.let {
            columnsCountGroup.isEnabled = false
            columnsCountGroup.isActivated = false
            for (columnRadio in columnsRadios) {
                columnRadio.isEnabled = false
            }
        } ?: run {
            columnsRadios[columnsCount.index].isChecked = true
            columnsCountGroup.setOnCheckedChangeListener { _, id ->
                val i = findIndexOfId(id, columnsRadios)
                columnsCount.index = i
                publisherDefaultSwitch.isChecked = false
                updateEnumerable(columnsCount)
                updateViewCSS(COLUMN_COUNT_REF)
            }
        }


        // Page margins
        val pageMarginsDecreaseButton = layout.pmDecrease
        val pageMarginsIncreaseButton = layout.pmIncrease
        val pageMarginsDisplay = layout.pmDisplay
        pageMarginsDisplay.text = pageMargins.value.toString()

        UIPreset[ReadiumCSSName.pageMargins]?.let {
            pageMarginsDecreaseButton.isEnabled = false
            pageMarginsIncreaseButton.isEnabled = false
        } ?: run {
            pageMarginsDecreaseButton.setOnClickListener {
                pageMargins.decrement()
                pageMarginsDisplay.text = pageMargins.value.toString()
                publisherDefaultSwitch.isChecked = false
                updateIncremental(pageMargins)
                updateViewCSS(PAGE_MARGINS_REF)
            }

            pageMarginsIncreaseButton.setOnClickListener {
                pageMargins.increment()
                pageMarginsDisplay.text = pageMargins.value.toString()
                publisherDefaultSwitch.isChecked = false
                updateIncremental(pageMargins)
                updateViewCSS(PAGE_MARGINS_REF)
            }
        }


        // Word spacing
        val wordSpacingDecreaseButton = layout.wsDecrease
        val wordSpacingIncreaseButton = layout.wsIncrease
        val wordSpacingDisplay = layout.wsDisplay
        wordSpacingDisplay.text = (if (wordSpacing.value == wordSpacing.min) "auto" else wordSpacing.value.toString())

        UIPreset[ReadiumCSSName.wordSpacing]?.let {
            wordSpacingDecreaseButton.isEnabled = false
            wordSpacingIncreaseButton.isEnabled = false
        } ?: run {
            wordSpacingDecreaseButton.setOnClickListener {
                wordSpacing.decrement()
                wordSpacingDisplay.text = (if (wordSpacing.value == wordSpacing.min) "auto" else wordSpacing.value.toString())
                publisherDefaultSwitch.isChecked = false
                updateIncremental(wordSpacing)
                updateViewCSS(WORD_SPACING_REF)
            }

            wordSpacingIncreaseButton.setOnClickListener {
                wordSpacing.increment()
                wordSpacingDisplay.text = wordSpacing.value.toString()
                publisherDefaultSwitch.isChecked = false
                updateIncremental(wordSpacing)
                updateViewCSS(WORD_SPACING_REF)
            }
        }

        // Letter spacing
        val letterSpacingDecreaseButton = layout.lsDecrease
        val letterSpacingIncreaseButton = layout.lsIncrease
        val letterSpacingDisplay = layout.lsDisplay
        letterSpacingDisplay.text = (if (letterSpacing.value == letterSpacing.min) "auto" else letterSpacing.value.toString())

        UIPreset[ReadiumCSSName.letterSpacing]?.let {
            letterSpacingDecreaseButton.isEnabled = false
            letterSpacingIncreaseButton.isEnabled = false
        } ?: run {
            letterSpacingDecreaseButton.setOnClickListener {
                letterSpacing.decrement()
                letterSpacingDisplay.text = (if (letterSpacing.value == letterSpacing.min) "auto" else letterSpacing.value.toString())
                publisherDefaultSwitch.isChecked = false
                updateIncremental(letterSpacing)
                updateViewCSS(LETTER_SPACING_REF)
            }

            letterSpacingIncreaseButton.setOnClickListener {
                letterSpacing.increment()
                letterSpacingDisplay.text = (if (letterSpacing.value == letterSpacing.min) "auto" else letterSpacing.value.toString())
                publisherDefaultSwitch.isChecked = false
                updateIncremental(letterSpacing)
                updateViewCSS(LETTER_SPACING_REF)
            }
        }


        // Line height
        val lineHeightDecreaseButton = layout.lhDecrease
        val lineHeightIncreaseButton = layout.lhIncrease
        val lineHeightDisplay = layout.lhDisplay
        lineHeightDisplay.text = (if (lineHeight.value == lineHeight.min) "auto" else lineHeight.value.toString())

        UIPreset[ReadiumCSSName.lineHeight]?.let {
            lineHeightDecreaseButton.isEnabled = false
            lineHeightIncreaseButton.isEnabled = false
        } ?: run {
            lineHeightDecreaseButton.setOnClickListener {
                lineHeight.decrement()
                lineHeightDisplay.text = (if (lineHeight.value == lineHeight.min) "auto" else lineHeight.value.toString())
                publisherDefaultSwitch.isChecked = false
                updateIncremental(lineHeight)
                updateViewCSS(LINE_HEIGHT_REF)
            }
            lineHeightIncreaseButton.setOnClickListener {
                lineHeight.increment()
                lineHeightDisplay.text = (if (lineHeight.value == lineHeight.min) "auto" else lineHeight.value.toString())
                publisherDefaultSwitch.isChecked = false
                updateIncremental(lineHeight)
                updateViewCSS(LINE_HEIGHT_REF)
            }
        }

        // Brightness
        val brightnessSeekbar = layout.brightness
        val brightness = preferences.getInt("reader_brightness", 50)
        brightnessSeekbar.progress = brightness
        brightnessSeekbar.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(bar: SeekBar, progress: Int, from_user: Boolean) {
                        val backLightValue = progress.toFloat() / 100
                        val layoutParams = (context as AppCompatActivity).window.attributes
                        layoutParams.screenBrightness = backLightValue
                        context.window.attributes = layoutParams
                        preferences.edit().putInt("reader_brightness", progress).apply()
                    }

                    override fun onStartTrackingTouch(bar: SeekBar) {
                        // Nothing
                    }

                    override fun onStopTrackingTouch(bar: SeekBar) {
                        // Nothing
                    }
                })

        // Speech speed
        val speechSeekBar = layout.TTSSpeechSpeed

        //Get the user settings value or set the progress bar to a neutral position (1 time speech speed).
        val speed = preferences.getInt("reader_TTS_speed", (2.75 * 3.toDouble() / 11.toDouble() * 100).toInt())

        speechSeekBar.progress = speed
        speechSeekBar.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {

                    override fun onProgressChanged(bar: SeekBar, progress: Int, from_user: Boolean) {
                        // Nothing
                    }

                    override fun onStartTrackingTouch(bar: SeekBar) {
                        // Nothing
                    }

                    override fun onStopTrackingTouch(bar: SeekBar) {
                        preferences.edit().putInt("reader_TTS_speed", bar.progress).apply()
                    }
                })

        return userSettingsPopup
    }
}