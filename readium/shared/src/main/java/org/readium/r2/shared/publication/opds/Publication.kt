/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.opds

import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.Link

// OPDS extensions for [Publication]

val Publication.images: List<Link> get() = linksWithRole("images")
