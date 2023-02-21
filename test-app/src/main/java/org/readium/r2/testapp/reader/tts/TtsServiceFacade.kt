package org.readium.r2.testapp.reader.tts

import android.app.Application
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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
    private val coroutineScope: CoroutineScope =
        MainScope()

    private val mutex: Mutex =
        Mutex()

    private var binder: TtsService.Binder? =
        null

    private var bindingJob: Job? =
        null

    private val sessionMutable: MutableStateFlow<TtsService.Session?> =
        MutableStateFlow(null)

    val session: StateFlow<TtsService.Session?> =
        sessionMutable.asStateFlow()

    suspend fun openSession(
        bookId: Long,
        navigator: AndroidTtsNavigator
    ) = mutex.withLock {
        if (session.value != null) {
            throw CancellationException("A session is already running.")
        }

        try {
            if (binder == null) {
                TtsService.start(application)
                val binder = TtsService.bind(application)
                this.binder = binder
                bindingJob = binder.session
                    .onEach { sessionMutable.value = it }
                    .launchIn(coroutineScope)
            }

            binder!!.openSession(navigator, bookId)
        } catch (e: CancellationException) {
            TtsService.stop(application)
            throw e
        }
    }

    suspend fun closeSession() = mutex.withLock {
        if (session.value == null) {
            throw CancellationException("No session to close.")
        }

        withContext(NonCancellable) {
            bindingJob!!.cancelAndJoin()
            binder!!.closeSession()
            sessionMutable.value = null
            binder = null
            TtsService.stop(application)
        }
    }
}
