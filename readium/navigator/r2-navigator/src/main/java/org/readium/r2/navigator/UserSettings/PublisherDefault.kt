package org.readium.r2.navigator.UserSettings

enum class PublisherDefault(val value: String) : CharSequence by value {

    On("readium-advanced-off"),
    Off("readium-advanced-on");

    override fun toString() = value
}