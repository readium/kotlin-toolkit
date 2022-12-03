/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi, Mickaël Menu
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.container

/**
 * Access a License Document stored in an EPUB archive, under META-INF/license.lcpl.
 */
internal class EPUBLicenseContainer(epub: String) :
    ZIPLicenseContainer(zip = epub, pathInZIP = "META-INF/license.lcpl")
