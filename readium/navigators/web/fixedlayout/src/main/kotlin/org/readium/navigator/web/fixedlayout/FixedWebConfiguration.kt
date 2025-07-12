/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.fixedlayout

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.readium.navigator.web.common.WebDecorationTemplates
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
public data class FixedWebConfiguration(
    /**
     * Patterns for asset paths which will be available to EPUB resources under
     * https://readium/assets/.
     *
     * The patterns can use simple glob wildcards, see:
     * https://developer.android.com/reference/android/os/PatternMatcher#PATTERN_SIMPLE_GLOB
     *
     * Use .* to serve all app assets.
     */
    val servedAssets: ImmutableList<String> = persistentListOf(),

    /**
     * Supported decoration templates.
     */
    val decorationTemplates: WebDecorationTemplates = WebDecorationTemplates.defaultTemplates(),
)
