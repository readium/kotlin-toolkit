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
import org.readium.r2.navigator.media2.MediaNavigator
import org.readium.r2.navigator.media2.MediaSessionNavigator
import org.readium.r2.shared.util.Try
import org.readium.r2.testapp.bookshelf.BookRepository
import org.readium.r2.testapp.db.BookDatabase
import org.readium.r2.testapp.reader.ReaderContract
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

        private var currentBookId: Long? = null

        private var saveLocationJob: Job? = null

        var mediaSession: MediaSession? = null

        var mediaNavigator: MediaSessionNavigator? = null

        suspend fun openPublication(arguments: ReaderContract.Input): Try<Unit, MediaNavigator.Exception> {
            closePublication()
            return MediaSessionNavigator.create(
                this@MediaService,
                arguments.publication,
                arguments.initialLocator
            ).map {
                bindNavigator(it, arguments)
            }
        }

        fun closePublication() {
            mediaSession?.close()
            mediaSession = null
            saveLocationJob?.cancel()
            saveLocationJob = null
            mediaNavigator?.close()
            mediaNavigator = null
            currentBookId = null
        }

        @OptIn(FlowPreview::class)
        private fun bindNavigator(navigator: MediaSessionNavigator, arguments: ReaderContract.Input) {
            val activityIntent = createSessionActivityIntent(arguments)
            mediaSession = navigator.session(applicationContext, arguments.bookId.toString(), activityIntent)
                .also { addSession(it) }
            mediaNavigator = navigator
            saveLocationJob = navigator.currentLocator
                .buffer(1, BufferOverflow.DROP_OLDEST)
                .onEach {  locator ->
                    delay(3.seconds)
                    currentBookId?.let { id -> books.saveProgression(locator, id) }
                }
                .launchIn(lifecycleScope)
            currentBookId = arguments.bookId
        }

        private fun createSessionActivityIntent(input: ReaderContract.Input): PendingIntent {
            var flags = PendingIntent.FLAG_UPDATE_CURRENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags = flags or PendingIntent.FLAG_IMMUTABLE
            }

            val intent =
                ReaderContract().createIntent(applicationContext, input.copy(initialLocator = null))
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
        binder.closePublication()
        stopSelf()
    }

    companion object {
        const val SERVICE_INTERFACE = "org.readium.r2.testapp.MediaService"
    }
}