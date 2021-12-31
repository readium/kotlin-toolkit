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
import org.readium.r2.shared.extensions.destroyPublication
import org.readium.r2.shared.extensions.getPublication
import org.readium.r2.shared.extensions.getPublicationOrNull
import org.readium.r2.shared.extensions.putPublication
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import java.net.URL

class ReaderContract : ActivityResultContract<ReaderContract.Input, ReaderContract.Output?>() {

    data class Input(
        val bookId: Long,
        val publication: Publication,
        val initialLocator: Locator? = null,
        val baseUrl: URL? = null
    )

    data class Output(
        val publication: Publication
    )

    override fun createIntent(context: Context, input: Input): Intent {
        val intent = Intent(
            context,
            when  {
                input.publication.conformsTo(Publication.Profile.AUDIOBOOK) ->
                    ReaderActivity::class.java
                input.publication.conformsTo(Publication.Profile.EPUB) ||
                        input.publication.conformsTo(Publication.Profile.DIVINA) ||
                        input.publication.conformsTo(Publication.Profile.PDF) ->
                    VisualReaderActivity::class.java
                else ->
                    throw IllegalArgumentException("Unknown [mediaType]")
            },
        )
        val arguments = createBundle(input)
        intent.putExtras(arguments)
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Output? {
        if (intent == null)
            return null

        intent.destroyPublication(null)
        return Output(
            publication = intent.getPublication(null),
        )
    }

    companion object {

        fun createBundle(input: Input): Bundle = bundleOf(
            "bookId" to input.bookId,
            "baseUrl" to input.baseUrl?.toString(),
            "locator" to input.initialLocator,
        ).apply {
            putPublication(input.publication)
        }

        fun parseBundle(bundle: Bundle): Input? {
            val publication = bundle.getPublicationOrNull()
                ?: return null

            val bookId = bundle.getLong("bookId", -1)
                .takeUnless { it == -1L }
                ?: return null

            return Input(
                bookId = bookId,
                publication = publication,
                initialLocator = bundle.getParcelable("locator"),
                baseUrl = bundle.getString("baseUrl")?.let { URL(it) }
            )
        }

        fun parseIntent(activity: Activity): Input? =
            parseBundle(activity.intent.extras!!)
    }
}