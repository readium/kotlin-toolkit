package org.readium.r2.navigator.UserSettings

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.readium.r2.navigator.R
import org.readium.r2.navigator.R2EpubActivity
import org.readium.r2.navigator.pager.R2ViewPager
import org.readium.r2.navigator.pager.R2WebView
import timber.log.Timber

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


class UserSettings(var preferences: SharedPreferences, val context: Context) {

    private val TAG = this::class.java.simpleName

    data class UserSetting(var ref: String, var name: String, var value: String)

    lateinit var resourcePager: R2ViewPager
    val properties = mutableListOf<String>()

    var fontSize = FontSize(FontSize.min)
    var fontOverride = FontOverride.Off.toString()
    var fontFamily = FontFamily.Publisher.toString()
    var appearance = Appearance.Default.toString()
    var verticalScroll = Scroll.Off.toString()

    //Advanced settings
    var publisherSettings = PublisherDefault.On.toString()
    var textAlignment = TextAlignment.Justify.toString()
    var columnCount = ColumnCount.Auto.toString()
    var wordSpacing = WordSpacing(WordSpacing.min)
    var letterSpacing = LetterSpacing(LetterSpacing.min)
    var pageMargins = PageMargins(PageMargins.min)

    var isVerticalScrollEnabled: Boolean = false
        get() {
            return verticalScroll == Scroll.On.toString()
        }

    init {
        appearance = preferences.getString(APPEARANCE_REF, appearance)
        verticalScroll = preferences.getString(SCROLL_REF, verticalScroll)
        fontFamily = preferences.getString(FONT_FAMILY_REF, fontFamily)
        if (fontFamily != FontFamily.Publisher.toString()) {
            fontOverride = FontOverride.On.toString()
        }
        publisherSettings = preferences.getString(PUBLISHER_DEFAULT_REF, publisherSettings)
        textAlignment = preferences.getString(TEXT_ALIGNMENT_REF, textAlignment)
        columnCount = preferences.getString(COLUMN_COUNT_REF, columnCount)


        fontSize = FontSize(preferences.getString(FONT_SIZE_REF, fontSize.value.toString()).toInt())
        wordSpacing = WordSpacing(preferences.getString(WORD_SPACING_REF, wordSpacing.value.toString()).toFloat())
        letterSpacing = LetterSpacing(preferences.getString(LETTER_SPACING_REF, letterSpacing.value.toString()).toFloat())
        pageMargins = PageMargins(preferences.getString(PAGE_MARGINS_REF, pageMargins.value.toString()).toFloat())
    }

    fun getUserSettings(): List<UserSetting> {
        val properties = mutableListOf<UserSetting>()

        properties.add(UserSetting(FONT_SIZE_REF, FONT_SIZE_NAME, fontSize.toString()))
        properties.add(UserSetting(FONT_OVERRIDE_REF, FONT_OVERRIDE_NAME, fontOverride))
        if (fontOverride == FontOverride.On.toString()) {
            properties.add(UserSetting(FONT_FAMILY_REF, FONT_FAMILY_NAME, fontFamily))
        }
        //Appearance (default, sepia, night)
        properties.add(UserSetting(APPEARANCE_REF, APPEARANCE_NAME, appearance))
        //Scroll
        properties.add(UserSetting(SCROLL_REF, SCROLL_NAME, verticalScroll))
        //Advanced settings
        properties.add(UserSetting(PUBLISHER_DEFAULT_REF, PUBLISHER_DEFAULT_NAME, publisherSettings))

        properties.add(UserSetting(TEXT_ALIGNMENT_REF, TEXT_ALIGNMENT_NAME, textAlignment))
        properties.add(UserSetting(COLUMN_COUNT_REF, COLUMN_COUNT_NAME, columnCount))
        properties.add(UserSetting(WORD_SPACING_REF, WORD_SPACING_NAME, wordSpacing.toString()))
        properties.add(UserSetting(LETTER_SPACING_REF, LETTER_SPACING_NAME, letterSpacing.toString()))
        properties.add(UserSetting(PAGE_MARGINS_REF, PAGE_MARGINS_NAME, pageMargins.toString()))

        return properties
    }


    fun currentFontSize(): UserSetting {
        return UserSetting(FONT_SIZE_REF, FONT_SIZE_NAME, fontSize.value.toString())
    }

    fun updateFontSize(setting: UserSetting) {
        fontSize = FontSize(setting.value.toInt())
        updateProperties(setting)
    }

    fun currentFontFamily(): UserSetting {
        return UserSetting(FONT_FAMILY_REF, FONT_FAMILY_NAME, fontFamily)
    }

