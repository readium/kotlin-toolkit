package org.readium.r2.testapp.reader.tts

import android.app.Application
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.readium.r2.shared.ExperimentalReadiumApi

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
        startService()
        val binderNow = checkNotNull(binder)
        binderNow.openSession(navigator, bookId)
    }

    private suspend fun startService() {
        if (binder != null)
            return

        TtsService.start(application)
        binder = TtsService.bind(application)
    }

    suspend fun closeSession() = mutex.withLock {
        binder?.closeSession()
        stopService()
    }

    private fun stopService() {
        if (binder == null)
            return

        binder = null
        TtsService.stop(application)
    }
}
