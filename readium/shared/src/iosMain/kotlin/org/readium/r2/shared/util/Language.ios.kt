/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import platform.Foundation.NSLocale
import platform.Foundation.countryCode

internal actual fun localeRegionOf(bcp47Tag: String): String? =
    NSLocale(localeIdentifier = bcp47Tag).countryCode
        ?.takeIf { it.isNotEmpty() }
