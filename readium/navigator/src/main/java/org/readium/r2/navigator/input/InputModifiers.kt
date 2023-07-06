package org.readium.r2.navigator.input

import android.view.KeyEvent

/**
 * Represents a set of modifiers for an input event.
 */
@JvmInline
value class InputModifiers(val value: Int) {

    companion object {
        val None = InputModifiers(0)
        val Alt = InputModifiers(1 shl 0)
        val Control = InputModifiers(1 shl 1)
        val Meta = InputModifiers(1 shl 2)
        val Shift = InputModifiers(1 shl 3)
    }

    fun contains(other: InputModifiers): Boolean =
        (value and other.value) == other.value

    operator fun plus(other: InputModifiers): InputModifiers =
        InputModifiers(value or other.value)
}
