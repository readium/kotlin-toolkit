/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.lifecycleScope
import androidx.media2.session.MediaSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import org.readium.navigator.media2.ExperimentalMedia2
import org.readium.navigator.media2.MediaNavigator
import org.readium.r2.testapp.bookshelf.BookRepository
import org.readium.r2.testapp.db.BookDatabase
import org.readium.r2.testapp.reader.ReaderActivityContract
import org.readium.r2.testapp.utils.LifecycleMediaSessionService
import timber.log.Timber
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalMedia2::class, ExperimentalCoroutinesApi::class)
class MediaService : LifecycleMediaSessionService() {

    /**
     * The service interface to be used by the app.
     */
    inner class Binder : android.os.Binder() {

        private val books by lazy {
            BookRepository(BookDatabase.getDatabase(this@MediaService).booksDao())
        }

        private var saveLocationJob: Job? = null

        private var mediaNavigator: MediaNavigator? = null

        var mediaSession: MediaSession? = null

        fun closeNavigator() {
            stopForeground(true)
            mediaSession?.close()
            mediaSession = null
            saveLocationJob?.cancel()
            saveLocationJob = null
            mediaNavigator?.close()
            mediaNavigator?.publication?.close()
            mediaNavigator = null
        }

        @OptIn(FlowPreview::class)
        fun bindNavigator(navigator: MediaNavigator, bookId: Long) {
            val activityIntent = createSessionActivityIntent(bookId)
            mediaNavigator = navigator
            mediaSession = navigator.session(applicationContext, activityIntent)
                .also { addSession(it) }

            /*
             * Launch a job for saving progression even when playback is going on in the background
             * with no ReaderActivity opened.
             */
            saveLocationJob = navigator.currentLocator
                .sample(3000)
                .onEach {  locator -> books.saveProgression(locator, bookId) }
                .launchIn(lifecycleScope)
        }

        private fun createSessionActivityIntent(bookId: Long): PendingIntent {
            // This intent will be triggered when the notification is clicked.
            var flags = PendingIntent.FLAG_UPDATE_CURRENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags = flags or PendingIntent.FLAG_IMMUTABLE
            }

            val intent =
                ReaderActivityContract().createIntent(
                    applicationContext,
                    ReaderActivityContract.Arguments(bookId)
                )
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            return PendingIntent.getActivity(applicationContext, 0, intent, flags)
        }
    }

    private val binder by lazy {
        Binder()
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("MediaService created.")
    }

    override fun onBind(intent: Intent): IBinder? {
        Timber.d("onBind called with $intent")

        return if (intent.action == SERVICE_INTERFACE) {
            super.onBind(intent)
            // Readium-aware client.
            Timber.d("Returning custom binder.")
            binder
        } else {
            // External controller.
            Timber.d("Returning MediaSessionService binder.")
            super.onBind(intent)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return binder.mediaSession
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("MediaService destroyed.")
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        Timber.d("Task removed. Stopping session and service.")
        // Close the navigator to allow the service to be stopped.
        binder.closeNavigator()
        stopSelf()
    }

    companion object {
        const val SERVICE_INTERFACE = "org.readium.r2.testapp.MediaService"
    }
}