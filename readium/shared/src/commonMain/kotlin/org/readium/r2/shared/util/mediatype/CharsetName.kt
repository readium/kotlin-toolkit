/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.mediatype

/**
 * Returns the canonical name of the character set with the given [name] (which may be an alias,
 * e.g. `ascii` for `US-ASCII`), or null if it is not recognized.
 */
internal expect fun canonicalCharsetName(name: String): String?
