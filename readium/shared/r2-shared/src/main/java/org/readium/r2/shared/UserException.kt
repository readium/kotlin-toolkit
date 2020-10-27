/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import org.joda.time.DateTime
import java.text.DateFormat
import java.util.*

/**
 * An exception that can be presented to the user using a localized message.
 *
 * @param userMessageId String resource id of the localized user message.
 * @param args Optional arguments to expand in the message.
 * @param quantity Quantity to use if the user message is a quantity strings.
 */
open class UserException protected constructor(
    private val userMessageId: Int,
    private val args: Array<out Any>,
    private val quantity: Int?,
    cause: Throwable?
) : Exception(cause) {

    constructor(@StringRes userMessageId: Int, vararg args: Any, cause: Throwable? = null)
        : this(userMessageId, args, null, cause)

    constructor(@PluralsRes userMessageId: Int, quantity: Int, vararg args: Any, cause: Throwable? = null)
        : this(userMessageId, args, quantity, cause)

    /**
     * Gets the localized user-facing message for this exception.
     *
     * @param includesCauses Includes nested [UserException] causes in the user message when true.
     */
    open fun getUserMessage(context: Context, includesCauses: Boolean = true): String {
        // Convert complex objects to strings, such as Date, to be interpolated.
        val args = args.map { arg ->
            when (arg) {
                is Date -> DateFormat.getDateInstance().format(arg)
                is DateTime -> DateFormat.getDateInstance().format(arg.toDate())
                else -> arg
            }
        }

        var message =
            if (quantity != null) context.resources.getQuantityString(userMessageId, quantity, *(args.toTypedArray()))
            else context.getString(userMessageId, *(args.toTypedArray()))

        // Includes nested causes if they are also [UserException].
        val cause = cause
        if (cause is UserException && includesCauses) {
            message += ": ${cause.getUserMessage(context)}"
        }

        return message
    }

}
