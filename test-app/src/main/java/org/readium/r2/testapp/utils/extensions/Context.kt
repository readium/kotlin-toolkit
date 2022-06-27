/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp.utils.extensions

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import org.readium.r2.testapp.R
import kotlin.coroutines.resume


/**
 * Extensions
 */

@ColorInt
fun Context.color(@ColorRes id: Int): Int {
    return ContextCompat.getColor(this, id)
}

suspend fun Context.confirmDialog(
    message: String,
    @StringRes positiveButton: Int = R.string.ok,
    @StringRes negativeButton: Int = R.string.cancel
): Boolean =
    suspendCancellableCoroutine { cont ->
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(getString(positiveButton)) { dialog, _ ->
                dialog.dismiss()
                cont.resume(true)
            }
            .setNegativeButton(getString(negativeButton)) { dialog, _ ->
                dialog.cancel()
            }
            .setOnCancelListener {
                cont.resume(false)
            }
            .show()
    }
