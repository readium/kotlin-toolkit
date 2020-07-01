/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.extensions

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import org.readium.r2.shared.BuildConfig
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import timber.log.Timber
import java.util.*

private val extraKey = "publicationId"
private val deprecationException = IllegalArgumentException("The [publication] intent extra is not supported anymore. Use the shared [PublicationRepository] instead.")

fun Intent.putPublication(publication: Publication) {
    val id = PublicationRepository.add(publication)
    putExtra(extraKey, id)
}

fun Intent.putPublicationFrom(activity: Activity) {
    putExtra(extraKey, activity.intent.getStringExtra(extraKey))
}

fun Intent.getPublication(activity: Activity?): Publication {
    if (hasExtra("publication")) {
        if (BuildConfig.DEBUG) {
            throw deprecationException
        } else {
            Timber.e(deprecationException)
        }
        activity?.finish()
    }

    val publication = getStringExtra(extraKey)?.let { PublicationRepository.get(it) }
    if (publication == null) {
        activity?.finish()
        // Fallbacks on a dummy Publication to avoid crashing the app until the Activity finishes.
        val metadata = Metadata(identifier = "dummy", localizedTitle = LocalizedString(""))
        return Publication(Manifest(metadata = metadata))
    }

    return publication
}

fun Intent.getPublicationOrNull(activity: Activity): Publication? {
    if (hasExtra("publication")) {
        if (BuildConfig.DEBUG) {
            throw deprecationException
        } else {
            Timber.e(deprecationException)
        }
    }

    return getStringExtra(extraKey)?.let { PublicationRepository.get(it) }
}

fun Intent.destroyPublication(activity: Activity?) {
    if (activity == null || activity.isFinishing) {
        getStringExtra(extraKey)?.let {
            PublicationRepository.remove(it)
        }
    }
}

fun Bundle.putPublication(publication: Publication) {
    val id = PublicationRepository.add(publication)
    putString(extraKey, id)
}

fun Bundle.putPublicationFrom(activity: Activity) {
    putString(extraKey, activity.intent.getStringExtra(extraKey))
}

fun Bundle.getPublicationOrNull(): Publication? {
    return getString(extraKey)?.let { PublicationRepository.get(it) }
}

/**
 * [Publication] is too big to be sent through an Intent extra, which can produce a
 * TransactionTooLargeException exception when starting an Activity.
 * See this issue for more information: https://github.com/readium/r2-testapp-kotlin/issues/286
 *
 * To circumvent the problem, you can use this shared [PublicationRepository] to store a
 * [Publication] meant to be shared between activities.
 *
 * This PR shows all the changes to do in the app:
 * https://github.com/readium/r2-testapp-kotlin/pull/303
 *
 * In the source Activity, do:
 * intent.putPublication(publication)
 *
 * And in the target Activity, in onCreate():
 * val publication = intent.getPublication(this)
 *
 * Don't forget to call this in Activity.onDestroy(), to release the Publication:
 * intent.destroyPublication(activity)
 */
private object PublicationRepository {

    private val publications = mutableMapOf<String, Publication>()

    /** Gets the [Publication] stored in the repository with the given ID. */
    fun get(id: String): Publication? = publications[id]

    /** Adds a [Publication] to the repository and returns its ID. */
    fun add(publication: Publication): String {
        val id = createId()
        publications[id] = publication
        return id
    }

    /** Removes the [Publication] with the given [id] from the repository. */
    fun remove(id: String) {
        publications.remove(id)
    }

    /** Removes all the [Publication] in the repository. */
    fun clear() {
        publications.clear()
    }

    private fun createId(): String {
        val id = UUID.randomUUID().toString()
        if (publications.containsKey(id)) {
            return createId()
        }

        return id
    }

}
