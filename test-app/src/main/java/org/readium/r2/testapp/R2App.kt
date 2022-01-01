/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp

import android.annotation.SuppressLint
import android.app.Application
import android.content.*
import android.os.IBinder
import androidx.media2.session.SessionToken
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.readium.r2.navigator.ExperimentalAudiobook
import org.readium.r2.streamer.server.Server
import org.readium.r2.testapp.BuildConfig.DEBUG
import timber.log.Timber
import java.io.IOException
import java.net.ServerSocket
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class R2App : Application() {

    val sessionToken: SessionToken by lazy {
        val context = this.applicationContext
        SessionToken(context, ComponentName(context as Application, MediaService::class.java))
    }

    val mediaServiceBinder: Deferred<MediaService.Binder>
        get() = _mediaServiceBinder

    private val _mediaServiceBinder: CompletableDeferred<MediaService.Binder> =
        CompletableDeferred()

    private val mediaServiceConnection = object: ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder) {
            _mediaServiceBinder.complete(service as MediaService.Binder)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            // Should not happen, do nothing.
        }

        override fun onNullBinding(name: ComponentName) {
           // Should not happen, do nothing.
        }
    }

    @OptIn(ExperimentalAudiobook::class)
    override fun onCreate() {
        super.onCreate()
        if (DEBUG) Timber.plant(Timber.DebugTree())
        val s = ServerSocket(if (DEBUG) 8080 else 0)
        s.close()
        server = Server(s.localPort, applicationContext)
        startServer()
        R2DIRECTORY = r2Directory
        val context = this.applicationContext
        startService(Intent(context, MediaService::class.java))
        bindService(Intent(context, MediaService::class.java), mediaServiceConnection, 0)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var server: Server
            private set

        lateinit var R2DIRECTORY: String
            private set

        var isServerStarted = false
            private set
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

                isServerStarted = true
            }
        }
    }

    private fun stopServer() {
        if (server.isAlive) {
            server.stop()
            isServerStarted = false
        }
    }

    private val r2Directory: String
        get() {
            val properties = Properties()
            val inputStream = applicationContext.assets.open("configs/config.properties")
            properties.load(inputStream)
            val useExternalFileDir =
                properties.getProperty("useExternalFileDir", "false")!!.toBoolean()
            return if (useExternalFileDir) {
                applicationContext.getExternalFilesDir(null)?.path + "/"
            } else {
                applicationContext.filesDir?.path + "/"
            }
        }
}

val Context.resolver: ContentResolver
    get() = applicationContext.contentResolver
