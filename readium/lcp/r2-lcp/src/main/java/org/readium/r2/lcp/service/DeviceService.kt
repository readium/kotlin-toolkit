/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.service

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.SharedPreferences
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.lcp.license.model.components.Link
import timber.log.Timber
import java.io.Serializable
import java.util.*

internal class DeviceService(private val repository: DeviceRepository, private val network: NetworkService, val context: Context):Serializable {

    val preferences: SharedPreferences = context.getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)

    val id: String
        get() {
            val deviceId = preferences.getString("lcp_device_id", UUID.randomUUID().toString())!!
            preferences.edit().putString("lcp_device_id", deviceId).apply()
            return deviceId
        }
    val name: String
        get() {
            val bluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothName =
                try {
                    bluetoothManager.adapter.name
                } catch (e: Exception) {
                    Timber.e(e)
                    null
                }
            return bluetoothName ?: "Android"
        }

    val asQueryParameters: URLParameters
        get() = mapOf("id" to id, "name" to name)


    suspend fun registerLicense(license: LicenseDocument, link: Link): ByteArray? {
        if (repository.isDeviceRegistered(license)) {
            return null
        }

        val url = link.url(asQueryParameters).toString()
        val data = network.fetch(url, NetworkService.Method.POST, asQueryParameters)
            .getOrNull() ?: return null

        repository.registerDevice(license)
        return data
    }

}
