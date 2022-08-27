/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp

import android.content.*
import android.os.IBinder
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.*
import org.readium.r2.testapp.BuildConfig.DEBUG
import org.readium.r2.testapp.bookshelf.BookRepository
import org.readium.r2.testapp.db.BookDatabase
import org.readium.r2.testapp.reader.ReaderRepository
import timber.log.Timber
import java.io.File
import java.util.*

class Application : android.app.Application() {

    lateinit var readium: Readium
        private set

    lateinit var storageDir: File

    lateinit var bookRepository: BookRepository
        private set

    lateinit var readerRepository: Deferred<ReaderRepository>
        private set

    private val coroutineScope: CoroutineScope =
        MainScope()

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
        DynamicColors.applyToActivitiesIfAvailable(this)
        if (DEBUG) Timber.plant(Timber.DebugTree())

        readium = Readium(this)

        readium.onAppStart()

        storageDir = computeStorageDir()

        /*
         * Starting media service.
         */

        // MediaSessionService.onBind requires the intent to have a non-null action.
        val intent = Intent(MediaService.SERVICE_INTERFACE)
            .apply { setClass(applicationContext, MediaService::class.java) }
        startService(intent)
        bindService(intent, mediaServiceConnection, 0)


        /*
         * Initializing repositories
         */
        bookRepository =
            BookDatabase.getDatabase(this).booksDao()
                .let {  BookRepository(it) }

        readerRepository =
            coroutineScope.async {
                ReaderRepository(
                    this@Application,
                    readium,
                    mediaServiceBinder.await(),
                    bookRepository
                )
            }

    }

    override fun onTerminate() {
        super.onTerminate()
        readium.onAppTerminate()
    }

    private fun computeStorageDir(): File {
        val properties = Properties()
        val inputStream = assets.open("configs/config.properties")
        properties.load(inputStream)
        val useExternalFileDir =
            properties.getProperty("useExternalFileDir", "false")!!.toBoolean()

        return File(
            if (useExternalFileDir) getExternalFilesDir(null)?.path + "/"
            else filesDir?.path + "/"
        )
    }
}


val Context.resolver: ContentResolver
    get() = applicationContext.contentResolver
