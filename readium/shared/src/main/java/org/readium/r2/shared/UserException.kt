/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import java.text.DateFormat
import java.util.*
import org.joda.time.DateTime
import org.readium.r2.shared.extensions.asInstance

/**
 * An exception that can be presented to the user using a localized message.
 */
open class UserException protected constructor(
    protected val content: Content,
    cause: Throwable?
) : Exception(cause) {

    constructor(@StringRes userMessageId: Int, vararg args: Any?, cause: Throwable? = null) :
        this(Content(userMessageId, *args), cause)

    constructor(
        @PluralsRes userMessageId: Int,
        quantity: Int?,
        vararg args: Any?,
        cause: Throwable? = null
    ) :
        this(Content(userMessageId, quantity, *args), cause)

    constructor(message: String, cause: Throwable? = null) :
        this(Content(message), cause)

    constructor(cause: UserException) :
        this(Content(cause), cause)

    /**
     * Gets the localized user-facing message for this exception.
     *
     * @param includesCauses Includes nested [UserException] causes in the user message when true.
     */
    open fun getUserMessage(context: Context, includesCauses: Boolean = true): String =
        content.getUserMessage(context, cause, includesCauses)

    /**
     * Provides a way to generate a localized user message.
     */
    protected sealed class Content {

        abstract fun getUserMessage(
            context: Context,
            cause: Throwable? = null,
            includesCauses: Boolean = true
        ): String

        /**
         * Holds a nested [UserException].
         */
        class Exception(val exception: UserException) : Content() {
            override fun getUserMessage(
                context: Context,
                cause: Throwable?,
                includesCauses: Boolean
            ): String =
                exception.getUserMessage(context, includesCauses)
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
                cause: Throwable?,
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
                    if (quantity != null) context.resources.getQuantityString(userMessageId, quantity, *(args.toTypedArray()))
                    else context.getString(userMessageId, *(args.toTypedArray()))

                // Includes nested causes if they are also [UserException].
                val userException = cause?.asInstance<UserException>()
                if (userException != null && includesCauses) {
                    message += ": ${userException.getUserMessage(context, includesCauses)}"
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
                cause: Throwable?,
                includesCauses: Boolean
            ): String = message
        }

        companion object {
            operator fun invoke(@StringRes userMessageId: Int, vararg args: Any?) =
                LocalizedString(userMessageId, args, null)
            operator fun invoke(@PluralsRes userMessageId: Int, quantity: Int?, vararg args: Any?) =
                LocalizedString(userMessageId, args, quantity)
            operator fun invoke(cause: UserException) =
                Exception(cause)
            operator fun invoke(message: String) =
                Message(message)
        }
    }
}
