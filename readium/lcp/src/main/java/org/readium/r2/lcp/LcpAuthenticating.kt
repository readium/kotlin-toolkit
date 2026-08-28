/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi, Mickaël Menu
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp

import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.lcp.license.model.components.Link
import org.readium.r2.lcp.license.model.components.lcp.User

public interface LcpAuthenticating {

    /**
     * Retrieves the passphrase used to decrypt the given license.
     *
     * If [allowUserInteraction] is true, the reading app can prompt the user to enter the
     * passphrase. Otherwise, use a background retrieval method (e.g. web service) or return null.
     *
     * The returned passphrase can be clear or already hashed.
     *
     * You can implement an asynchronous pop-up with callbacks using `suspendCoroutine`:
     * ```
     * suspendCoroutine<String?> { cont ->
     *     cancelButton.setOnClickListener {
     *         cont.resume(null)
     *     }
     *
     *     okButton.setOnClickListener {
     *         cont.resume(passwordEditText.text.toString())
     *     }
     *
     *     // show pop-up...
     * }
     * ```
     *
     * @param license Information to show to the user about the license being opened.
     * @param reason Reason why the passphrase is requested. It should be used to prompt the user.
     * @param allowUserInteraction Indicates whether the user can be prompted for their passphrase.
     */
    public suspend fun retrievePassphrase(
        license: AuthenticatedLicense,
        reason: AuthenticationReason,
        allowUserInteraction: Boolean,
    ): String?

    public enum class AuthenticationReason {

        /** No matching passphrase was found. */
        PassphraseNotFound,

        /** The provided passphrase was invalid. */
        InvalidPassphrase,

        ;

        public companion object
    }

    /**
     * @param document License Document being opened.
     */
    public data class AuthenticatedLicense(val document: LicenseDocument) {

        /**
         * A hint to be displayed to the User to help them remember the User Passphrase.
         */
        val hint: String
            get() = document.encryption.userKey.textHint

        /**
         * Location where a Reading System can redirect a User looking for additional information
         * about the User Passphrase.
         */
        val hintLink: Link?
            get() = document.link(LicenseDocument.Rel.Hint)

        /**
         * Support resources for the user (either a website, an email or a telephone number).
         */
        val supportLinks: List<Link>
            get() = document.links(LicenseDocument.Rel.Support)

        /**
         * URI of the license provider.
         */
        val provider: String
            get() = document.provider

        /**
         * Informations about the user owning the license.
         */
        val user: User
            get() = document.user
    }
}
