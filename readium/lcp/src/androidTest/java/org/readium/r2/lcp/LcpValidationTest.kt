/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.lcp.auth.LcpPassphraseAuthentication
import org.readium.r2.lcp.persistence.LcpDatabase
import org.readium.r2.lcp.service.CRLService
import org.readium.r2.lcp.service.DeviceRepository
import org.readium.r2.lcp.service.DeviceService
import org.readium.r2.lcp.service.LicensesRepository
import org.readium.r2.lcp.service.LicensesService
import org.readium.r2.lcp.service.PassphrasesRepository
import org.readium.r2.lcp.service.PassphrasesService
import org.readium.r2.shared.util.FileExtension
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.asset.ResourceAsset
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.FormatSpecification
import org.readium.r2.shared.util.format.Specification
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.InMemoryResource

@RunWith(AndroidJUnit4::class)
class LcpValidationTest {

    private val targetContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val testContext: Context = InstrumentationRegistry.getInstrumentation().context

    private lateinit var database: LcpDatabase
    private lateinit var lcpService: LicensesService
    private lateinit var assetRetriever: AssetRetriever

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            targetContext,
            LcpDatabase::class.java
        ).allowMainThreadQueries().build()

        val db = database.lcpDao()
        val deviceRepository = DeviceRepository(lcpDao = db)
        val passphraseRepository = PassphrasesRepository(lcpDao = db)
        val licenseRepository = LicensesRepository(lcpDao = db)

        val httpClient = DefaultHttpClient()

        val device = DeviceService(
            deviceName = "Test Device",
            deviceId = "test-id",
            repository = deviceRepository,
            httpClient = httpClient,
            context = targetContext
        )
        val crl = CRLService(httpClient = httpClient, context = targetContext)
        val passphrases = PassphrasesService(repository = passphraseRepository)

        assetRetriever = AssetRetriever(
            contentResolver = targetContext.contentResolver,
            httpClient = httpClient
        )

        lcpService = LicensesService(
            licenses = licenseRepository,
            crl = crl,
            device = device,
            httpClient = httpClient,
            passphrases = passphrases,
            context = targetContext,
            assetRetriever = assetRetriever
        )
    }

    @Test
    fun testSuccessfulValidationFlow() = runTest {
        val licenseJson =
            testContext.assets.open("active-lcpl-unknown.lcpl").bufferedReader().use { it.readText() }

        val licenseObj = JSONObject(licenseJson)
        val licenseId = licenseObj.getString("id")

        val lcplFormat = Format(
            specification = FormatSpecification(
                setOf(
                    Specification.Json,
                    Specification.LcpLicense
                )
            ),
            mediaType = MediaType.LCP_LICENSE_DOCUMENT,
            fileExtension = FileExtension("lcpl")
        )

        val asset = ResourceAsset(
            format = lcplFormat,
            resource = InMemoryResource(licenseObj.toString().toByteArray(Charsets.UTF_8))
        )

        val authentication = LcpPassphraseAuthentication(passphrase = "test")
        val result = lcpService.retrieveLicense(
            asset = asset,
            authentication = authentication,
            allowUserInteraction = false
        )

        assertTrue("License retrieval should succeed", result.isSuccess)
        val license = result.getOrNull()
        assertNotNull(license)
        assertEquals(licenseId, license?.license?.id)

        assertTrue(
            "Device should be registered in database",
            database.lcpDao().isDeviceRegistered(licenseId = licenseId)
        )
    }
}
