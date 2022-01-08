/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp

import android.content.*
import android.os.IBinder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.readium.r2.navigator.ExperimentalAudiobook
import org.readium.r2.streamer.server.Server
import org.readium.r2.testapp.BuildConfig.DEBUG
import org.readium.r2.testapp.reader.PublicationRepository
import timber.log.Timber
import java.io.IOException
import java.net.ServerSocket
import java.util.*

class Application : android.app.Application() {

    lateinit var r2Directory: String
        private set

    lateinit var server: Server
        private set

    lateinit var publicationRepository: PublicationRepository
        private set

    private val mediaServiceBinder: CompletableDeferred<MediaService.Binder> =
        CompletableDeferred()

    private val mediaServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder) {
            Timber.d("MediaService bound.")
            mediaServiceBinder.complete(service as MediaService.Binder)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Timber.d("MediaService disconnected.")
            // Should not happen, do nothing.
        }

        override fun onNullBinding(name: ComponentName) {
            Timber.d("Failed to bind to MediaService.")
            // Should not happen, do nothing.
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (DEBUG) Timber.plant(Timber.DebugTree())

        r2Directory = computeAppDirectory()

        /*
         * Starting HTTP server.
         */

        val s = ServerSocket(if (DEBUG) 8080 else 0)
        s.close()
        server = Server(s.localPort, applicationContext)
        startServer()

        /*
         * Starting media service.
         */

        // MediaSessionService.onBind requires the intent to have a non-null action.
        val intent = Intent(MediaService.SERVICE_INTERFACE)
            .apply { setClass(applicationContext, MediaService::class.java) }
        startService(intent)
        bindService(intent, mediaServiceConnection, 0)

        publicationRepository =
            PublicationRepository.create(this, server, mediaServiceBinder)
    }

    override fun onTerminate() {
        super.onTerminate()
        stopServer()
    }

    private fun startServer() {
        if (!server.isAlive) {
            try {
                server.start()
            } catch (e: IOException) {
                // do nothing
                if (DEBUG) Timber.e(e)
            }
            if (server.isAlive) {
//                // Add your own resources here
//                server.loadCustomResource(assets.open("scripts/test.js"), "test.js")
//                server.loadCustomResource(assets.open("styles/test.css"), "test.css")
//                server.loadCustomFont(assets.open("fonts/test.otf"), applicationContext, "test.otf")
            }
        }
    }

    private fun stopServer() {
        if (server.isAlive) {
            server.stop()
        }
    }

    private fun computeAppDirectory(): String {
        val properties = Properties()
        val inputStream = assets.open("configs/config.properties")
        properties.load(inputStream)
        val useExternalFileDir =
            properties.getProperty("useExternalFileDir", "false")!!.toBoolean()
        return if (useExternalFileDir) {
            getExternalFilesDir(null)?.path + "/"
        } else {
            filesDir?.path + "/"
        }
    }
}


val Context.resolver: ContentResolver
    get() = applicationContext.contentResolver
