package org.readium.r2.navigator.UserSettings

import android.content.SharedPreferences
import android.util.Log
import org.readium.r2.navigator.R

private const val fontSizeKey = "--USER__fontSize"
private const val fontKey = "--USER__fontFamily"
private const val fontOverrideKey = "--USER__fontOverride"
private const val appearanceKey = "--USER__appearance"
private const val scrollKey = "--USER__scroll"
private const val publisherSettingsKey = "--USER__advancedSettings"
private const val textAlignmentKey = "--USER__textAlign"
private const val columnCountKey = "--USER__colCount"
private const val wordSpacingKey = "--USER__wordSpacing"
private const val letterSpacingKey = "--USER__letterSpacing"
private const val pageMarginsKey = "--USER__pageMargins"

class UserSettings(preferences: SharedPreferences) {

    data class CssProperty(val key: String, val value: String)

    var fontSize = "100"
    var fontOverride = false
    var font = "Publisher's default"
    var appearance = "readium-default-on"
    var verticalScroll = "readium-scroll-off"

    //Advanced settings
    var advancedSettings = false
    var textAlignment = "justify"
    var columnCount = "auto"
    var wordSpacing = 0.0f
    var letterSpacing = 0.0f
    var pageMargins = 1.0f

    init {
        fontSize = preferences.getString("fontSize", "100")
        appearance = preferences.getString("appearance", "readium-default-on")
        verticalScroll = preferences.getString("scroll", "readium-scroll-off")
        font = preferences.getString("fontFamily", font)
        if (font != "Publisher's default"){
            fontOverride = true
        }
        advancedSettings = preferences.getBoolean("advancedSettings", true)
        textAlignment = preferences.getString("textAlignment", "justify")
        columnCount = preferences.getString("columnCount", "auto")
        wordSpacing = preferences.getString("wordSpacing", "0.0f").toFloat()
        letterSpacing = preferences.getString("letterSpacing", "0.0f").toFloat()
        pageMargins = preferences.getString("pageMargins", "1.0f").toFloat()
        Log.d("", "")
    }

//    var isVerticalScrollEnabled:Boolean = false
//        get() { return verticalScroll == "readium-scroll-on"}

    fun getProperties() : List<CssProperty> {
        val properties = mutableListOf<CssProperty>()

        //Font size
        properties.add(CssProperty(fontSizeKey, fontSize + "%"))
        //Font
        if (fontOverride == false){
            properties.add(CssProperty(fontOverrideKey, "readium-font-off"))
        } else {
            properties.add(CssProperty(fontOverrideKey, "readium-font-on"))
            properties.add(CssProperty(fontKey, font))
        }
        //Appearance (default, sepia, night)
        properties.add(CssProperty(appearanceKey, appearance))
        //Scroll
        properties.add(CssProperty(scrollKey, verticalScroll))
        //Advanced settings
        if (advancedSettings == false){
            properties.add(CssProperty(publisherSettingsKey, "readium-advanced-off"))
        } else {
            properties.add(CssProperty(publisherSettingsKey, "readium-advanced-on"))
        }
        properties.add(CssProperty(textAlignmentKey, textAlignment))
        properties.add(CssProperty(columnCountKey, columnCount))
        properties.add(CssProperty(wordSpacingKey, wordSpacing.toString() + "rem"))
        properties.add(CssProperty(letterSpacingKey, letterSpacing.toString() + "rem"))
        properties.add(CssProperty(pageMarginsKey, pageMargins.toString()))
        return properties
    }

}