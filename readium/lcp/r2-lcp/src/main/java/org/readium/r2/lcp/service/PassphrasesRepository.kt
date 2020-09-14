/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.service

internal interface PassphrasesRepository {
    fun passphrase(licenseId: String) : String?
    fun passphrases(userId: String) : List<String>
    fun allPassphrases(): List<String>
    fun addPassphrase(passphraseHash: String, licenseId: String?, provider: String?, userId: String?)
}
