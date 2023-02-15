package org.readium.r2.testapp.reader.tts

import android.app.Application
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.readium.r2.navigator.media3.tts.AndroidTtsNavigator
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Enables to try to close a session without starting the [TtsService] if it is not started.
 */
@OptIn(ExperimentalReadiumApi::class)
class TtsServiceFacade(
    private val application: Application
) {

    private val mutex = Mutex()

    private var binder: TtsService.Binder? = null

    fun sessionNow(): TtsService.Session? =
        binder?.session

    suspend fun getSession(): TtsService.Session? = mutex.withLock {
        binder?.session
    }

    suspend fun openSession(
        bookId: Long,
        navigator: AndroidTtsNavigator
    ): TtsService.Session = mutex.withLock {
        try {
            if (binder == null) {
                TtsService.start(application)
                binder = TtsService.bind(application)
            }

            binder!!.openSession(navigator, bookId)
        } catch (e: CancellationException) {
            TtsService.stop(application)
            throw e
        }
    }

    suspend fun closeSession() = mutex.withLock {
        binder?.closeSession()
        binder = null
        TtsService.stop(application)
    }
}
