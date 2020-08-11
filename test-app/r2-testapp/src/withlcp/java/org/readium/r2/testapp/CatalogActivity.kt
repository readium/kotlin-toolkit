/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ListPopupWindow
import android.widget.PopupWindow
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mcxiaoke.koi.ext.fileExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.readium.r2.lcp.*
import org.readium.r2.shared.drm.DRM
import org.readium.r2.shared.util.Try
import org.readium.r2.testapp.drm.DRMFulfilledPublication
import org.readium.r2.testapp.library.LibraryActivity
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CatalogActivity : LibraryActivity(), LcpAuthenticating {

    private lateinit var lcpService: LcpService

    override fun onCreate(savedInstanceState: Bundle?) {
            lcpService = LcpService.create(this)
        contentProtections = listOf(LcpContentProtection(lcpService, this))
        super.onCreate(savedInstanceState)
    }

    override val brand: DRM.Brand
        get() = DRM.Brand.lcp

    override fun canFulfill(file: String): Boolean =
            file.fileExtension().toLowerCase(Locale.ROOT) == "lcpl"

    override suspend fun fulfill(byteArray: ByteArray): Try<DRMFulfilledPublication, LcpException> =
        lcpService.importPublication(byteArray).map { DRMFulfilledPublication(it.localURL, it.suggestedFilename) }

    override suspend fun retrievePassphrase(license: LcpAuthenticating.AuthenticatedLicense, reason: LcpAuthenticating.AuthenticationReason, allowUserInteraction: Boolean, sender: Any?): String? =
        if (allowUserInteraction)
            withContext(Dispatchers.Main) { requestPassphrase(license, reason) }
        else null

    suspend fun requestPassphrase(license: LcpAuthenticating.AuthenticatedLicense, reason: LcpAuthenticating.AuthenticationReason): String? {
        // Initialize a new instance of LayoutInflater service
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        // Inflate the custom layout/view
        val customView = inflater.inflate(R.layout.popup_passphrase, null)

        // Initialize a new instance of popup window
        val mPopupWindow = PopupWindow(
            customView,
            ListPopupWindow.MATCH_PARENT,
            ListPopupWindow.MATCH_PARENT
        )
        mPopupWindow.isOutsideTouchable = false
        mPopupWindow.isFocusable = true

        // Set an elevation value for popup window
        // Call requires API level 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mPopupWindow.elevation = 5.0f
        }

        val title = customView.findViewById(R.id.title) as TextView
        val description = customView.findViewById(R.id.description) as TextView
        val hint = customView.findViewById(R.id.hint) as TextView
        val passwordLayout = customView.findViewById(R.id.passwordLayout) as TextInputLayout
        val password = customView.findViewById(R.id.password) as TextInputEditText
        val confirmButton = customView.findViewById(R.id.confirm_button) as Button
        val cancelButton = customView.findViewById(R.id.cancel_button) as Button
        val forgotButton = customView.findViewById(R.id.forgot_link) as Button
        val helpButton = customView.findViewById(R.id.help_link) as Button

        if (license.supportLinks.isEmpty()) {
            helpButton.visibility = View.GONE
        } else {
            helpButton.visibility = View.VISIBLE
        }

        when (reason.name) {
            "passphraseNotFound" -> title.text = "Passphrase Required"
            "invalidPassphrase" -> {
                title.text = "Incorrect Passphrase"
                passwordLayout.error = "Incorrect Passphrase"
            }
        }

        val provider = try {
            val test = URL(license.provider)
            URL(license.provider).host
        } catch (e: Exception) {
            license.provider
        }

        description.text =
            "This publication is protected by Readium LCP.\n\nIn order to open it, we need to know the passphrase required by: \n\n$provider.\n\nTo help you remember it, the following hint is available:"
        hint.text = license.hint

        // Finally, show the popup window at the center location of root relative layout
        return suspendCoroutine<String?> { cont ->

           // Set a click listener for the popup window close button
           cancelButton.setOnClickListener {
               // Dismiss the popup window
               mPopupWindow.dismiss()
               cont.resume(null)
           }

           confirmButton.setOnClickListener {
               mPopupWindow.dismiss()
               cont.resume(password.text.toString())
           }

           forgotButton.setOnClickListener {
               license.hintLink?.href?.let { href ->
                   val intent = Intent(Intent.ACTION_VIEW)
                   intent.data = Uri.parse(href)
                   startActivity(intent)
               }
           }

           helpButton.setOnClickListener {
                   alert(Appcompat) {
                       customView {
                           verticalLayout {
                               license.supportLinks.forEach { link ->
                                   button {
                                       link.title?.let {
                                           title.text = it
                                       } ?: run {
                                           title.text = try {
                                               when (URL(link.href).protocol) {
                                                   "http" -> "Website"
                                                   "https" -> "Website"
                                                   "tel" -> "Phone"
                                                   "mailto" -> "Mail"
                                                   else -> "Support"
                                               }
                                           } catch (e: Exception) {
                                               "Support"
                                           }
                                       }
                                       setOnClickListener {
                                           val intent = try {
                                               when (URL(link.href).protocol) {
                                                   "http" -> Intent(Intent.ACTION_VIEW)
                                                   "https" -> Intent(Intent.ACTION_VIEW)
                                                   "tel" -> Intent(Intent.ACTION_CALL)
                                                   "mailto" -> Intent(Intent.ACTION_SEND)
                                                   else -> Intent(Intent.ACTION_VIEW)
                                               }
                                           } catch (e: Exception) {
                                               Intent(Intent.ACTION_VIEW)
                                           }
                                           intent.data = Uri.parse(link.href)
                                           startActivity(intent)
                                       }
                                   }
                               }
                           }
                       }
                   }.build().show()
               }

           mPopupWindow.showAtLocation(contentView, Gravity.CENTER, 0, 0)

        }
    }
}
