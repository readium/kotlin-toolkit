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

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.readium.r2.shared.publication.Publication


/**
 * Created by aferditamuriqi on 1/16/18.
 */

class R2IntentMapper(private val mContext: Context, private val mIntents: R2IntentHelper) {

    fun dispatchIntent(intent: Intent) {

        // Get intent, action and MIME type
        val action = intent.action
        val type = intent.type
        val scheme = intent.scheme
        var uri: Uri? = null
        val clipData = intent.clipData
        val extras = intent.extras

        when (action) {
            Intent.ACTION_SEND -> {
                type?.let {
                    if ("text/plain" == it) {
                        intent.getStringExtra(Intent.EXTRA_TEXT).let { url ->
                            uri = Uri.parse(url)
                        }
                    } else {
                        uri = intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    }
                }
            }
            else -> {
                uri = intent.data ?: throw IllegalArgumentException("Uri cannot be null")
            }
        }

        uri?.let {
            if (uri.toString().contains(".")) {

                val extension = when (uri.toString().substring(uri.toString().lastIndexOf("."))) {
                    Publication.EXTENSION.EPUB.value -> Publication.EXTENSION.EPUB
                    Publication.EXTENSION.JSON.value -> Publication.EXTENSION.JSON
                    Publication.EXTENSION.AUDIO.value -> Publication.EXTENSION.AUDIO
                    Publication.EXTENSION.DIVINA.value -> Publication.EXTENSION.DIVINA
                    Publication.EXTENSION.LCPL.value -> Publication.EXTENSION.LCPL
                    Publication.EXTENSION.CBZ.value -> Publication.EXTENSION.CBZ
                    else -> Publication.EXTENSION.UNKNOWN
                }
                mContext.startActivity(mIntents.catalogActivityIntent(mContext, uri, extension))
            }
        }
    }
}
