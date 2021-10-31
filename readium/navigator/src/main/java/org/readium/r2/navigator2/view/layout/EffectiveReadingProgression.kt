package org.readium.r2.navigator2.view.layout

enum class EffectiveReadingProgression(val isHorizontal: Boolean) {
    /** Right to left */
    RTL(true),

    /** Left to right */
    LTR(true),

    /** Top to bottom */
    TTB(false),

    /** Bottom to top */
    BTT(false);

}