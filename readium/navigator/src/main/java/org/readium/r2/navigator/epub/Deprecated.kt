/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import org.readium.r2.shared.publication.Locator

@Deprecated(
    "Use EpubNavigatorFragment in your own activity instead.",
    level = DeprecationLevel.ERROR
)
public open class R2EpubActivity

@Deprecated("Use Decorator API instead.", level = DeprecationLevel.ERROR)
public interface IR2Highlightable

@Deprecated("Use Decorator API instead.", level = DeprecationLevel.ERROR)
public data class Highlight(
    val id: String
)

@Deprecated("Use Decorator API instead.", level = DeprecationLevel.ERROR)
public enum class Style {
    Highlight, Underline, Strikethrough
}

@Deprecated("Use navigator fragments.", level = DeprecationLevel.ERROR)
public interface IR2Selectable {
    public fun currentSelection(callback: (Locator?) -> Unit)
}
