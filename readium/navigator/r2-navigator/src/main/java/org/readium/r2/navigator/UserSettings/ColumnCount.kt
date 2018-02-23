package org.readium.r2.navigator.UserSettings

enum class ColumnCount(val value: String)  : CharSequence by value{

    Auto("auto"),
    One("1"),
    Two("2");

    override fun toString() = value

}