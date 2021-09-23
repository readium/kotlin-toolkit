/*
 * Module: r2-lcp-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license.container

import org.readium.r2.lcp.license.model.LicenseDocument

/**
 * Access a License Document from its raw bytes.
 */
internal class BytesLicenseContainer(private var bytes: ByteArray) : LicenseContainer {

    override fun read() : ByteArray = bytes

    override fun write(license: LicenseDocument) {
        bytes = license.data
    }

}

