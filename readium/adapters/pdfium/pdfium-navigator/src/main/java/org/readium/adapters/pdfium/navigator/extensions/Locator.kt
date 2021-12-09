/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapters.pdfium.navigator.extensions

import org.readium.r2.navigator.extensions.fragmentParameters
import org.readium.r2.shared.publication.Locator

/**
 * Page fragment identifier, used for example in PDF.
 */
internal val Locator.Locations.page: Int? get() =
    fragmentParameters["page"]?.toIntOrNull()
