/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared

import android.content.Context
import androidx.annotation.StringRes

/**
 * An exception that can be presented to the user using a localized message.
 *
 * @param userMessageId String resource id of the localized user message.
 * @param args Optional arguments to expand in the message.
 */
open class UserException(
    @StringRes private val userMessageId: Int,
    private val args: Array<out Any> = emptyArray(),
    cause: Throwable? = null
) : Exception(cause) {

    /**
     * Gets the localized user-facing message for this exception.
     *
     * @param includesCauses Includes nested [UserException] causes in the user message when true.
     */
    open fun getUserMessage(context: Context, includesCauses: Boolean = true): String {
        var message = context.getString(userMessageId, *args)

        // Includes nested causes if they are also [UserException].
        val cause = cause
        if (cause is UserException && includesCauses) {
            message += ": ${cause.getUserMessage(context)}"
        }

        return message
    }

}
