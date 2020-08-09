/*
 * Module: r2-testapp-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */


package org.readium.r2.testapp.utils

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import org.readium.r2.shared.extensions.destroyPublication
import org.readium.r2.shared.extensions.getPublication
import org.readium.r2.shared.extensions.putPublication
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.File
import org.readium.r2.testapp.audiobook.AudiobookActivity
import org.readium.r2.testapp.comic.ComicActivity
import org.readium.r2.testapp.comic.DiViNaActivity
import org.readium.r2.testapp.epub.EpubActivity

class NavigatorContract : ActivityResultContract<NavigatorContract.Input, NavigatorContract.Output>() {

    data class Input(
        val file: File,
        val publication: Publication,
        val bookId: Long?,
        val initialLocator: Locator? = null,
        val deleteOnResult: Boolean = false
    )

    data class Output(
        val file: File,
        val publication: Publication,
        val deleteOnResult: Boolean
    )

    override fun createIntent(context: Context, input: Input): Intent {
        val intent = Intent(context, when (input.publication.type) {
            Publication.TYPE.AUDIO -> AudiobookActivity::class.java
            Publication.TYPE.CBZ -> ComicActivity::class.java
            Publication.TYPE.DiViNa -> DiViNaActivity::class.java
            else -> EpubActivity::class.java
        })

        return intent.apply {
            putPublication(input.publication)
            putExtra("bookId", input.bookId)
            putExtra("publicationPath", input.file.path)
            putExtra("publicationFileName", input.file.name)
            putExtra("deleteOnResult", input.deleteOnResult)
            if (input.initialLocator != null) {
                putExtra("locator", input.initialLocator)
            }
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Output? {
        if (intent == null)
            return null

        val path = intent.getStringExtra("publicationPath")
            ?: throw Exception("publicationPath required")

        intent.destroyPublication(null)

        return Output(
            file = File(path),
            publication = intent.getPublication(null),
            deleteOnResult = intent.getBooleanExtra("deleteOnResult", false)
        )
    }
}