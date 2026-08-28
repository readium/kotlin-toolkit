/*
 * Copyright 2025 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.internals.webapi

public fun String.toJavaScriptLiteral(): String {
    val content = replace("\\", "\\\\").replace("'", "\\'")
    return "'$content'"
}
