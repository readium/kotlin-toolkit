package org.readium.r2.navigator.UserSettings

enum class Appearance(val value: String) : CharSequence by value {

    Default("readium-default-on"),
    Sepia("readium-sepia-on"),
    Night("readium-night-on");

    override fun toString() = value

}
