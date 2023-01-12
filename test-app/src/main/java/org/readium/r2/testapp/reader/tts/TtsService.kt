/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader.tts

import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.testapp.reader.ReaderActivityContract
import org.readium.r2.testapp.utils.LifecycleMedia3SessionService
import timber.log.Timber

@OptIn(ExperimentalReadiumApi::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class TtsService : LifecycleMedia3SessionService() {

    class Session(
        val bookId: Long,
        val navigator: AndroidTtsNavigator,
        val mediaSession: MediaSession,
    ) {
        val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }

    /**
     * The service interface to be used by the app.
     */
    inner class Binder : android.os.Binder() {

        private val app: org.readium.r2.testapp.Application
            get() = application as org.readium.r2.testapp.Application

        var session: Session? = null

        fun closeSession() {
            stopForeground(true)
            session?.mediaSession?.release()
            session?.navigator?.close()
            session?.coroutineScope?.cancel()
            session = null
        }

        fun openSession(
            navigator: AndroidTtsNavigator,
            bookId: Long
        ): Session {
            val activityIntent = createSessionActivityIntent(bookId)
            val mediaSession = MediaSession.Builder(applicationContext, navigator.asPlayer())
                .setSessionActivity(activityIntent)
                .setId(bookId.toString())
                .build()

            addSession(mediaSession)

            val session = Session(
                bookId,
                navigator,
                mediaSession
            )

            this@Binder.session = session

            /*
             * Launch a job for saving progression even when playback is going on in the background
             * with no ReaderActivity opened.
             */
            navigator.currentLocator
                .sample(3000)
                .onEach { locator ->
                    Timber.d("Saving TTS progression $locator")
                    app.bookRepository.saveProgression(locator, bookId)
                }.launchIn(session.coroutineScope)

            return session
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
            /*intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)*/

            return PendingIntent.getActivity(applicationContext, 0, intent, flags)
        }
    }

    private val binder by lazy {
        Binder()
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("TtsService created.")
        // val initialNotification = createInitialNotification()
        // startForeground(1, initialNotification)
    }

    private fun createInitialNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannelId = createNotificationChannel()
            Notification.Builder(this, notificationChannelId)
                .setContentTitle("R2 testapp")
                .setContentText("rgergergergg")
                .setAutoCancel(true)
                .build()
        } else {
            NotificationCompat.Builder(this)
                .setContentTitle("R2 testapp")
                .setContentText("grgrgrgrg")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val notificationChannelId = "example.permanence"
        val channelName = "Background Service"
        val channel = NotificationChannel(notificationChannelId, channelName, NotificationManager.IMPORTANCE_NONE)
        channel.lightColor = Color.BLUE
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
        return notificationChannelId
    }

    override fun onBind(intent: Intent?): IBinder? {
        Timber.d("onBind called with $intent")

        return if (intent?.action == SERVICE_INTERFACE) {
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
        return binder.session?.mediaSession
    }

    override fun onUpdateNotification(session: MediaSession) {
        Timber.d("onUpdateNotification")
        super.onUpdateNotification(session)
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("MediaService destroyed.")
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        Timber.d("Task removed. Stopping session and service.")
        // Close the navigator to allow the service to be stopped.
        binder.closeSession()
        stopSelf()
    }

    companion object {

        const val SERVICE_INTERFACE = "org.readium.r2.testapp.reader.tts.TtsService"

        fun start(application: Application) {
            val intent = intent(application)
            ContextCompat.startForegroundService(application, intent)
        }

        suspend fun bind(application: Application): TtsService.Binder {
            val mediaServiceBinder: CompletableDeferred<TtsService.Binder> =
                CompletableDeferred()

            val mediaServiceConnection = object : ServiceConnection {

                override fun onServiceConnected(name: ComponentName?, service: IBinder) {
                    Timber.d("MediaService bound.")
                    mediaServiceBinder.complete(service as Binder)
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    Timber.d("MediaService disconnected.")
                    // Should not happen, do nothing.
                }

                override fun onNullBinding(name: ComponentName) {
                    val errorMessage = "Failed to bind to MediaService."
                    Timber.e(errorMessage)
                    val exception = IllegalStateException(errorMessage)
                    mediaServiceBinder.completeExceptionally(exception)
                }
            }

            val intent = intent(application)
            application.bindService(intent, mediaServiceConnection, 0)

            return mediaServiceBinder.await()
        }

        fun stop(application: Application) {
            val intent = intent(application)
            application.stopService(intent)
        }

        private fun intent(application: Application) =
            Intent(SERVICE_INTERFACE)
                // MediaSessionService.onBind requires the intent to have a non-null action
                .apply { setClass(application, TtsService::class.java) }
    }
}
