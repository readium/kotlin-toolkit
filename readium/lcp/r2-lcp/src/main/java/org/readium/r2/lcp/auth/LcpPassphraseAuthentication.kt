/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ListPopupWindow
import android.widget.PopupWindow
import android.widget.TextView
import org.jetbrains.anko.alert
import org.jetbrains.anko.contentView
import org.readium.r2.lcp.LcpAuthenticating
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * An [LcpAuthenticating] implementation which can directly use a provided clear or hashed
 * passphrase.
 *
 * If the provided [passphrase] is incorrect, the given [fallback] authentication is used.
 */
class LcpPassphraseAuthentication(
    private val passphrase: String,
    private val fallback: LcpAuthenticating? = null
) : LcpAuthenticating {

    override suspend fun retrievePassphrase(
        license: LcpAuthenticating.AuthenticatedLicense,
        reason: LcpAuthenticating.AuthenticationReason,
        allowUserInteraction: Boolean,
        sender: Any?
    ): String? {
        if (reason != LcpAuthenticating.AuthenticationReason.PassphraseNotFound) {
            return fallback?.retrievePassphrase(license, reason, allowUserInteraction = allowUserInteraction, sender = sender)
        }

        return passphrase
    }

}