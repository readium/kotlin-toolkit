/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.service

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import java.util.UUID
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.lcp.license.model.components.Link

internal class DeviceService(
    deviceName: String?,
    deviceId: String?,
    private val repository: DeviceRepository,
    private val network: NetworkService,
    val context: Context,
) {

    val id: String = deviceId ?: generatedId

    private val generatedId: String get() {
        val key = "lcp_device_id"

        val preferences = context.getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)

        preferences.getString(key, null)
            ?.let { return it }

        val id = UUID.randomUUID().toString()
        preferences.edit { putString(key, id) }
        return id
    }

    val name: String =
        deviceName ?: "${Build.MANUFACTURER} ${Build.MODEL}"

    val asQueryParameters: URLParameters
        get() = mapOf("id" to id, "name" to name)

    suspend fun registerLicense(license: LicenseDocument, link: Link): ByteArray? {
        if (repository.isDeviceRegistered(license)) {
            return null
        }

        val url = link.url(parameters = asQueryParameters).toString()
        val data = network.fetch(url, NetworkService.Method.POST, asQueryParameters)
            .getOrNull() ?: return null

        repository.registerDevice(license)
        return data
    }
}
