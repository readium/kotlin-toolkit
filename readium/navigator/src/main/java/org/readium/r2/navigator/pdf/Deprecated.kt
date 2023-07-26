/*
 * Module: r2-navigator-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.pdf

import androidx.appcompat.app.AppCompatActivity

@Deprecated("Use `PdfNavigatorFragment` in your own activity instead", level = DeprecationLevel.ERROR)
public abstract class R2PdfActivity : AppCompatActivity()

// This is for lint to pass.
private val fake = null
