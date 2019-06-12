/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.service

import android.bluetooth.BluetoothAdapter
import android.content.Context
import kotlinx.coroutines.runBlocking
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.lcp.license.model.components.Link
import org.readium.r2.lcp.public.LCPError
import java.io.Serializable
import java.util.*

class DeviceService(private val repository: DeviceRepository, private val network: NetworkService, val context: Context):Serializable {

    val preferences = context.getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)

    val id: String
        get() {
            val deviceId = preferences.getString("lcp_device_id", UUID.randomUUID().toString())!!
            preferences.edit().putString("lcp_device_id", deviceId).apply()
            return deviceId
        }
    val name: String
        get() = {
            val deviceName = BluetoothAdapter.getDefaultAdapter()
            deviceName?.name?.let  {
                return@let it
            }?: run {
                return@run "Android Unknown"
            }
        }.toString()

    val asQueryParameters: MutableList<Pair<String, Any?>>
        get() = mutableListOf("id" to id, "name" to name)


    fun registerLicense(license: LicenseDocument, link: Link, completion: (ByteArray?) -> Unit) {
        runBlocking {
            val registered = repository.isDeviceRegistered(license)
            if (registered) {
                completion(null)
            }

            // TODO templated url
            val url = link.url(asQueryParameters).toString() ?: throw LCPError.licenseInteractionNotAvailable

            network.fetch(url, method = NetworkService.Method.post, params = asQueryParameters) { status, data ->
                if (status != 200) {
                    completion(null)
                }

                repository.registerDevice(license)
                completion(data)
            }

        }
    }

}