    fun updateFontFamily(setting: UserSetting) {
        if (!properties.contains(FONT_OVERRIDE_NAME)) {
            properties.add(FONT_OVERRIDE_NAME)
        }
        if (setting.value == FontFamily.Publisher.toString()) {
            fontOverride = FontOverride.Off.toString()
        } else {
            fontOverride = FontOverride.On.toString()
            fontFamily = setting.value
        }
        updateProperties(setting)
    }

    fun currentAppearance(): UserSetting {
        return UserSetting(APPEARANCE_REF, APPEARANCE_NAME, appearance)
    }

    fun updateAppearance(setting: UserSetting) {
        appearance = setting.value
        updateProperties(setting)
    }

    fun currentScrollMode(): UserSetting {
        return UserSetting(SCROLL_REF, SCROLL_NAME, verticalScroll)
    }

    fun updateScrollMode(setting: UserSetting) {
        verticalScroll = setting.value
        updateProperties(setting)
    }

    fun currentTextAlignment(): UserSetting {
        return UserSetting(TEXT_ALIGNMENT_REF, TEXT_ALIGNMENT_NAME, textAlignment)
    }

    fun updateTextAlignment(setting: UserSetting) {
        if (!properties.contains(PUBLISHER_DEFAULT_NAME)) {
            properties.add(PUBLISHER_DEFAULT_NAME)
        }
        textAlignment = setting.value
        publisherSettings = PublisherDefault.Off.toString()
        updateProperties(setting)
    }

    fun currentColumnCount(): UserSetting {
        return UserSetting(COLUMN_COUNT_REF, COLUMN_COUNT_NAME, columnCount)
    }

    fun updateColumnCount(setting: UserSetting) {
        if (!properties.contains(PUBLISHER_DEFAULT_NAME)) {
            properties.add(PUBLISHER_DEFAULT_NAME)
        }
        columnCount = setting.value
        publisherSettings = PublisherDefault.Off.toString()
        updateProperties(setting)
    }

    fun currentPageMargins(): UserSetting {
        return UserSetting(PAGE_MARGINS_REF, PAGE_MARGINS_NAME, pageMargins.value.toString())
    }

    fun updatePageMargins(setting: UserSetting) {
        if (!properties.contains(PUBLISHER_DEFAULT_NAME)) {
            properties.add(PUBLISHER_DEFAULT_NAME)
        }
        pageMargins = PageMargins(setting.value.toFloat())
        publisherSettings = PublisherDefault.Off.toString()
        updateProperties(setting)
    }

    fun currentWordSpacing(): UserSetting {
        return UserSetting(WORD_SPACING_REF, WORD_SPACING_NAME, wordSpacing.value.toString())
    }

    fun updateWordSpacing(setting: UserSetting) {
        if (!properties.contains(PUBLISHER_DEFAULT_NAME)) {
            properties.add(PUBLISHER_DEFAULT_NAME)
        }
        wordSpacing = WordSpacing(setting.value.toFloat())
        publisherSettings = PublisherDefault.Off.toString()
        updateProperties(setting)
    }

    fun currentLetterSpacing(): UserSetting {
        return UserSetting(LETTER_SPACING_REF, LETTER_SPACING_NAME, letterSpacing.value.toString())
    }

    fun updateLetterSpacing(setting: UserSetting) {
        if (!properties.contains(PUBLISHER_DEFAULT_NAME)) {
            properties.add(PUBLISHER_DEFAULT_NAME)
        }
        letterSpacing = LetterSpacing(setting.value.toFloat())
        publisherSettings = PublisherDefault.Off.toString()
        updateProperties(setting)
    }

    fun currentPublisherDefault(): UserSetting {
        return UserSetting(PUBLISHER_DEFAULT_REF, PUBLISHER_DEFAULT_NAME, publisherSettings)
    }

    fun updatePublisherDefault(setting: UserSetting) {
        publisherSettings = setting.value
        updateProperties(setting)
    }

    private fun updateProperties(setting: UserSetting) {
        if (!properties.contains(setting.name)) {
            properties.add(setting.name)
        }
        preferences.edit().putString(setting.ref, setting.value).apply()
        Timber.d(TAG, "settings - ${properties.toString()}")
        updateViewCSS(properties)
    }

    fun updateViewCSS(properties: List<String>) {
        val webView = resourcePager.getFocusedChild().findViewById(R.id.webView) as R2WebView
        applyCSS(webView, properties)
    }

