/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.public

import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.lcp.license.model.components.Link
import org.readium.r2.lcp.license.model.components.lcp.User

interface LCPAuthenticating {
    fun requestPassphrase(license: LCPAuthenticatedLicense, reason: LCPAuthenticationReason, completion: (String?) -> Unit)
}


interface LCPAuthenticationDelegate {
    fun authenticate(license: LCPAuthenticatedLicense, passphrase: String)
    fun didCancelAuthentication(license: LCPAuthenticatedLicense)
}

enum class LCPAuthenticationReason {
    passphraseNotFound,
    invalidPassphrase
}

data class LCPAuthenticatedLicense(val document: LicenseDocument) {
    val hint: String
        get() = document.encryption.userKey.textHint
    val hintLink: Link?
        get() = document.link(LicenseDocument.Rel.hint)
    val supportLinks: List<Link>
        get() = document.links(LicenseDocument.Rel.support)
    val provider: String
        get() = document.provider
    val user: User?
        get() = document.user

}
