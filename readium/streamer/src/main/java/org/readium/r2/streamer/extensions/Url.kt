/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.extensions

import org.readium.r2.shared.util.Url

internal val Url.isHiddenOrThumbs: Boolean
    get() = filename?.let { it.startsWith(".") || it == "Thumbs.db" } ?: false
