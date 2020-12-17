/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import org.readium.r2.testapp.library.LibraryActivity
import timber.log.Timber

class R2DispatcherActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dispatchIntent(intent)
        finish()
    }

    private fun dispatchIntent(intent: Intent) {
        val uri = uriFromIntent(intent)
            ?: run {
                Timber.d("Got an empty intent.")
                return
            }
        val newIntent = Intent(this, LibraryActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            data = uri
        }
        startActivity(newIntent)
    }

    private fun uriFromIntent(intent: Intent): Uri? =
        when (intent.action) {
            Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    intent.getStringExtra(Intent.EXTRA_TEXT).let { Uri.parse(it) }
                } else {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
            }
            else -> {
                intent.data
            }
        }
}