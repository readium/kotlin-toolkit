/*
 * Module: r2-lcp-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp

import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.ContentProtectionService

internal class LCPContentProtectionService(license: LCPLicense?) : ContentProtectionService {

    override val isRestricted: Boolean = license != null

    override val credentials: String? = null

    override val rights: ContentProtectionService.UserRights =
        license?.let { LCPUserRights(it) }
            ?: ContentProtectionService.UserRights.AllRestricted

    override val name: LocalizedString = LocalizedString("Readium LCP")

    private class LCPUserRights(val license: LCPLicense) : ContentProtectionService.UserRights {

        override val canCopy: Boolean
            get() = license.canCopy

        override fun canCopy(text: String): Boolean =
            license.charactersToCopyLeft?.let { it <= text.length }
                ?: true

        override fun copy(text: String): Boolean =
            canCopy(text).also { if (it) license.copy(text) }

        override val canPrint: Boolean
            get() = license.canPrint

        override fun canPrint(pageCount: Int): Boolean =
            license.pagesToPrintLeft?.let { it <= pageCount }
                ?: true

        override fun print(pageCount: Int): Boolean =
            canPrint(pageCount).also { if (it) license.print(pageCount) }

    }

    companion object {

        fun createFactory(license: LCPLicense?): (Publication.Service.Context) -> LCPContentProtectionService =
            { LCPContentProtectionService(license) }

    }
}
