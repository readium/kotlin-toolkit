/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.lcp.fakes.FakeLcpLicense
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.protection.ContentProtection
import org.readium.r2.shared.publication.services.ContentProtectionService
import org.readium.r2.shared.util.data.EmptyContainer
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LcpContentProtectionServiceTest {

    @Test
    fun `isRestricted is true when license is null`() {
        val service = LcpContentProtectionService(license = null, error = null)
        assertTrue(service.isRestricted)
    }

    @Test
    fun `isRestricted is false when license is provided`() {
        val service = LcpContentProtectionService(license = FakeLcpLicense(), error = null)
        assertFalse(service.isRestricted)
    }

    @Test
    fun `rights is AllRestricted when license is null`() {
        val service = LcpContentProtectionService(license = null, error = null)
        assertEquals(ContentProtectionService.UserRights.AllRestricted, service.rights)
    }

    @Test
    fun `rights delegates to license when provided`() {
        val license = FakeLcpLicense(canCopy = true, canPrint = false)
        val service = LcpContentProtectionService(license = license, error = null)
        assertEquals(license, service.rights)
    }

    @Test
    fun `credentials is always null`() {
        val service = LcpContentProtectionService(license = null, error = null)
        assertNull(service.credentials)
    }

    @Test
    fun `scheme is always Lcp`() {
        val service = LcpContentProtectionService(license = null, error = null)
        assertEquals(ContentProtection.Scheme.Lcp, service.scheme)
    }

    @Test
    fun `close delegates to license close`() {
        val license = FakeLcpLicense()
        val service = LcpContentProtectionService(license = license, error = null)
        assertFalse(license.closed)
        service.close()
        assertTrue(license.closed)
    }

    @Test
    fun `createFactory returns a working factory`() {
        val license = FakeLcpLicense()
        val error = LcpError.MissingPassphrase
        val factory = LcpContentProtectionService.createFactory(license = license, error = error)
        val publication = Publication(
            manifest = Manifest(
                metadata = Metadata(
                    localizedTitle = LocalizedString(
                        value = "test"
                    )
                )
            )
        )
        val context = Publication.Service.Context(
            manifest = publication.manifest,
            container = EmptyContainer(),
            services = publication
        )
        val service = factory(context)

        assertEquals(license, service.license)
        assertEquals(error, service.error)
    }

    @Test
    fun `Publication lcpLicense extension returns license when service exists`() {
        val license = FakeLcpLicense()
        val factory = LcpContentProtectionService.createFactory(license = license, error = null)
        val publication = Publication(
            manifest = Manifest(
                metadata = Metadata(
                    localizedTitle = LocalizedString(
                        value = "test"
                    )
                )
            ),
            servicesBuilder = Publication.ServicesBuilder().apply {
                set(serviceType = ContentProtectionService::class, factory = factory)
            }
        )

        assertEquals(license, publication.lcpLicense)
    }

    @Test
    fun `Publication lcpLicense extension returns null when service does not exist`() {
        val publication = Publication(
            manifest = Manifest(
                metadata = Metadata(
                    localizedTitle = LocalizedString(
                        value = "test"
                    )
                )
            )
        )

        assertNull(publication.lcpLicense)
    }
}
