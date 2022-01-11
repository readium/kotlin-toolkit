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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import org.readium.r2.navigator.ExperimentalAudiobook
import org.readium.r2.navigator.media2.MediaSessionNavigator
import org.readium.r2.testapp.bookshelf.BookRepository
import org.readium.r2.testapp.db.BookDatabase
import org.readium.r2.testapp.reader.NavigatorType
import org.readium.r2.testapp.reader.ReaderActivityContract
import org.readium.r2.testapp.utils.LifecycleMediaSessionService
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalAudiobook::class, ExperimentalCoroutinesApi::class)
class MediaService : LifecycleMediaSessionService() {

    inner class Binder : android.os.Binder() {

        private val books by lazy {
            BookRepository(BookDatabase.getDatabase(this@MediaService).booksDao())
        }

        private var saveLocationJob: Job? = null

        private var mediaNavigator: MediaSessionNavigator? = null

        var mediaSession: MediaSession? = null

        fun unbindNavigator() {
            mediaSession?.close()
            mediaSession = null
            saveLocationJob?.cancel()
            saveLocationJob = null
            mediaNavigator = null
        }

        fun unbindAndCloseNavigator() {
            mediaSession?.close()
            mediaSession = null
            saveLocationJob?.cancel()
            saveLocationJob = null
            mediaNavigator?.close()
            mediaNavigator?.publication?.close()
            mediaNavigator = null
        }

        @OptIn(FlowPreview::class)
        fun bindNavigator(navigator: MediaSessionNavigator, bookId: Long) {
            val activityIntent = createSessionActivityIntent()
            mediaNavigator = navigator
            mediaSession = navigator.session(applicationContext, bookId.toString(), activityIntent)
                .also { addSession(it) }
            saveLocationJob = navigator.currentLocator
                .buffer(1, BufferOverflow.DROP_OLDEST)
                .onEach {  locator ->
                    delay(3.seconds)
                    books.saveProgression(locator, bookId)
                }
                .launchIn(lifecycleScope)
        }

        private fun createSessionActivityIntent(): PendingIntent {
            var flags = PendingIntent.FLAG_UPDATE_CURRENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags = flags or PendingIntent.FLAG_IMMUTABLE
            }

            val intent =
                ReaderActivityContract().createIntent(
                    applicationContext,
                    NavigatorType.Media
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
            Timber.d("Returning custom binder.")
            binder
        } else {
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
        binder.unbindAndCloseNavigator()
        stopSelf()
    }

    companion object {
        const val SERVICE_INTERFACE = "org.readium.r2.testapp.MediaService"
    }
}