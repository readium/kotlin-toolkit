/*
 * Copyright 2024 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.reflowable.decoration

import org.readium.r2.shared.util.Url

public data class ReflowableWebDecorationLocation(
    val href: Url,
    val target: DecorationTarget,
)

public sealed class DecorationTarget

public data class TextDecorationTarget(
    val targetedText: String,
    val textBefore: String?,
    val textAfter: String?,
    val cssSelector: String?,
)

public data class ElementDecorationTarget(
    val cssSelector: String,
)