    fun updateViewsCSS(properties: List<String>) {
        for (i in 0 until resourcePager.childCount) {
            val webView = resourcePager.getChildAt(i).findViewById(R.id.webView) as R2WebView
            applyCSS(webView, properties)
        }
    }

    fun applyCSS(view: R2WebView, properties: List<String>) {
        for (property in (getUserSettings().filter { properties.contains(it.name) })) {
            view.setProperty(property.name, property.value)
        }
    }

    fun applyAllCSS(view: R2WebView) {
        for (property in (getUserSettings())) {
            view.setProperty(property.name, property.value)
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

        val fontSpinner: Spinner = layout.findViewById(R.id.spinner_action_settings_intervall_values) as Spinner

        var selectedFontIndex = 0

        val fonts = context.getResources().getStringArray(R.array.font_list)

//        val fonts = mutableListOf<String>()
//        enumValues<FontFamily>().forEach {
//            fonts.add(it.toString())
//        }

        val dataAdapter = object : ArrayAdapter<String>(context, R.layout.spinner_item, fonts) {

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                var v: View? = null
                v = super.getDropDownView(position, null, parent)
                // If this is the selected item position
                if (position == selectedFontIndex) {
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

        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fontSpinner.setAdapter(dataAdapter)


        val fontFamily = currentFontFamily()
        when (fontFamily.value) {
            FontFamily.Publisher.toString() -> {
                selectedFontIndex = 0
            }
            FontFamily.Helvetica.toString() -> {
                selectedFontIndex = 1
            }
            FontFamily.Iowan.toString() -> {
                selectedFontIndex = 2
            }
            FontFamily.Athelas.toString() -> {
                selectedFontIndex = 3
            }
            FontFamily.Seravek.toString() -> {
                selectedFontIndex = 4
            }

        }
        fontSpinner.setSelection(selectedFontIndex)

        fontSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
                selectedFontIndex = pos

                when (pos) {
                    0 -> {
                        fontFamily.value = FontFamily.Publisher.toString()
                    }
                    1 -> {
                        fontFamily.value = FontFamily.Helvetica.toString()
                    }
                    2 -> {
                        fontFamily.value = FontFamily.Iowan.toString()
                    }
                    3 -> {
                        fontFamily.value = FontFamily.Athelas.toString()
                    }
                    4 -> {
                        fontFamily.value = FontFamily.Seravek.toString()
                    }
                }
                updateFontFamily(fontFamily)
            }

            override fun onNothingSelected(parent: AdapterView<out Adapter>?) {

            }

        }


        val appearanceGroup = layout.findViewById(R.id.appearance) as RadioGroup
        val appearanceDefaultButton = layout.findViewById(R.id.appearance_default) as RadioButton
        val appearanceSepiaButton = layout.findViewById(R.id.appearance_sepia) as RadioButton
        val appearanceNightButton = layout.findViewById(R.id.appearance_night) as RadioButton

        val appearance = currentAppearance()
        when (appearance.value) {
            Appearance.Default.toString() -> {
                appearanceDefaultButton.isChecked = true
            }
            Appearance.Sepia.toString() -> {
                appearanceSepiaButton.isChecked = true
            }
            Appearance.Night.toString() -> {
                appearanceNightButton.isChecked = true
            }
        }

        appearanceGroup.setOnCheckedChangeListener { radioGroup, i ->
            when (i) {
                R.id.appearance_default -> {
                    appearance.value = Appearance.Default.toString()
                    resourcePager.setBackgroundColor(Color.parseColor("#ffffff"))
                    (resourcePager.focusedChild.findViewById(R.id.book_title) as TextView).setTextColor(Color.parseColor("#000000"))
                }
                R.id.appearance_sepia -> {
                    appearance.value = Appearance.Sepia.toString()
                    resourcePager.setBackgroundColor(Color.parseColor("#faf4e8"))
                    (resourcePager.focusedChild.findViewById(R.id.book_title) as TextView).setTextColor(Color.parseColor("#000000"))
                }
                R.id.appearance_night -> {
                    appearance.value = Appearance.Night.toString()
                    resourcePager.setBackgroundColor(Color.parseColor("#000000"))
                    (resourcePager.focusedChild.findViewById(R.id.book_title) as TextView).setTextColor(Color.parseColor("#ffffff"))
                }
            }
            updateAppearance(appearance)
        }

        val fontSize = currentFontSize()
        val fs = FontSize(fontSize.value.toInt())

        val fontDecreaseButton = layout.findViewById(R.id.font_decrease) as ImageButton
        val fontIncreaseButton = layout.findViewById(R.id.font_increase) as ImageButton

        fontDecreaseButton.setOnClickListener {
            fs.decrement()
            fontSize.value = fs.value.toString()
            updateFontSize(fontSize)
        }

        fontIncreaseButton.setOnClickListener {
            fs.increment()
            fontSize.value = fs.value.toString()
            updateFontSize(fontSize)
        }


        val publisherDefaultSwitch = layout.findViewById(R.id.publisher_default) as Switch
        val publisherDefault = currentPublisherDefault()
        when (publisherDefault.value) {
            PublisherDefault.On.toString() -> {
                publisherDefaultSwitch.isChecked = true
            }
            PublisherDefault.Off.toString() -> {
                publisherDefaultSwitch.isChecked = false
            }
        }

        publisherDefaultSwitch.setOnCheckedChangeListener { compoundButton, b ->
            when (b) {
                true -> {
                    publisherDefault.value = PublisherDefault.On.toString()
                }
                false -> {
                    publisherDefault.value = PublisherDefault.Off.toString()
                }
            }
            updatePublisherDefault(publisherDefault)
        }


        val scrollModeSwitch = layout.findViewById(R.id.scroll_mode) as Switch
        val scrollMode = currentScrollMode()

        when (scrollMode.value) {
            Scroll.On.toString() -> {
                scrollModeSwitch.isChecked = true
            }
            Scroll.Off.toString() -> {
                scrollModeSwitch.isChecked = false
            }
        }

        scrollModeSwitch.setOnCheckedChangeListener { compoundButton, b ->
            when (b) {
                true -> {
                    scrollMode.value = Scroll.On.toString()
                    (resourcePager.focusedChild.findViewById(R.id.book_title) as TextView).visibility = View.GONE
                    resourcePager.focusedChild.setPadding(0, 5, 0, 5)
                }
                false -> {
                    scrollMode.value = Scroll.Off.toString()
                    (resourcePager.focusedChild.findViewById(R.id.book_title) as TextView).visibility = View.VISIBLE
                    resourcePager.focusedChild.setPadding(0, 30, 0, 30)
                }
            }
            updateScrollMode(scrollMode)
        }

        val alignmentGroup = layout.findViewById(R.id.TextAlignment) as RadioGroup
        val alignmentLeftButton = layout.findViewById(R.id.alignment_left) as RadioButton
        val alignmentJustifyButto = layout.findViewById(R.id.alignment_justify) as RadioButton

        val alignment = currentTextAlignment()
        when (alignment.value) {
            TextAlignment.Justify.toString() -> {
                alignmentJustifyButto.isChecked = true
                alignmentJustifyButto.setCompoundDrawablesWithIntrinsicBounds(null, context.getDrawable(R.drawable.icon_justify_white), null, null)
                alignmentLeftButton.setCompoundDrawablesWithIntrinsicBounds(null, context.getDrawable(R.drawable.icon_left), null, null)
            }
            TextAlignment.Left.toString() -> {
                alignmentLeftButton.isChecked = true
                alignmentJustifyButto.setCompoundDrawablesWithIntrinsicBounds(null, context.getDrawable(R.drawable.icon_justify), null, null)
                alignmentLeftButton.setCompoundDrawablesWithIntrinsicBounds(null, context.getDrawable(R.drawable.icon_left_white), null, null)
            }
        }

        alignmentGroup.setOnCheckedChangeListener { radioGroup, i ->
            when (i) {
                R.id.alignment_justify -> {
                    alignmentJustifyButto.setCompoundDrawablesWithIntrinsicBounds(null, context.getDrawable(R.drawable.icon_justify_white), null, null)
                    alignmentLeftButton.setCompoundDrawablesWithIntrinsicBounds(null, context.getDrawable(R.drawable.icon_left), null, null)

                    alignment.value = TextAlignment.Justify.toString()
                }
                R.id.alignment_left -> {
                    alignmentJustifyButto.setCompoundDrawablesWithIntrinsicBounds(null, context.getDrawable(R.drawable.icon_justify), null, null)
                    alignmentLeftButton.setCompoundDrawablesWithIntrinsicBounds(null, context.getDrawable(R.drawable.icon_left_white), null, null)

                    alignment.value = TextAlignment.Left.toString()
                }
            }
            updateTextAlignment(alignment)
            publisherDefaultSwitch.isChecked = false
        }

        val columnsCountGroup = layout.findViewById(R.id.columns) as RadioGroup
        val columnsCountAutoButton = layout.findViewById(R.id.column_auto) as RadioButton
        val columnsCountOneButton = layout.findViewById(R.id.column_one) as RadioButton
        val columnsCountTwoButton = layout.findViewById(R.id.column_two) as RadioButton

        val columnsCount = currentColumnCount()
        when (columnsCount.value) {
            ColumnCount.Auto.toString() -> {
                columnsCountAutoButton.isChecked = true
            }
            ColumnCount.One.toString() -> {
                columnsCountOneButton.isChecked = true
            }
            ColumnCount.Two.toString() -> {
                columnsCountTwoButton.isChecked = true
            }
        }
        columnsCountGroup.setOnCheckedChangeListener { radioGroup, i ->
            when (i) {
                R.id.column_auto -> {
                    columnsCount.value = ColumnCount.Auto.toString()
                }
                R.id.column_one -> {
                    columnsCount.value = ColumnCount.One.toString()
                }
                R.id.column_two -> {
                    columnsCount.value = ColumnCount.Two.toString()
                }
            }
            updateColumnCount(columnsCount)
            publisherDefaultSwitch.isChecked = false
        }

        val pageMargins = currentPageMargins()
        val pm = PageMargins(pageMargins.value.toFloat())
        val pageMarginsDecreaseButton = layout.findViewById(R.id.pm_decrease) as ImageButton
        val pageMarginsIncreaseButton = layout.findViewById(R.id.pm_increase) as ImageButton
        val pageMarginsDisplay = layout.findViewById(R.id.pm_display) as TextView
        pageMarginsDisplay.text = pageMargins.value

        pageMarginsDecreaseButton.setOnClickListener {
            pm.decrement()
            pageMargins.value = pm.value.toString()
            pageMarginsDisplay.text = pageMargins.value
            updatePageMargins(pageMargins)
            publisherDefaultSwitch.isChecked = false
        }

        pageMarginsIncreaseButton.setOnClickListener {
            pm.increment()
            pageMargins.value = pm.value.toString()
            pageMarginsDisplay.text = pageMargins.value
            updatePageMargins(pageMargins)
            publisherDefaultSwitch.isChecked = false
        }

        val wordSpacing = currentWordSpacing()
        val ws = WordSpacing(wordSpacing.value.toFloat())
        val wordSpacingDecreaseButton = layout.findViewById(R.id.ws_decrease) as ImageButton
        val wordSpacingIncreaseButton = layout.findViewById(R.id.ws_increase) as ImageButton
        val wordSpacingDisplay = layout.findViewById(R.id.ws_display) as TextView

        if (ws.value == WordSpacing.min) {
            wordSpacingDisplay.text = "auto"
        } else {
            wordSpacingDisplay.text = wordSpacing.value
        }

        wordSpacingDecreaseButton.setOnClickListener {
            ws.decrement()
            wordSpacing.value = ws.value.toString()
            if (ws.value == WordSpacing.min) {
                wordSpacingDisplay.text = "auto"
            } else {
                wordSpacingDisplay.text = wordSpacing.value
            }
            updateWordSpacing(wordSpacing)
            publisherDefaultSwitch.isChecked = false
        }

        wordSpacingIncreaseButton.setOnClickListener {
            ws.increment()
            wordSpacing.value = ws.value.toString()
            wordSpacingDisplay.text = wordSpacing.value
            updateWordSpacing(wordSpacing)
            publisherDefaultSwitch.isChecked = false
        }

        val letterSpacing = currentLetterSpacing()
        val ls = LetterSpacing(letterSpacing.value.toFloat())
        val letterSpacingDecreaseButton = layout.findViewById(R.id.ls_decrease) as ImageButton
        val letterSpacingIncreaseButton = layout.findViewById(R.id.ls_increase) as ImageButton
        val letterSpacingDisplay = layout.findViewById(R.id.ls_display) as TextView


        if (ls.value == LetterSpacing.min) {
            letterSpacingDisplay.text = "auto"
        } else {
            letterSpacingDisplay.text = letterSpacing.value
        }

        letterSpacingDecreaseButton.setOnClickListener {
            ls.decrement()
            letterSpacing.value = ls.value.toString()
            if (ls.value == LetterSpacing.min) {
                letterSpacingDisplay.text = "auto"
            } else {
                letterSpacingDisplay.text = letterSpacing.value
            }
            updateLetterSpacing(letterSpacing)
            publisherDefaultSwitch.isChecked = false
        }

        letterSpacingIncreaseButton.setOnClickListener {
            ls.increment()
            letterSpacing.value = ls.value.toString()
            letterSpacingDisplay.text = letterSpacing.value
            updateLetterSpacing(letterSpacing)
            publisherDefaultSwitch.isChecked = false
        }


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
