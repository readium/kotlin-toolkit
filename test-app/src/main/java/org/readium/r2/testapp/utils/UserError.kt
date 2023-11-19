/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.utils

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import java.text.DateFormat
import java.util.Date
import org.joda.time.DateTime

/**
 * An exception that can be presented to the user using a localized message.
 */
interface UserError {

    val content: Content

    val cause: UserError?

    /**
     * Provides a way to generate a localized user message.
     */
    sealed class Content {

        abstract fun getUserMessage(
            context: Context,
            cause: UserError? = null,
            includesCauses: Boolean = true
        ): String

        /**
         * Holds a nested [UserError].
         */
        class Error(val error: UserError) : Content() {
            override fun getUserMessage(
                context: Context,
                cause: UserError?,
                includesCauses: Boolean
            ): String =
                error.getUserMessage(context, includesCauses)
        }

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
            private val quantity: Int?
        ) : Content() {
            override fun getUserMessage(
                context: Context,
                cause: UserError?,
                includesCauses: Boolean
            ): String {
                // Convert complex objects to strings, such as Date, to be interpolated.
                val args = args.map { arg ->
                    when (arg) {
                        is Date -> DateFormat.getDateInstance().format(arg)
                        is DateTime -> DateFormat.getDateInstance().format(arg.toDate())
                        else -> arg
                    }
                }

                var message =
                    if (quantity != null) {
                        context.resources.getQuantityString(
                            userMessageId,
                            quantity,
                            *(args.toTypedArray())
                        )
                    } else {
                        context.getString(userMessageId, *(args.toTypedArray()))
                    }

                if (cause != null && includesCauses) {
                    message += ": ${cause.getUserMessage(context, true)}"
                }

                return message
            }
        }

        /**
         * Holds an already localized string message. For example, received from an HTTP
         * Problem Details object.
         */
        class Message(private val message: String) : Content() {
            override fun getUserMessage(
                context: Context,
                cause: UserError?,
                includesCauses: Boolean
            ): String = message
        }

        companion object {
            operator fun invoke(@StringRes userMessageId: Int, vararg args: Any?): Content =
                LocalizedString(userMessageId, args, null)
            operator fun invoke(
                @PluralsRes userMessageId: Int,
                quantity: Int?,
                vararg args: Any?
            ): Content =
                LocalizedString(userMessageId, args, quantity)
            operator fun invoke(cause: UserError): Content =
                Error(cause)
            operator fun invoke(message: String): Content =
                Message(message)
        }
    }
}

/**
 * Gets the localized user-facing message for this exception.
 *
 * @param includesCauses Includes nested [UserError] causes in the user message when true.
 */
fun UserError.getUserMessage(context: Context, includesCauses: Boolean = true): String =
    content.getUserMessage(context, cause, includesCauses)
