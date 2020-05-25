/*
 * Module: r2-testapp-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp.utils.extensions

import org.readium.r2.shared.publication.Metadata

val Metadata.authorName: String get() =
    authors.firstOrNull()?.name ?: ""
