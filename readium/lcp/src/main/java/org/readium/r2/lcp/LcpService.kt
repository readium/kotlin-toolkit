/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi, Mickaël Menu
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp

import android.content.Context
import java.io.File
import kotlinx.coroutines.DelicateCoroutinesApi
import org.readium.r2.lcp.auth.LcpDialogAuthentication
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.lcp.persistence.LcpDatabase
import org.readium.r2.lcp.service.CRLService
import org.readium.r2.lcp.service.DeviceRepository
import org.readium.r2.lcp.service.DeviceService
import org.readium.r2.lcp.service.LcpClient
import org.readium.r2.lcp.service.LicensesRepository
import org.readium.r2.lcp.service.LicensesService
import org.readium.r2.lcp.service.NetworkService
import org.readium.r2.lcp.service.PassphrasesRepository
import org.readium.r2.lcp.service.PassphrasesService
import org.readium.r2.shared.publication.protection.ContentProtection
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.asset.ContainerAsset
import org.readium.r2.shared.util.format.Format

/**
 * Service used to acquire and open publications protected with LCP.
 */
public interface LcpService {

    /**
     * Returns if the file is a LCP license document or a publication protected by LCP.
     */
    @Deprecated(
        "Use an AssetSniffer and check the conformance of the returned format to LcpSpecification",
        level = DeprecationLevel.ERROR
    )
    public suspend fun isLcpProtected(file: File): Boolean {
        throw NotImplementedError()
    }

    /**
     * Acquires a protected publication from a standalone LCPL's bytes.
     *
     * License will be injected into the publication archive without explicitly calling
     * [injectLicenseDocument].
     * You can cancel the on-going acquisition by cancelling its parent coroutine context.
     *
     * @param onProgress Callback to follow the acquisition progress from 0.0 to 1.0.
     */
    public suspend fun acquirePublication(
        lcpl: ByteArray,
        onProgress: (Double) -> Unit = {}
    ): Try<AcquiredPublication, LcpError>

    /**
     * Acquires a protected publication from a standalone LCPL file.
     *
     * License will be injected into the publication archive without explicitly calling
     * [injectLicenseDocument].
     * You can cancel the on-going acquisition by cancelling its parent coroutine context.
     *
     * @param onProgress Callback to follow the acquisition progress from 0.0 to 1.0.
     */
    public suspend fun acquirePublication(
        lcpl: File,
        onProgress: (Double) -> Unit = {}
    ): Try<AcquiredPublication, LcpError>

    /**
     * Opens the LCP license of a protected publication, to access its DRM metadata and decipher
     * its content.
     *
     * @param authentication Used to retrieve the user passphrase if it is not already known.
     *        The request will be cancelled if no passphrase is found in the LCP passphrase storage
     *        and the provided [authentication].
     * @param allowUserInteraction Indicates whether the user can be prompted for their passphrase.
     * @param sender Free object that can be used by reading apps to give some UX context when
     *        presenting dialogs with [LcpAuthenticating].
     */
    @Deprecated(
        "Use the overload taking an asset instead.",
        level = DeprecationLevel.ERROR
    )
    public suspend fun retrieveLicense(
        file: File,
        authentication: LcpAuthenticating = LcpDialogAuthentication(),
        allowUserInteraction: Boolean,
        sender: Any? = null
    ): Try<LcpLicense, LcpError>? {
        throw NotImplementedError()
    }

    /**
     * Opens the LCP license of a protected publication, to access its DRM metadata and decipher
     * its content. If the updated license cannot be stored into the [Asset], you'll get
     * an exception if the license points to a LSD server that cannot be reached,
     * for instance because no Internet gateway is available.
     *
     * Updated licenses can currently be stored only into [Asset]s whose source property points to
     * a URL with scheme _file_ or _content_.
     *
     * @param authentication Used to retrieve the user passphrase if it is not already known.
     *        The request will be cancelled if no passphrase is found in the LCP passphrase storage
     *        and the provided [authentication].
     * @param allowUserInteraction Indicates whether the user can be prompted for their passphrase.
     */
    public suspend fun retrieveLicense(
        asset: Asset,
        authentication: LcpAuthenticating,
        allowUserInteraction: Boolean
    ): Try<LcpLicense, LcpError>

    /**
     * Retrieves the license document from a LCP-protected publication asset.
     */
    public suspend fun retrieveLicenseDocument(
        asset: ContainerAsset
    ): Try<LicenseDocument, LcpError>

