/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.common

import kotlin.reflect.KClass
import kotlinx.collections.immutable.ImmutableList
import org.readium.r2.navigator.Decoration

public interface DecorationController {

    /**
     * Declares the current state of the decorations for each group.
     *
     * The Navigator will decide when to actually render each decoration efficiently. Your only
     * responsibility is to submit the updated list of decorations when there are changes.
     * Name each decoration group as you see fit. A good practice is to use the name of the feature
     * requiring decorations, e.g. annotation, search, tts, etc.
     */
    public val decorations: MutableMap<String, ImmutableList<Decoration>>

    /**
     * Indicates whether the Navigator supports the given decoration [style] class.
     *
     * You should check whether the Navigator supports drawing the decoration styles required by a
     * particular feature before enabling it. For example, underlining an audiobook does not make
     * sense, so an Audiobook Navigator would not support the `underline` decoration style.
     */
    public fun <T : Decoration.Style> supportsDecorationStyle(style: KClass<T>): Boolean
}
