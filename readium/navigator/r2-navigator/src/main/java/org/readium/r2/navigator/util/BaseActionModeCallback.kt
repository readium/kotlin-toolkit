/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.util

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem

/**
 * A convenient base implementation of [ActionMode.Callback], when you don't need to override all
 * methods.
 */
abstract class BaseActionModeCallback : ActionMode.Callback {
    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false
    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = false
    override fun onDestroyActionMode(mode: ActionMode) {}
}
