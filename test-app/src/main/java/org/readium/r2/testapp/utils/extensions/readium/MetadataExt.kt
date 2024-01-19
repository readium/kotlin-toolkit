/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.utils.extensions.readium

import org.readium.r2.shared.publication.Metadata

val Metadata.authorName: String get() =
    authors.firstOrNull()?.name ?: ""
