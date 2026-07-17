/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.mediatype

import java.nio.charset.Charset
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.tryOrNull

/**
 * Encoding as declared in the `charset` parameter, if there's any.
 */
@OptIn(InternalReadiumApi::class)
public val MediaType.charset: Charset? get() =
    parameters["charset"]?.let { tryOrNull { Charset.forName(it) } }
