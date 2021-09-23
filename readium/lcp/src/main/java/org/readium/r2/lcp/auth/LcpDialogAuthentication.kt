/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListPopupWindow
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.lcp.LcpAuthenticating
import org.readium.r2.lcp.R
import org.readium.r2.lcp.license.model.components.Link
import org.readium.r2.shared.extensions.tryOr
import org.readium.r2.shared.extensions.tryOrNull
import timber.log.Timber
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * An [LcpAuthenticating] implementation presenting a dialog to the user.
 *
 * For this authentication to trigger, you must provide a [sender] parameter of type [Activity],
 * [Fragment] or [View] to `Streamer::open()` or `LcpService::retrieveLicense()`. It will be used as
 * the host view for the dialog.
 */
class LcpDialogAuthentication : LcpAuthenticating {

    override suspend fun retrievePassphrase(
        license: LcpAuthenticating.AuthenticatedLicense,
        reason: LcpAuthenticating.AuthenticationReason,
        allowUserInteraction: Boolean,
        sender: Any?
    ): String? =
        if (allowUserInteraction) withContext(Dispatchers.Main) { askPassphrase(license, reason, sender) }
        else null

    private suspend fun askPassphrase(license: LcpAuthenticating.AuthenticatedLicense, reason: LcpAuthenticating.AuthenticationReason, sender: Any?): String? {
        val hostView = (sender as? View) ?: (sender as? Activity)?.findViewById<ViewGroup>(android.R.id.content)?.getChildAt(0) ?: (sender as? Fragment)?.view
        ?: run {
            Timber.e("No valid [sender] was passed to `LcpDialogAuthentication::retrievePassphrase()`. Make sure it is an Activity, a Fragment or a View.")
            return null
        }
        val context = hostView.context

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        @SuppressLint("InflateParams")  // https://stackoverflow.com/q/26404951/1474476
        val dialogView = inflater.inflate(R.layout.r2_lcp_auth_dialog, null)

        val title = dialogView.findViewById(R.id.r2_title) as TextView
        val description = dialogView.findViewById(R.id.r2_description) as TextView
        val hint = dialogView.findViewById(R.id.r2_hint) as TextView
        val passwordLayout = dialogView.findViewById(R.id.r2_passwordLayout) as TextInputLayout
        val password = dialogView.findViewById(R.id.r2_password) as TextInputEditText
        val confirmButton = dialogView.findViewById(R.id.r2_confirmButton) as Button
        val cancelButton = dialogView.findViewById(R.id.r2_cancelButton) as Button
        val forgotButton = dialogView.findViewById(R.id.r2_forgotButton) as Button
        val helpButton = dialogView.findViewById(R.id.r2_helpButton) as Button

        forgotButton.isVisible = license.hintLink != null
        helpButton.isVisible = license.supportLinks.isNotEmpty()

        when (reason) {
            LcpAuthenticating.AuthenticationReason.PassphraseNotFound -> {
                title.text = context.getString(R.string.r2_lcp_dialog_reason_passphraseNotFound)
            }
            LcpAuthenticating.AuthenticationReason.InvalidPassphrase -> {
                title.text = context.getString(R.string.r2_lcp_dialog_reason_invalidPassphrase)
                passwordLayout.error = context.getString(R.string.r2_lcp_dialog_reason_invalidPassphrase)
            }
        }

        val provider = tryOr(license.provider) { Uri.parse(license.provider).host }
        description.text = context.getString(R.string.r2_lcp_dialog_prompt, provider)

        hint.text = license.hint

        return suspendCoroutine { cont ->
            val popupWindow = PopupWindow(dialogView, ListPopupWindow.MATCH_PARENT, ListPopupWindow.MATCH_PARENT).apply {
                isOutsideTouchable = false
                isFocusable = true
                elevation = 5.0f
            }

            cancelButton.setOnClickListener {
                popupWindow.dismiss()
                cont.resume(null)
            }

            confirmButton.setOnClickListener {
                popupWindow.dismiss()
                cont.resume(password.text.toString())
            }

            forgotButton.setOnClickListener {
                license.hintLink?.let { context.startActivityForLink(it) }
            }

            helpButton.setOnClickListener {
                showHelpDialog(context, license.supportLinks)
            }

            popupWindow.showAtLocation(hostView, Gravity.CENTER, 0, 0)
        }
    }

    private fun showHelpDialog(context: Context, links: List<Link>) {
        val titles = links.map {
            it.title ?: tryOr(context.getString(R.string.r2_lcp_dialog_support)) {
                when (Uri.parse(it.href).scheme) {
                    "http", "https" -> context.getString(R.string.r2_lcp_dialog_support_web)
                    "tel" -> context.getString(R.string.r2_lcp_dialog_support_phone)
                    "mailto" -> context.getString(R.string.r2_lcp_dialog_support_mail)
                    else -> context.getString(R.string.r2_lcp_dialog_support)
                }
            }
        }.toTypedArray()

        AlertDialog.Builder(context)
            .setItems(titles) { _, i ->
                context.startActivityForLink(links[i])
            }
            .show()
    }

    private fun Context.startActivityForLink(link: Link) {
        val url = tryOrNull { Uri.parse(link.href) } ?: return

        val action = when (url.scheme?.lowercase(Locale.ROOT)) {
            "http", "https" -> Intent(Intent.ACTION_VIEW)
            "tel" -> Intent(Intent.ACTION_CALL)
            "mailto" -> Intent(Intent.ACTION_SEND)
            else -> Intent(Intent.ACTION_VIEW)
        }

        startActivity(Intent(action).apply {
            data = url
        })
    }

}
