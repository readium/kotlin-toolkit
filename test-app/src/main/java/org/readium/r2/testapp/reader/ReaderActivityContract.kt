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
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.bundleOf

class ReaderActivityContract
    : ActivityResultContract<NavigatorType, NavigatorType?>() {

    override fun createIntent(context: Context, input: NavigatorType): Intent {
        val intent = Intent(context, ReaderActivity::class.java)
        val arguments =  bundleOf("type" to input)
        intent.putExtras(arguments)
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): NavigatorType {
        val extras = requireNotNull(intent?.extras)
        return extras.getSerializable("type") as NavigatorType

    }

    companion object {

        fun parseIntent(activity: Activity): NavigatorType {
            val extras = requireNotNull(activity.intent.extras)
            return extras.getSerializable("type") as NavigatorType
        }
    }
}