    /**
     * Injects a [licenseDocument] into the given [publicationFile] package.
     *
     * This is useful if you downloaded the publication yourself instead of using [acquirePublication].
     */
    public suspend fun injectLicenseDocument(
        licenseDocument: LicenseDocument,
        publicationFile: File
    ): Try<Unit, LcpError>

    /**
     * Creates a [ContentProtection] instance which can be used with a Streamer to unlock
     * LCP protected publications.
     *
     * The provided [authentication] will be used to retrieve the user passphrase when opening an
     * LCP license. The default implementation [LcpDialogAuthentication] presents a dialog to the
     * user to enter their passphrase.
     */
    public fun contentProtection(
        authentication: LcpAuthenticating
    ): ContentProtection

    /**
     * Information about an acquired publication protected with LCP.
     *
     * @param localFile Path to the downloaded publication. You must move this file to the user
     *        library's folder.
     * @param suggestedFilename Filename that should be used for the publication when importing it in
     *        the user library.
     */
    public data class AcquiredPublication(
        val localFile: File,
        val suggestedFilename: String,
        val format: Format,
        val licenseDocument: LicenseDocument
    ) {
        @Deprecated(
            "Use `localFile` instead",
            replaceWith = ReplaceWith("localFile"),
            level = DeprecationLevel.ERROR
        )
        val localURL: String get() = localFile.path
    }

    public companion object {

        /**
         * LCP service factory.
         *
         * @param deviceName Device name used when registering a license to an LSD server.
         * If not provided, the device name will be generated from the device's manufacturer and
         * model.
         */
        public operator fun invoke(
            context: Context,
            assetRetriever: AssetRetriever,
            deviceName: String? = null
        ): LcpService? {
            if (!LcpClient.isAvailable()) {
                return null
            }

            val db = LcpDatabase.getDatabase(context).lcpDao()
            val deviceRepository = DeviceRepository(db)
            val passphraseRepository = PassphrasesRepository(db)
            val licenseRepository = LicensesRepository(db)
            val network = NetworkService()
            val device = DeviceService(
                deviceName = deviceName,
                repository = deviceRepository,
                network = network,
                context = context
            )
            val crl = CRLService(network = network, context = context)
            val passphrases = PassphrasesService(repository = passphraseRepository)
            return LicensesService(
                licenses = licenseRepository,
                crl = crl,
                device = device,
                network = network,
                passphrases = passphrases,
                context = context,
                assetRetriever = assetRetriever
            )
        }

        @Suppress("UNUSED_PARAMETER")
        @Deprecated(
            "Use `LcpService()` instead",
            ReplaceWith("LcpService(context, AssetRetriever(), MediaTypeRetriever())"),
            level = DeprecationLevel.ERROR
        )
        public fun create(context: Context): LcpService = throw NotImplementedError()
    }

    @Deprecated(
        "Use `acquirePublication()` with coroutines instead",
        ReplaceWith("acquirePublication(lcpl)")
    )
    @DelicateCoroutinesApi
    public fun importPublication(
        lcpl: ByteArray,
        authentication: LcpAuthenticating?,
        completion: (AcquiredPublication?, LcpError?) -> Unit
    ) {
        throw NotImplementedError()
    }

    @Deprecated(
        "Use `retrieveLicense()` with coroutines instead",
        ReplaceWith(
            "retrieveLicense(File(publication), authentication, allowUserInteraction = true)"
        ),
        level = DeprecationLevel.ERROR
    )
    @DelicateCoroutinesApi
    public fun retrieveLicense(
        publication: String,
        authentication: LcpAuthenticating?,
        completion: (LcpLicense?, LcpError?) -> Unit
    ) {
        throw NotImplementedError()
    }
}

@Suppress("UNUSED_PARAMETER")
@Deprecated(
    "Renamed to `LcpService()`",
    replaceWith = ReplaceWith("LcpService(context)"),
    level = DeprecationLevel.ERROR
)
public fun R2MakeLCPService(context: Context): LcpService =
    throw NotImplementedError()

@Deprecated(
    "Renamed to `LcpService.AcquiredPublication`",
    replaceWith = ReplaceWith("LcpService.AcquiredPublication"),
    level = DeprecationLevel.ERROR
)
public typealias LCPImportedPublication = LcpService.AcquiredPublication
