package org.readium.r2.navigator

import org.readium.r2.navigator.UserSettings.UserSettings
import org.readium.r2.navigator.pager.R2ViewPager
import org.readium.r2.navigator.pager.R2WebView


class CSSOperator(val userSettings: UserSettings) : R2ReaderSettingsFragment.Change {

    lateinit var resourcePager: R2ViewPager

    override fun onModeChange(mode: String){
        userSettings.appearance = mode
    }

    override fun onFontSizeChange(fontSize: String) {
        userSettings.fontSize = (fontSize.toFloat()).toString()
    }

    override fun onScrollChange(scroll: String){
        userSettings.verticalScroll = scroll
    }

    override fun onFontChange(font: String){
        if (font == "Publisher's default") {
            userSettings.fontOverride = false
        } else {
            userSettings.fontOverride = true
            userSettings.font = font
        }
    }

    override fun onPublisherSettingsChange(value: Boolean){
        userSettings.advancedSettings= value
    }

    override fun onTextAlignmentChange(value: String){
        userSettings.textAlignment = value
    }

    override fun onColumnCountChange(value: String){
        userSettings.columnCount = value
    }

    override fun onWordSpacingChange(value: String) {
        userSettings.wordSpacing = value.toFloat()
    }

    override fun onLetterSpacingChange(value: String) {
        userSettings.letterSpacing = value.toFloat()
    }

    override fun onPageMarginsChange(value: String) {
        userSettings.pageMargins = value.toFloat()
    }

    override fun onFragBack(properties: List<String>) {
        updateViewsCSS(properties)
    }

    override fun updateViewCSS(properties: List<String>){
        val webView =  resourcePager.getFocusedChild() as R2WebView
        applyCSS(webView, properties)
    }

    fun updateViewsCSS(properties: List<String>){
        for (i in 0 until resourcePager.childCount) {
            val webView = resourcePager.getChildAt(i) as R2WebView
            applyCSS(webView, properties)
        }
    }

    fun applyCSS(view: R2WebView, properties: List<String>){
        for (property in (userSettings.getProperties().filter { properties.contains(it.key) })){
            view.setProperty(property.key, property.value)
        }
    }

    fun applyAllCSS(view: R2WebView){
        for (property in (userSettings.getProperties())){
            view.setProperty(property.key, property.value)
        }
    }
}
