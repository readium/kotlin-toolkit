/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp

import android.content.Context
import org.readium.adapters.pdfium.document.PdfiumDocumentFactory
import org.readium.r2.lcp.LcpService
import org.readium.r2.shared.util.Try
import org.readium.r2.streamer.Streamer
import org.readium.r2.streamer.server.Server
import java.net.ServerSocket

/**
 * Holds the shared Readium objects and services used by the app.
 */
class Readium(context: Context) {
    private val context = context.applicationContext

    fun onAppStart() {
        startServer()
    }

    fun onAppTerminate() {
        stopServer()
    }

    /**
     * The LCP service decrypts LCP-protected publication and acquire publications from a
     * license file.
     */
    val lcpService = LcpService(context)
        ?.let { Try.success(it) }
        ?: Try.failure(Exception("liblcp is missing on the classpath"))

    /**
     * The Streamer is used to open and parse publications.
     */
    val streamer = Streamer(
        context,
        contentProtections = listOfNotNull(
            lcpService.getOrNull()?.contentProtection()
        ),
        // Only required if you want to support PDF files using the PDFium adapter.
        pdfFactory = PdfiumDocumentFactory(context)
    )

    /**
     * HTTP server.
     *
     * This local server is required to render EPUB publications with the EPUBNavigatorFragment.
     */
    var server: Server? = null
        private set

    private fun startServer() {
        try {
            val socket = ServerSocket(if (BuildConfig.DEBUG) 8080 else 0)
            socket.close()

            server = Server(socket.localPort, context).apply {
                start()

                // Add your own resources you want to server here.
//                loadCustomResource(assets.open("scripts/test.js"), "test.js")
//                loadCustomResource(assets.open("styles/test.css"), "test.css")
//                loadCustomFont(assets.open("fonts/test.otf"), applicationContext, "test.otf")
            }
        } catch (e: Exception) {
            throw Exception("Failed to start the HTTP server", e)
        }
    }

    private fun stopServer() {
        server?.run {
            if (isAlive) {
                stop()
            }
        }
    }
}