package org.readium.r2.navigator

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.SwitchPreference
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

class R2ReaderSettingsFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    lateinit var change: Change
    val properties = mutableListOf<String>()
    lateinit var switch: SwitchPreference

    interface Change {
        fun onModeChange(mode: String)
        fun onScrollChange(scroll: String)
        fun onFontSizeChange(fontSize: String)
        fun onFontChange(font: String)
        fun onPublisherSettingsChange(value: Boolean)
        fun onTextAlignmentChange(value: String)
        fun onColumnCountChange(value: String)
        fun onWordSpacingChange(value: String)
        fun onLetterSpacingChange(value: String)
        fun onPageMarginsChange(value: String)
        fun onFragBack(properties: List<String>)
        fun updateViewCSS(properties: List<String>)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == null)
            return
        if (!properties.contains(key)){
            properties.add("--USER__" + key)
        }
        when (key){
            "appearance" ->
                change.onModeChange(sharedPreferences?.getString(key, "readium-default-on") ?: "readium-default-on")
            "fontSize" ->
                change.onFontSizeChange(sharedPreferences?.getString(key, "100") ?: "100")
            "scroll" ->
                change.onScrollChange(sharedPreferences?.getString(key, "readium-scroll-off") ?: "readium-scroll-off")
            "fontFamily" -> {
                change.onFontChange(sharedPreferences?.getString(key, "Publisher\'s default") ?: "Publisher\'s default")
                properties.add("--USER__fontOverride")
            }
            "pageMargins" ->
                change.onPageMarginsChange(sharedPreferences?.getString(key, "1.0f") ?: "1.0f")
            "advancedSettings" ->
                change.onPublisherSettingsChange(sharedPreferences?.getBoolean(key, true) ?: true)
            "textAlign" -> {
                change.onTextAlignmentChange(sharedPreferences?.getString(
                        key, "justify") ?: "justify")
                sharedPreferences?.edit()?.putBoolean("advancedSettings", true)?.apply()
                switch.isChecked = true
            }
            "colCount" -> {
                change.onColumnCountChange(sharedPreferences?.getString(key, "auto") ?: "auto")
                sharedPreferences?.edit()?.putBoolean("advancedSettings", true)?.apply()
                switch.isChecked = true
            }
            "wordSpacing" -> {
                change.onWordSpacingChange(sharedPreferences?.getString(key, "1.0f") ?: "1.0f")
                sharedPreferences?.edit()?.putBoolean("advancedSettings", true)?.apply()
                switch.isChecked = true
            }
            "letterSpacing" -> {
                change.onLetterSpacingChange(sharedPreferences?.getString(key, "1.0f") ?: "1.0f")
                sharedPreferences?.edit()?.putBoolean("advancedSettings", true)?.apply()
                switch.isChecked = true
            }
        }
        Log.d("Settings", properties.toString())
        change.updateViewCSS(properties)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preference)
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        switch = findPreference("advancedSettings") as SwitchPreference
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view.setBackgroundColor(resources.getColor(android.R.color.white))
        return view
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        change = (activity as R2EpubActivity).cssOperator
    }

    override fun onPause() {
        super.onPause()
        change.onFragBack(properties)
        properties.clear()
    }

}
