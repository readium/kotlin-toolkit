/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import org.readium.r2.shared.publication.Locator

interface IR2Selectable {
    fun currentSelection(callback: (Locator?) -> Unit)
}
