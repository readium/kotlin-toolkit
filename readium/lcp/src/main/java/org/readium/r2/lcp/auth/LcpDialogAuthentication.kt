/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.auth

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
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
import org.readium.r2.shared.extensions.tryOr
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.toUri

/**
 * An [LcpAuthenticating] implementation presenting a dialog to the user.
 *
 * This authentication requires a view to anchor on. You can pass it at construction time if you
 * already have it or call onParentViewCreated later. Anyway, you'll need to follow
 * the view lifecycle calling onDestroyView when it gets destroyed and
 * onParentViewCreated every time it gets created again.
 */
public class LcpDialogAuthentication(
    private var parentView: View?
) : LcpAuthenticating {

    private class SuspendedCall(
        val continuation: Continuation<String?>,
        val license: LcpAuthenticating.AuthenticatedLicense,
        val reason: LcpAuthenticating.AuthenticationReason,
        var currentInput: Editable? = null
    )

    private val mutex: Mutex = Mutex()

    private var suspendedCall: SuspendedCall? = null

    private val handler: Handler = Handler(Looper.getMainLooper())

    /**
     * Call this method every time the anchor view has just been created, for instance when
     * the method onViewCreated of a fragment is called.
     */
    public fun onParentViewCreated(parentView: View) {
        this.parentView = parentView
        // Calling showPopupWindow immediately raises an exception due to the window
        // having a null token
        handler.post {
            suspendedCall
                ?.let {
                    showPopupWindow(
                        it.continuation,
                        it.license,
                        it.reason,
                        parentView,
                        it.currentInput
                    )
                }
        }
    }

    /**
     * Call this method every time the anchor view is about to be destroyed, for instance when the
     * method of the same name of a fragment is called.
     */
    public fun onDestroyView() {
        this.parentView = null
    }

    override suspend fun retrievePassphrase(
        license: LcpAuthenticating.AuthenticatedLicense,
        reason: LcpAuthenticating.AuthenticationReason,
        allowUserInteraction: Boolean
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
        reason: LcpAuthenticating.AuthenticationReason
    ): String? {
        mutex.lock()

        return suspendCoroutine { cont ->
            suspendedCall = SuspendedCall(cont, license, reason)
            parentView?.let { showPopupWindow(cont, license, reason, it) }
        }
    }

    private fun terminateCall() {
        suspendedCall = null
        mutex.unlock()
    }

    private fun showPopupWindow(
        continuation: Continuation<String?>,
        license: LcpAuthenticating.AuthenticatedLicense,
        reason: LcpAuthenticating.AuthenticationReason,
        hostView: View,
        currentInput: Editable? = null
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

        password.text = currentInput
        password.addTextChangedListener { suspendedCall?.currentInput = it }

        forgotButton.isVisible = license.hintLink != null
        helpButton.isVisible = license.supportLinks.isNotEmpty()

        when (reason) {
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

        val provider = tryOr(license.provider) { Uri.parse(license.provider).host }
        description.text = context.getString(R.string.readium_lcp_dialog_prompt, provider)

        hint.text = license.hint

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
            continuation.resume(null)
        }

        confirmButton.setOnClickListener {
            popupWindow.dismiss()
            terminateCall()
            continuation.resume(password.text.toString())
        }

        forgotButton.setOnClickListener {
            license.hintLink?.let { context.startActivityForLink(it) }
        }

        helpButton.setOnClickListener {
            showHelpDialog(context, license.supportLinks)
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
