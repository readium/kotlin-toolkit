package org.readium.r2.navigator.UserSettings

enum class TextAlignment(val value : String) : CharSequence by value {

    Justify("justify"),
    Left("start");

    override fun toString() = value

}