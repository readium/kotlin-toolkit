package org.readium.l2.lcp

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
import org.readium.r2.lcp.service.NetworkService
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

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var database: LcpDatabase
    private lateinit var lcpService: LicensesService
    private lateinit var assetRetriever: AssetRetriever
    private lateinit var network: NetworkService

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            context,
            LcpDatabase::class.java
        ).allowMainThreadQueries().build()

        val db = database.lcpDao()
        val deviceRepository = DeviceRepository(lcpDao = db)
        val passphraseRepository = PassphrasesRepository(lcpDao = db)
        val licenseRepository = LicensesRepository(lcpDao = db)

        network = NetworkService()

        val device = DeviceService(
            deviceName = "Test Device",
            deviceId = "test-id",
            repository = deviceRepository,
            network = network,
            context = context
        )
        val crl = CRLService(network = network, context = context)
        val passphrases = PassphrasesService(repository = passphraseRepository)

        assetRetriever = AssetRetriever(
            contentResolver = context.contentResolver,
            httpClient = DefaultHttpClient()
        )

        lcpService = LicensesService(
            licenses = licenseRepository,
            crl = crl,
            device = device,
            network = network,
            passphrases = passphrases,
            context = context,
            assetRetriever = assetRetriever
        )
    }

    @Test
    fun testSuccessfulValidationFlow() = runTest {
        val licenseJson =
            context.assets.open("active-lcpl.unknown").bufferedReader().use { it.readText() }

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
