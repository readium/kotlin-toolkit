/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media3.api

import org.readium.r2.shared.publication.Publication

/**
 *  To be implemented to use a custom [MediaMetadataFactory].
 */
fun interface MediaMetadataProvider {

    fun createMetadataFactory(publication: Publication): MediaMetadataFactory
}
