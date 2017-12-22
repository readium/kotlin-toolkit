package org.readium.r2.navigator.UserSettings

enum class Appearance(name: String) {

    Default("readium-default-on"),
    Sepia("readium-sepia-on"),
    Night("readium-night-on");

    override fun toString() = when (this){
            Default -> "readium-default-on"
            Sepia -> "readium-sepia-on"
            Night-> "readium-night-on"
        }
}