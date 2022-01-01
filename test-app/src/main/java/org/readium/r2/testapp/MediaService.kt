package org.readium.r2.testapp

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.lifecycleScope
import androidx.media2.session.MediaSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.readium.r2.navigator.ExperimentalAudiobook
import org.readium.r2.navigator.media2.PublicationPlayerFactory
import org.readium.r2.navigator.media2.locatorNow
import org.readium.r2.testapp.bookshelf.BookRepository
import org.readium.r2.testapp.db.BookDatabase
import org.readium.r2.testapp.reader.ReaderContract
import org.readium.r2.testapp.utils.LifecycleMediaSessionService
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@ExperimentalAudiobook
@ExperimentalTime
class MediaService : LifecycleMediaSessionService() {

    inner class Binder : android.os.Binder() {
        fun open(input: ReaderContract.Input): Boolean {
            val player = playerFactory.open(applicationContext, input.publication, input.initialLocator)
                ?: run {
                    Timber.e("Publication not supported by any engine.")
                    return false
                }

            when (val currentSession = mediaSession) {
                null -> {
                    val activityIntent = createSessionActivityIntent(input)
                    Timber.d("Creating MediaSession for book ${input.bookId}.")
                    val session = MediaSession.Builder(applicationContext, player)
                        .setSessionActivity(activityIntent)
                        .build()
                    mediaSession = session
                    currentBookId = input.bookId
                }
                else -> {
                    Timber.d("Updating MediaSession for book ${input.bookId}.")
                    currentSession.player.close()
                    currentSession.updatePlayer(player)
                    currentBookId = input.bookId
                }
            }

            return true
        }

        private fun createSessionActivityIntent(input: ReaderContract.Input): PendingIntent {
            var flags = PendingIntent.FLAG_UPDATE_CURRENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags = flags or PendingIntent.FLAG_IMMUTABLE
            }

            val intent = ReaderContract().createIntent(applicationContext, input.copy(initialLocator = null))
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            return PendingIntent.getActivity(applicationContext, 0, intent, flags)
        }
    }

    private val binder by lazy {
      Binder()
    }

    private val books by lazy {
        BookRepository(BookDatabase.getDatabase(this).booksDao())
    }

    private val playerFactory = PublicationPlayerFactory()

    private var mediaSession: MediaSession? = null

    private var currentBookId: Long? = null

    override fun onCreate() {
        super.onCreate()
        Timber.d("MediaService created.")

        // Save the current locator in the database. We can't do this in the [ReaderActivity] since
        // the playback can continue in the background without any [Activity].
        lifecycleScope.launch {
            while (isActive) {
                saveLocationIfNeeded()
                delay(3.seconds)
            }
        }
    }

    private suspend fun saveLocationIfNeeded() {
        val currentBookIdNow = currentBookId ?: return
        val locatorNow =  mediaSession?.player?.locatorNow() ?: return
        books.saveProgression(locatorNow, currentBookIdNow)
    }

    override fun onBind(intent: Intent): IBinder? {
        Timber.d("onBind called with $intent")
        return if (intent.action == null) {
            Timber.d("Returning custom binder.")
            binder
        } else {
            Timber.d("Returning MediaSessionService binder.")
            super.onBind(intent)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("MediaService destroyed.")
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        Timber.d("Task removed. Stopping session and service.")
        mediaSession?.player?.close()
        mediaSession?.close()
        stopSelf()
    }

    companion object {

        const val SERVICE_INTERFACE = "org.readium.r2.testapp.MediaService"
    }
}