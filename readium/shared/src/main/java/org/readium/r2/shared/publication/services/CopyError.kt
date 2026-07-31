/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.services

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.Error

/**
 * Error returned when a navigator failed to copy the current selection to the clipboard.
 */
public sealed class CopyError(
    override val message: String,
) : Error {

    override val cause: Error? = null

    /**
     * The publication's Content Protection forbade copying the selected text, e.g. because the
     * copy allowance is exhausted.
     */
    public data object Forbidden :
        CopyError("The Content Protection forbade copying the selection.")

    /**
     * There is currently no selection to copy.
     */
    public data object NoSelection :
        CopyError("There is no selection to copy.")
}

/**
 * Consumes the given [text] with the copy right, and writes it to the clipboard if allowed.
 *
 * The whole operation runs as a single non-cancellable unit on the main thread: either it never
 * starts (nothing is consumed) or it fully completes (allowance spent and clipboard written),
 * regardless of the launching scope.
 *
 * @param clearOnDenial When true and the copy is forbidden, the clipboard is cleared. This is a
 * fail-safe for interception paths where the text might already have been written to the clipboard
 * by the system before the denial was known.
 *
 * @return Whether the user is allowed to copy the given text.
 */
@InternalReadiumApi
public suspend fun ContentProtectionService.UserRights.copyToClipboard(
    context: Context,
    text: String,
    clearOnDenial: Boolean,
): Boolean = withContext(Dispatchers.Main.immediate + NonCancellable) {
    val allowed = copy(text)

    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    if (allowed) {
        clipboard.setPrimaryClip(ClipData.newPlainText(null, text))
    } else if (clearOnDenial) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip()
        } else {
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }

    allowed
}
