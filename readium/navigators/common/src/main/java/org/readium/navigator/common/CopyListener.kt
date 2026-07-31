/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.common

import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * This listener notifies you when the user attempted to copy content but the publication's
 * Content Protection forbade it (e.g. copy allowance exhausted).
 *
 * It is called only for copies intercepted by the navigator (e.g. from the system selection menu
 * or a keyboard shortcut). Programmatic [SelectionController.copySelection] calls report errors
 * through their return value instead.
 */
@ExperimentalReadiumApi
public fun interface CopyListener {

    public fun onCopyForbidden()
}

@ExperimentalReadiumApi
public class NullCopyListener : CopyListener {

    override fun onCopyForbidden() {
    }
}
