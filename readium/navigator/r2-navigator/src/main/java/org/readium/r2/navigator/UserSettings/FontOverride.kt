package org.readium.r2.navigator.UserSettings

enum class FontOverride(val value: String) : CharSequence by value {

    On("readium-font-on"),
    Off("readium-font-off");

    override fun toString() = value
}