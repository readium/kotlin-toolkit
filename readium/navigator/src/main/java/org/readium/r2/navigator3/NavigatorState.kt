package org.readium.r2.navigator3

import androidx.compose.runtime.mutableStateOf

class NavigatorState(
    readingProgression: ReadingProgression,
    overflow: Overflow
) {
    private val overflowState = mutableStateOf(overflow)

    private val readingProgressionState = mutableStateOf(readingProgression)

    var overflow: Overflow
        get() = overflowState.value
        set(value) {
            overflowState.value = value
        }

    var readingProgression: ReadingProgression
        get() = readingProgressionState.value
        set(value) {
            readingProgressionState.value = value
        }
}

enum class ReadingProgression(val value: String) {
    /** Right to left */
    RTL("rtl"),
    /** Left to right */
    LTR("ltr"),
    /** Top to bottom */
    TTB("ttb"),
    /** Bottom to top */
    BTT("btt");
}

enum class Fit(val value: String) {
    WIDTH("width"),
    HEIGHT("height"),
    CONTAIN("contain");
}

enum class Overflow(val value: String) {
    PAGINATED("paginated"),
    SCROLLED("scrolled");
}
