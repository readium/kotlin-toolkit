package org.readium.r2.navigator

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.readium.r2.navigator.pager.R2ViewPager
import org.readium.r2.navigator.pager.R2WebView
import org.readium.r2.shared.*
import timber.log.Timber
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

const val FONT_SIZE_REF = "fontSize"
const val FONT_FAMILY_REF = "fontFamily"
const val FONT_OVERRIDE_REF = "fontOverride"
const val APPEARANCE_REF = "appearance"
const val SCROLL_REF = "scroll"
const val PUBLISHER_DEFAULT_REF = "advancedSettings"
const val TEXT_ALIGNMENT_REF = "textAlign"
const val COLUMN_COUNT_REF = "colCount"
const val WORD_SPACING_REF = "wordSpacing"
const val LETTER_SPACING_REF = "letterSpacing"
const val PAGE_MARGINS_REF = "pageMargins"
const val LINE_HEIGHT_REF = "lineHeight"

const val FONT_SIZE_NAME = "--USER__$FONT_SIZE_REF"
const val FONT_FAMILY_NAME = "--USER__$FONT_FAMILY_REF"
const val FONT_OVERRIDE_NAME = "--USER__$FONT_OVERRIDE_REF"
const val APPEARANCE_NAME = "--USER__$APPEARANCE_REF"
const val SCROLL_NAME = "--USER__$SCROLL_REF"
const val PUBLISHER_DEFAULT_NAME = "--USER__$PUBLISHER_DEFAULT_REF"
const val TEXT_ALIGNMENT_NAME = "--USER__$TEXT_ALIGNMENT_REF"
const val COLUMN_COUNT_NAME = "--USER__$COLUMN_COUNT_REF"
const val WORD_SPACING_NAME = "--USER__$WORD_SPACING_REF"
const val LETTER_SPACING_NAME = "--USER__$LETTER_SPACING_REF"
const val PAGE_MARGINS_NAME = "--USER__$PAGE_MARGINS_REF"
const val LINE_HEIGHT_NAME = "--USER__$LINE_HEIGHT_REF"

class UserSettings(var preferences: SharedPreferences, val context: Context) {

    private val TAG = this::class.java.simpleName

    lateinit var resourcePager: R2ViewPager

    private val appearanceValues = listOf("readium-default-on", "readium-sepia-on","readium-night-on")
    private val fontFamilyValues = listOf("Publisher's default", "sans-serif", "Roboto", "serif", "Seravek")
    private val textAlignmentValues = listOf("justify", "start")
    private val columnCountValues = listOf("auto", "1", "2")

    var fontSize = 100f
    var fontOverride = false
    var fontFamily = 0
    var appearance = 0
    var verticalScroll = false

