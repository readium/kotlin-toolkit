/*
 * Module: r2-lcp-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.container

/**
 * Access a License Document stored in a Readium WebPub package (e.g. WebPub, Audiobook, LCPDF or DiViNa).
 */
internal class WebPubLicenseContainer(path: String)
    : ZIPLicenseContainer(zip = path, pathInZIP = "license.lcpl")
