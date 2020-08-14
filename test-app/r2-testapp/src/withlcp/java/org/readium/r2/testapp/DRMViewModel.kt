/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp

import android.content.Context
import org.joda.time.DateTime
import org.readium.r2.shared.util.Try
import java.io.Serializable


abstract class DRMViewModel(val context: Context) : Serializable {

    abstract val type: String

    open val state: String? = null

    open val provider: String? = null

    open val issued: DateTime? = null

    open val updated: DateTime? = null

    open val start: DateTime? = null

    open val end: DateTime? = null

    open val copiesLeft: String = "unlimited"

    open val printsLeft: String = "unlimited"

    open val canRenewLoan: Boolean
        get() = false

    open suspend fun renewLoan(end: DateTime?): Try<Unit, Exception> =
        Try.failure(Exception("Renewing a loan is not supported"))

    open val canReturnPublication: Boolean
        get() = false

    open suspend fun returnPublication(): Try<Unit, Exception> =
        Try.failure(Exception("Returning a publication is not supported"))
}
