package org.readium.r2.navigator.UserSettings

enum class Scroll(val value: String) : CharSequence by value {

    Off("readium-scroll-off"),
    On("readium-scroll-on");

    override fun toString() = value
}