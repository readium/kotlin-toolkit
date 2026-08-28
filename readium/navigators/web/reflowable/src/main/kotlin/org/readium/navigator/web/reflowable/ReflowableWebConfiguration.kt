/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.reflowable

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.readium.navigator.web.common.FontFamilyDeclarations
import org.readium.navigator.web.common.JavascriptInterfaceFactory
import org.readium.navigator.web.common.WebDecorationTemplates
import org.readium.navigator.web.reflowable.preferences.ReflowableWebDefaults
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
public data class ReflowableWebConfiguration(
    /**
     * Fallbacks for some preferences
     */
    val defaults: ReflowableWebDefaults = ReflowableWebDefaults(),

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
    val fontFamilyDeclarations: FontFamilyDeclarations = FontFamilyDeclarations {},

    /**
     * JavaScript interfaces to add to the web view of every resource, keyed by the name they
     * are exposed under in the `window` object.
     *
     * Return `null` in a factory to skip adding its interface for a given resource.
     */
    val javascriptInterfaces: ImmutableMap<String, JavascriptInterfaceFactory> = persistentMapOf(),
)