    //Advanced settings
    var publisherDefaults = false
    var textAlignment = 0
    var columnCount = 0
    var wordSpacing = 0f
    var letterSpacing = 0f
    var pageMargins = 0.5f
    var lineHeight = 1f

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
    }

    fun getUserSettings() : UserProperties {

        val userProperties = UserProperties()
        // Publisher default system
        userProperties.addSwitchable("readium-advanced-off", "readium-advanced-on", publisherDefaults, PUBLISHER_DEFAULT_REF, PUBLISHER_DEFAULT_NAME)
        // Font override
        userProperties.addSwitchable("readium-font-on", "readium-font-off", fontOverride, FONT_OVERRIDE_REF, FONT_OVERRIDE_NAME)
        // Column count
        userProperties.addEnumeratable(columnCount, columnCountValues, COLUMN_COUNT_REF, COLUMN_COUNT_NAME)
        // Appearance
        userProperties.addEnumeratable(appearance, appearanceValues, APPEARANCE_REF, APPEARANCE_NAME)
        // Page margins
        userProperties.addIncrementable(pageMargins, 0.5f, 2f, 0.25f, "", PAGE_MARGINS_REF, PAGE_MARGINS_NAME)
        // Text alignment
        userProperties.addEnumeratable(textAlignment, textAlignmentValues, TEXT_ALIGNMENT_REF, TEXT_ALIGNMENT_NAME)
        // Font family
        userProperties.addEnumeratable(fontFamily, fontFamilyValues, FONT_FAMILY_REF, FONT_FAMILY_NAME)
        // Font size
        userProperties.addIncrementable(fontSize, 100f, 300f, 25f, "%", FONT_SIZE_REF, FONT_SIZE_NAME)
        // Line height
        userProperties.addIncrementable(lineHeight, 1f, 2f, 0.25f, "", LINE_HEIGHT_REF, LINE_HEIGHT_NAME)
        // Word spacing
        userProperties.addIncrementable(wordSpacing, 0f, 0.5f, 0.25f, "rem", WORD_SPACING_REF, WORD_SPACING_NAME)
        // Letter spacing
        userProperties.addIncrementable(letterSpacing, 0f, 0.5f, 0.0625f, "em", LETTER_SPACING_REF, LETTER_SPACING_NAME)
        // Scroll
        userProperties.addSwitchable("readium-scroll-on", "readium-scroll-off", verticalScroll, SCROLL_REF, SCROLL_NAME)

        return userProperties
    }

    private fun makeJson() : JSONArray {
        val array = JSONArray()
        for(userProperty in userProperties.properties){
            array.put(userProperty.getJson())
        }
        return array
    }

    private fun saveChanges() {
        val json = makeJson()
        val dir = File(context.getExternalFilesDir(null).path + "/styles/")
        dir.mkdirs()
        val file = File(dir, "UserProperties.json")
        file.printWriter().use { out ->
            out.println(json)
        }
    }

    private fun updateEnumeratable(enumeratable: Enumeratable) {
        preferences.edit().putInt(enumeratable.ref, enumeratable.index).apply()
        saveChanges()
    }


    private fun updateSwitchable(switchable: Switchable) {
        preferences.edit().putBoolean(switchable.ref, switchable.on).apply()
        saveChanges()
    }

    private fun updateIncrementable(incrementable: Incrementable) {
        preferences.edit().putFloat(incrementable.ref, incrementable.value).apply()
        saveChanges()
    }

    fun updateViewCSS(ref: String) {
        val webView = resourcePager.getFocusedChild().findViewById(R.id.webView) as R2WebView
        applyCSS(webView, ref)
    }

    private fun applyCSS(view: R2WebView, ref: String) {
        val userSetting = userProperties.getByRef<UserProperty>(ref)
        view.setProperty(userSetting.name, userSetting.toString())
        println("applyCss : " + userSetting.name + ": " + userSetting.toString())
    }

    fun applyAllCSS(view: R2WebView) {
        for (userSetting in userProperties.properties) {
            println(userSetting.name + ": " + userSetting.toString())
            view.setProperty("applyAllCss : " + userSetting.name, userSetting.toString())
        }
    }

    fun userSettingsPopUp(): PopupWindow {

        val layoutInflater = LayoutInflater.from(context)
        val layout = layoutInflater.inflate(R.layout.popup_window, null)
        val userSettingsPopup = PopupWindow(context)
        userSettingsPopup.setContentView(layout)
        userSettingsPopup.setWidth(ListPopupWindow.WRAP_CONTENT)
        userSettingsPopup.setHeight(ListPopupWindow.WRAP_CONTENT)
        userSettingsPopup.isOutsideTouchable = true
        userSettingsPopup.isFocusable = true

        val host = layout.findViewById(R.id.tabhost) as TabHost
        host.setup()

        //Tab 1
        var spec: TabHost.TabSpec = host.newTabSpec("Settings")
        spec.setContent(R.id.Settings)
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

        val fontFamily = (userProperties.getByRef<Enumeratable>(FONT_FAMILY_REF))
        val fontOverride = (userProperties.getByRef<Switchable>(FONT_OVERRIDE_REF))
        val appearance = userProperties.getByRef<Enumeratable>(APPEARANCE_REF)
        val fontSize = userProperties.getByRef<Incrementable>(FONT_SIZE_REF)
        val publisherDefault = userProperties.getByRef<Switchable>(PUBLISHER_DEFAULT_REF)
        val scrollMode = userProperties.getByRef<Switchable>(SCROLL_REF)
        val alignment = userProperties.getByRef<Enumeratable>(TEXT_ALIGNMENT_REF)
        val columnsCount = userProperties.getByRef<Enumeratable>(COLUMN_COUNT_REF)
        val pageMargins = userProperties.getByRef<Incrementable>(PAGE_MARGINS_REF)
        val wordSpacing = userProperties.getByRef<Incrementable>(WORD_SPACING_REF)
        val letterSpacing = userProperties.getByRef<Incrementable>(LETTER_SPACING_REF)
        val lineHeight = userProperties.getByRef<Incrementable>(LINE_HEIGHT_REF)

        val fontSpinner: Spinner = layout.findViewById(R.id.spinner_action_settings_intervall_values) as Spinner
        fontSpinner.setSelection(fontFamily.index)

        val fonts = context.getResources().getStringArray(R.array.font_list)

        val dataAdapter = object : ArrayAdapter<String>(context, R.layout.spinner_item, fonts) {

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                var v: View? = null
                v = super.getDropDownView(position, null, parent)
                // Makes the selected font appear in dark
                // If this is the selected item position
                if (position == fontFamily.index) {
                    v!!.setBackgroundColor(context.getResources().getColor(R.color.colorPrimaryDark))
                    v.findViewById<TextView>(android.R.id.text1).setTextColor(Color.WHITE)

                } else {
                    // for other views
                    v!!.setBackgroundColor(Color.WHITE)
                    v.findViewById<TextView>(android.R.id.text1).setTextColor(Color.BLACK)

                }
                return v
            }
        }

        fun findIndexOfId(id: Int, list: MutableList<RadioButton>) : Int {
            for (i in 0..list.size) {
                if (list[i].id == id) {
                    return i
                }
            }
            return 0
        }


        // Font family
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fontSpinner.setAdapter(dataAdapter)
        fontSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
                fontFamily.index = pos
                fontOverride.on = (pos != 0)
                updateSwitchable(fontOverride)
                updateEnumeratable(fontFamily)
                println("selected a font")
                updateViewCSS(FONT_OVERRIDE_REF)
                updateViewCSS(FONT_FAMILY_REF)
            }

            override fun onNothingSelected(parent: AdapterView<out Adapter>?) {
                // fontSpinner.setSelection(selectedFontIndex)
            }
        }


        // Appearance
        val appearanceGroup = layout.findViewById(R.id.appearance) as RadioGroup
        val appearanceRadios = mutableListOf<RadioButton>()
        appearanceRadios.add(layout.findViewById(R.id.appearance_default) as RadioButton)
        appearanceRadios.add(layout.findViewById(R.id.appearance_sepia) as RadioButton)
        appearanceRadios.add(layout.findViewById(R.id.appearance_night) as RadioButton)

        appearanceRadios[appearance.index].isChecked = true

        appearanceGroup.setOnCheckedChangeListener { radioGroup, id ->
            val i = findIndexOfId(id, appearanceRadios)
            appearance.index = i
            when (i) {
                0 -> {
                    resourcePager.setBackgroundColor(Color.parseColor("#ffffff"))
                    (resourcePager.focusedChild.findViewById(R.id.book_title) as TextView).setTextColor(Color.parseColor("#000000"))
                }
                1 -> {
                    resourcePager.setBackgroundColor(Color.parseColor("#faf4e8"))
                    (resourcePager.focusedChild.findViewById(R.id.book_title) as TextView).setTextColor(Color.parseColor("#000000"))
                }
                2 -> {
                    resourcePager.setBackgroundColor(Color.parseColor("#000000"))
                    (resourcePager.focusedChild.findViewById(R.id.book_title) as TextView).setTextColor(Color.parseColor("#ffffff"))
                }
            }
            updateEnumeratable(appearance)
            updateViewCSS(APPEARANCE_REF)
        }


        // Font size
        val fontDecreaseButton = layout.findViewById(R.id.font_decrease) as ImageButton
        val fontIncreaseButton = layout.findViewById(R.id.font_increase) as ImageButton
        fontDecreaseButton.setOnClickListener {
            fontSize.decrement()
            updateIncrementable(fontSize)
            updateViewCSS(FONT_SIZE_REF)
        }

        fontIncreaseButton.setOnClickListener {
            fontSize.increment()
            updateIncrementable(fontSize)
            updateViewCSS(FONT_SIZE_REF)
        }


        // Publisher defaults
        val publisherDefaultSwitch = layout.findViewById(R.id.publisher_default) as Switch
        publisherDefaultSwitch.isChecked = publisherDefault.on
        publisherDefaultSwitch.setOnCheckedChangeListener { compoundButton, b ->
            publisherDefault.on = b
            updateSwitchable(publisherDefault)
            updateViewCSS(PUBLISHER_DEFAULT_REF)
        }


        // Vertical scroll
        val scrollModeSwitch = layout.findViewById(R.id.scroll_mode) as Switch
        scrollModeSwitch.isChecked = scrollMode.on
        scrollModeSwitch.setOnCheckedChangeListener { compoundButton, b ->
            scrollMode.on = scrollModeSwitch.isChecked
            when (b) {
                true -> {
                    (resourcePager.focusedChild.findViewById(R.id.book_title) as TextView).visibility = View.GONE
                    resourcePager.focusedChild.setPadding(0, 5, 0, 5)
                }
                false -> {
                    (resourcePager.focusedChild.findViewById(R.id.book_title) as TextView).visibility = View.VISIBLE
                    resourcePager.focusedChild.setPadding(0, 30, 0, 30)
                }
            }
            updateSwitchable(scrollMode)
            updateViewCSS(SCROLL_REF)
        }


        // Text alignment
        val alignmentGroup = layout.findViewById(R.id.TextAlignment) as RadioGroup
        val alignmentRadios = mutableListOf<RadioButton>()
        alignmentRadios.add(layout.findViewById(R.id.alignment_left))
        alignmentRadios.add(layout.findViewById(R.id.alignment_justify))

        alignmentRadios[alignment.index].isChecked = true
        alignmentRadios[0].setCompoundDrawablesWithIntrinsicBounds(null,
                (if (alignment.index == 0) context.getDrawable(R.drawable.icon_justify_white) else context.getDrawable(R.drawable.icon_justify)),
                null, null)
        alignmentRadios[1].setCompoundDrawablesWithIntrinsicBounds(null,
                (if (alignment.index == 0) context.getDrawable(R.drawable.icon_left) else context.getDrawable(R.drawable.icon_left_white)),
                null, null)

        alignmentGroup.setOnCheckedChangeListener { radioGroup, i ->
            alignment.index = findIndexOfId(i, alignmentRadios)
            alignmentRadios[0].setCompoundDrawablesWithIntrinsicBounds(null,
                    (if (alignment.index == 0) context.getDrawable(R.drawable.icon_justify_white) else context.getDrawable(R.drawable.icon_justify)),
                    null, null)
            alignmentRadios[1].setCompoundDrawablesWithIntrinsicBounds(null,
                    (if (alignment.index == 0) context.getDrawable(R.drawable.icon_left) else context.getDrawable(R.drawable.icon_left_white)),
                    null, null)
            publisherDefaultSwitch.isChecked = false
            updateEnumeratable(alignment)
            updateViewCSS(TEXT_ALIGNMENT_REF)
        }


        // Column count
        val columnsCountGroup = layout.findViewById(R.id.columns) as RadioGroup
        val columnsRadios = mutableListOf<RadioButton>()
        columnsRadios.add(layout.findViewById(R.id.column_auto))
        columnsRadios.add(layout.findViewById(R.id.column_one))
        columnsRadios.add(layout.findViewById(R.id.column_two))
        columnsRadios[columnsCount.index].isChecked = true
        columnsCountGroup.setOnCheckedChangeListener { radioGroup, id ->
            val i = findIndexOfId(id, columnsRadios)
            columnsCount.index = i
            publisherDefaultSwitch.isChecked = false
            updateEnumeratable(columnsCount)
            updateViewCSS(COLUMN_COUNT_REF)
        }


        // Page margins
        val pageMarginsDecreaseButton = layout.findViewById(R.id.pm_decrease) as ImageButton
        val pageMarginsIncreaseButton = layout.findViewById(R.id.pm_increase) as ImageButton
        val pageMarginsDisplay = layout.findViewById(R.id.pm_display) as TextView
        pageMarginsDisplay.text = pageMargins.value.toString()

        pageMarginsDecreaseButton.setOnClickListener {
            pageMargins.decrement()
            pageMarginsDisplay.text = pageMargins.value.toString()
            publisherDefaultSwitch.isChecked = false
            updateIncrementable(pageMargins)
            updateViewCSS(PAGE_MARGINS_REF)
        }

        pageMarginsIncreaseButton.setOnClickListener {
            pageMargins.increment()
            pageMarginsDisplay.text = pageMargins.value.toString()
            publisherDefaultSwitch.isChecked = false
            updateIncrementable(pageMargins)
            updateViewCSS(PAGE_MARGINS_REF)
        }


        // Word spacing
        val wordSpacingDecreaseButton = layout.findViewById(R.id.ws_decrease) as ImageButton
        val wordSpacingIncreaseButton = layout.findViewById(R.id.ws_increase) as ImageButton
        val wordSpacingDisplay = layout.findViewById(R.id.ws_display) as TextView

        wordSpacingDisplay.text = (if (wordSpacing.value == wordSpacing.min) "auto" else wordSpacing.value.toString())
        wordSpacingDecreaseButton.setOnClickListener {
            wordSpacing.decrement()
            wordSpacingDisplay.text = (if (wordSpacing.value == wordSpacing.min) "auto" else wordSpacing.value.toString())
            publisherDefaultSwitch.isChecked = false
            updateIncrementable(wordSpacing)
            updateViewCSS(WORD_SPACING_REF)
        }

        wordSpacingIncreaseButton.setOnClickListener {
            wordSpacing.increment()
            wordSpacingDisplay.text = wordSpacing.value.toString()
            publisherDefaultSwitch.isChecked = false
            updateIncrementable(wordSpacing)
            updateViewCSS(WORD_SPACING_REF)
        }


        // Letter spacing
        val letterSpacingDecreaseButton = layout.findViewById(R.id.ls_decrease) as ImageButton
        val letterSpacingIncreaseButton = layout.findViewById(R.id.ls_increase) as ImageButton
        val letterSpacingDisplay = layout.findViewById(R.id.ls_display) as TextView

        letterSpacingDisplay.text = (if (letterSpacing.value == letterSpacing.min) "auto" else letterSpacing.value.toString())

        letterSpacingDecreaseButton.setOnClickListener {
            letterSpacing.decrement()
            letterSpacingDisplay.text = (if (letterSpacing.value == letterSpacing.min) "auto" else letterSpacing.value.toString())
            publisherDefaultSwitch.isChecked = false
            updateIncrementable(letterSpacing)
            updateViewCSS(LETTER_SPACING_REF)
        }

        letterSpacingIncreaseButton.setOnClickListener {
            letterSpacing.increment()
            letterSpacingDisplay.text = (if (letterSpacing.value == letterSpacing.min) "auto" else letterSpacing.value.toString())
            publisherDefaultSwitch.isChecked = false
            updateIncrementable(letterSpacing)
            updateViewCSS(LETTER_SPACING_REF)
        }


        // Line height
        val lineHeightDecreaseButton = layout.findViewById(R.id.lh_decrease) as ImageButton
        val lineHeightIncreaseButton = layout.findViewById(R.id.lh_increase) as ImageButton
        val lineHeightDisplay = layout.findViewById(R.id.lh_display) as TextView

        lineHeightDisplay.text = (if (lineHeight.value == lineHeight.min) "auto" else lineHeight.value.toString())
        lineHeightDecreaseButton.setOnClickListener {
            lineHeight.decrement()
            lineHeightDisplay.text = (if (lineHeight.value == lineHeight.min) "auto" else lineHeight.value.toString())
            publisherDefaultSwitch.isChecked = false
            updateIncrementable(lineHeight)
            updateViewCSS(LINE_HEIGHT_REF)
        }
        lineHeightIncreaseButton.setOnClickListener {
            lineHeight.increment()
            lineHeightDisplay.text = (if (lineHeight.value == lineHeight.min) "auto" else lineHeight.value.toString())
            publisherDefaultSwitch.isChecked = false
            updateIncrementable(lineHeight)
            updateViewCSS(LINE_HEIGHT_REF)
        }

        // Brightness
        val brightnessSeekbar = layout.findViewById(R.id.brightness) as SeekBar
        val brightness = preferences.getInt("reader_brightness", 50)
        run {
            val backLightValue = brightness.toFloat() / 100
            val layoutParams = (context as R2EpubActivity).window.attributes
            layoutParams.screenBrightness = backLightValue
            context.window.attributes = layoutParams
        }
        brightnessSeekbar.setProgress(brightness)
        brightnessSeekbar.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(bar: SeekBar, progress: Int, from_user: Boolean) {
                        val backLightValue = progress.toFloat() / 100
                        val layoutParams = (context as R2EpubActivity).window.getAttributes()
                        layoutParams.screenBrightness = backLightValue
                        context.window.setAttributes(layoutParams)
                        preferences.edit().putInt("reader_brightness", progress).apply()
                    }

                    override fun onStartTrackingTouch(bar: SeekBar) {
                        // Nothing
                    }

                    override fun onStopTrackingTouch(bar: SeekBar) {
                        // Nothing
                    }
                })

        return userSettingsPopup
    }
}