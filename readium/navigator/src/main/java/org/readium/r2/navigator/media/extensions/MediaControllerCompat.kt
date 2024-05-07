/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:Suppress("DEPRECATION")

package org.readium.r2.navigator.media.extensions

import android.support.v4.media.session.MediaControllerCompat
import org.readium.r2.navigator.media.MediaService
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.PublicationId

@InternalReadiumApi
internal val MediaControllerCompat.publicationId: PublicationId?
    get() = extras?.getString(MediaService.EXTRA_PUBLICATION_ID)
