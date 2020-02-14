/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.service

import org.readium.r2.lcp.license.model.LicenseDocument

internal interface DeviceRepository {
    fun isDeviceRegistered(license: LicenseDocument) : Boolean
    fun registerDevice(license: LicenseDocument)
}
