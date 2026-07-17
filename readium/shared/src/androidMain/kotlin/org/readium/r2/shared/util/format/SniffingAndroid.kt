/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.format

import java.nio.charset.Charset
import org.readium.r2.shared.util.mediatype.charset

/** Finds the first [Charset] declared in the media types' `charset` parameter. */
public val FormatHints.charset: Charset? get() =
    mediaTypes.firstNotNullOfOrNull { it.charset }
