package org.readium.r2.navigator.UserSettings

enum class FontFamily(val value: String) : CharSequence by value {

    Publisher("Publisher's default"),
    Helvetica("sans-serif"),
    Iowan("Roboto"),
    Athelas("serif"),
    Seravek("Seravek");

    override fun toString() = value

}
