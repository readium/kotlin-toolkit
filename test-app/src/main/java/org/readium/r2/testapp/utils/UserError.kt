/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.utils

import android.app.Activity
import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import java.text.DateFormat
import java.util.Date
import org.joda.time.DateTime
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.toDebugDescription
import org.readium.r2.testapp.R
import org.readium.r2.testapp.utils.extensions.createShareIntent
import timber.log.Timber

/**
 * An error that can be presented to the user using a localized message.
 */
class UserError private constructor(
    val content: Content,
    val cause: Error?,
) {

    constructor(@StringRes userMessageId: Int, vararg args: Any?, cause: Error?) :
        this(Content(userMessageId, *args), cause)

    constructor(
        @PluralsRes userMessageId: Int,
        quantity: Int?,
        vararg args: Any?,
        cause: Error?,
    ) :
        this(Content(userMessageId, quantity, *args), cause)

    constructor(message: String, cause: Error?) :
        this(Content(message), cause)

    /**
     * Gets the localized user-facing message for this exception.
     */
    fun getUserMessage(context: Context): String =
        content.getUserMessage(context)

    /**
     * Presents the error in the given activity.
     */
    fun show(activity: Activity) {
        val message = getUserMessage(activity)
        val snackbar = Snackbar.make(
            activity.findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_LONG
        )

        var details = "UserError: $message"
        cause?.toDebugDescription()?.let {
            details += "\n$it"
            snackbar.setAction(R.string.details) {
                AlertDialog.Builder(activity)
                    .setTitle(R.string.details)
                    .setMessage(details)
                    .setPositiveButton(R.string.share) { _, _ ->
                        activity.startActivity(createShareIntent(activity, details))
                    }
                    .setNegativeButton(R.string.close, null)
                    .show()
            }
        }
        Timber.e(details)
        snackbar.show()
    }

    /**
     * Provides a way to generate a localized user message.
     */
    sealed class Content {

        abstract fun getUserMessage(context: Context): String

        /**
         * Holds the parts of a localized string message.
         *
         * @param userMessageId String resource id of the localized user message.
         * @param args Optional arguments to expand in the message.
         * @param quantity Quantity to use if the user message is a quantity strings.
         */
        class LocalizedString(
            private val userMessageId: Int,
            private val args: Array<out Any?>,
            private val quantity: Int?,
        ) : Content() {
            override fun getUserMessage(context: Context): String {
                // Convert complex objects to strings, such as Date, to be interpolated.
                val args = args.map { arg ->
                    when (arg) {
                        is Date -> DateFormat.getDateInstance().format(arg)
                        is DateTime -> DateFormat.getDateInstance().format(arg.toDate())
                        else -> arg
                    }
                }

                val message =
                    if (quantity != null) {
                        context.resources.getQuantityString(
                            userMessageId,
                            quantity,
                            *(args.toTypedArray())
                        )
                    } else {
                        context.getString(userMessageId, *(args.toTypedArray()))
                    }

                return message
            }
        }

        /**
         * Holds an already localized string message. For example, received from an HTTP
         * Problem Details object.
         */
        class Message(private val message: String) : Content() {
            override fun getUserMessage(context: Context): String = message
        }

        companion object {
            operator fun invoke(@StringRes userMessageId: Int, vararg args: Any?): Content =
                LocalizedString(userMessageId, args, null)
            operator fun invoke(
                @PluralsRes userMessageId: Int,
                quantity: Int?,
                vararg args: Any?,
            ): Content =
                LocalizedString(userMessageId, args, quantity)

            operator fun invoke(message: String): Content =
                Message(message)
        }
    }
}
