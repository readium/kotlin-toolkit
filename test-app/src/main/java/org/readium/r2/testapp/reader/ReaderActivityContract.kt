/*
 * Module: r2-testapp-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp.reader

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.bundleOf

class ReaderActivityContract
    : ActivityResultContract<ReaderActivityContract.Arguments, ReaderActivityContract.Arguments?>() {

    data class Arguments(val bookId: Long)

    override fun createIntent(context: Context, input: Arguments): Intent {
        val intent = Intent(context, ReaderActivity::class.java)
        val arguments =  bundleOf("bookId" to input.bookId)
        intent.putExtras(arguments)
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Arguments? {
        if (intent == null)
            return null

        val extras = requireNotNull(intent.extras)
        return parseExtras(extras)
    }

    companion object {

        fun parseIntent(activity: Activity): Arguments {
            val extras = requireNotNull(activity.intent.extras)
            return parseExtras(extras)
        }

        private fun parseExtras(extras: Bundle): Arguments {
            val bookId = extras.getLong("bookId")
            check(bookId != 0L)
            return Arguments(bookId)
        }
    }
}