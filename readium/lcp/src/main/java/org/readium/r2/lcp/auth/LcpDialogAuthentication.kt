/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.lcp.auth

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Editable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ListPopupWindow
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.readium.r2.lcp.LcpAuthenticating
import org.readium.r2.lcp.R
import org.readium.r2.lcp.license.model.components.Link
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.tryOr
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.toUri

/**
 * An [LcpAuthenticating] implementation presenting a dialog to the user.
 *
 * This authentication requires a view to anchor on. To use it, you'll need to call
 * [onParentViewAttachedToWindow] every time it gets attached to a window and
 * [onParentViewDetachedFromWindow] when it gets detached. You can typically achieve this with
 * a [View.OnAttachStateChangeListener]. Without view to anchor on, [retrievePassphrase] will
 * suspend until one is available.
 */
public class LcpDialogAuthentication : LcpAuthenticating {

    private class SuspendedCall(
        val continuation: Continuation<String?>,
        val license: LcpAuthenticating.AuthenticatedLicense,
        val reason: LcpAuthenticating.AuthenticationReason,
        var currentInput: Editable? = null,
    )

    private val mutex: Mutex = Mutex()
    private var suspendedCall: SuspendedCall? = null
    private var parentView: View? = null

    /**
     * Call this method every time the anchor view gets attached to the window.
     */
    public fun onParentViewAttachedToWindow(parentView: View) {
        this@LcpDialogAuthentication.parentView = parentView
        suspendedCall?.let { showPopupWindow(it, parentView) }
    }

    /**
     * Call this method every time the anchor view gets detached from the window.
     */
    public fun onParentViewDetachedFromWindow() {
        this.parentView = null
    }

    override suspend fun retrievePassphrase(
        license: LcpAuthenticating.AuthenticatedLicense,
        reason: LcpAuthenticating.AuthenticationReason,
        allowUserInteraction: Boolean,
    ): String? =
        if (allowUserInteraction) {
            withContext(Dispatchers.Main) {
                askPassphrase(
                    license,
                    reason
                )
            }
        } else {
            null
        }

    private suspend fun askPassphrase(
        license: LcpAuthenticating.AuthenticatedLicense,
        reason: LcpAuthenticating.AuthenticationReason,
    ): String? {
        mutex.lock()

        return suspendCoroutine { cont ->
            val suspendedCall = SuspendedCall(cont, license, reason)
            this.suspendedCall = suspendedCall
            parentView?.let { showPopupWindow(suspendedCall, it) }
        }
    }

    private fun terminateCall() {
        suspendedCall = null
        mutex.unlock()
    }

    private fun showPopupWindow(
        suspendedCall: SuspendedCall,
        hostView: View,
    ) {
        val context = hostView.context

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        @SuppressLint("InflateParams") // https://stackoverflow.com/q/26404951/1474476
        val dialogView = inflater.inflate(R.layout.readium_lcp_auth_dialog, null)

        val title = dialogView.findViewById(R.id.r2_title) as TextView
        val description = dialogView.findViewById(R.id.r2_description) as TextView
        val hint = dialogView.findViewById(R.id.r2_hint) as TextView
        val passwordLayout = dialogView.findViewById(R.id.r2_passwordLayout) as TextInputLayout
        val password = dialogView.findViewById(R.id.r2_password) as TextInputEditText
        val confirmButton = dialogView.findViewById(R.id.r2_confirmButton) as Button
        val cancelButton = dialogView.findViewById(R.id.r2_cancelButton) as Button
        val forgotButton = dialogView.findViewById(R.id.r2_forgotButton) as Button
        val helpButton = dialogView.findViewById(R.id.r2_helpButton) as Button

        password.text = suspendedCall.currentInput
        password.addTextChangedListener { suspendedCall.currentInput = it }

        forgotButton.isVisible = suspendedCall.license.hintLink != null
        helpButton.isVisible = suspendedCall.license.supportLinks.isNotEmpty()

        when (suspendedCall.reason) {
            LcpAuthenticating.AuthenticationReason.PassphraseNotFound -> {
                title.text = context.getString(
                    R.string.readium_lcp_dialog_reason_passphraseNotFound
                )
            }

            LcpAuthenticating.AuthenticationReason.InvalidPassphrase -> {
                title.text = context.getString(R.string.readium_lcp_dialog_reason_invalidPassphrase)
                passwordLayout.error = context.getString(
                    R.string.readium_lcp_dialog_reason_invalidPassphrase
                )
            }
        }

        val provider = tryOr(suspendedCall.license.provider) {
            Uri.parse(suspendedCall.license.provider).host
        }
        description.text = context.getString(R.string.readium_lcp_dialog_prompt, provider)

        hint.text = suspendedCall.license.hint

        val popupWindow = PopupWindow(
            dialogView,
            ListPopupWindow.MATCH_PARENT,
            ListPopupWindow.MATCH_PARENT
        ).apply {
            isOutsideTouchable = false
            isFocusable = true
            elevation = 5.0f
        }

        cancelButton.setOnClickListener {
            popupWindow.dismiss()
            terminateCall()
            suspendedCall.continuation.resume(null)
        }

        confirmButton.setOnClickListener {
            popupWindow.dismiss()
            terminateCall()
            suspendedCall.continuation.resume(password.text.toString())
        }

        forgotButton.setOnClickListener {
            suspendedCall.license.hintLink?.let { context.startActivityForLink(it) }
        }

        helpButton.setOnClickListener {
            showHelpDialog(context, suspendedCall.license.supportLinks)
        }

        popupWindow.showAtLocation(hostView, Gravity.CENTER, 0, 0)
    }

    private fun showHelpDialog(context: Context, links: List<Link>) {
        val titles = links.map {
            it.title ?: tryOr(context.getString(R.string.readium_lcp_dialog_support)) {
                when ((it.url() as? AbsoluteUrl)?.scheme?.value) {
                    "http", "https" -> context.getString(R.string.readium_lcp_dialog_support_web)
                    "tel" -> context.getString(R.string.readium_lcp_dialog_support_phone)
                    "mailto" -> context.getString(R.string.readium_lcp_dialog_support_mail)
                    else -> context.getString(R.string.readium_lcp_dialog_support)
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
        val url = tryOrNull { (link.url() as? AbsoluteUrl) } ?: return

        val action = when (url.scheme.value) {
            "http", "https" -> Intent(Intent.ACTION_VIEW)
            "tel" -> Intent(Intent.ACTION_CALL)
            "mailto" -> Intent(Intent.ACTION_SEND)
            else -> Intent(Intent.ACTION_VIEW)
        }

        startActivity(
            Intent(action).apply {
                data = url.toUri()
            }
        )
    }
}
