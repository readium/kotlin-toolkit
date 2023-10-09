/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import android.app.Application
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.readium.navigator.media.common.Media3Adapter
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Enables to try to close a session without starting the [MediaService] if it is not started.
 */
@OptIn(ExperimentalReadiumApi::class)
class MediaServiceFacade(
    private val application: Application
) {
    private val coroutineScope: CoroutineScope =
        MainScope()

    private val mutex: Mutex =
        Mutex()

    private var binder: MediaService.Binder? =
        null

    private var bindingJob: Job? =
        null

    private val sessionMutable: MutableStateFlow<MediaService.Session?> =
        MutableStateFlow(null)

    val session: StateFlow<MediaService.Session?> =
        sessionMutable.asStateFlow()

    suspend fun <N> openSession(
        bookId: Long,
        navigator: N
    ) where N : AnyMediaNavigator, N : Media3Adapter = mutex.withLock {
        if (session.value != null) {
            throw CancellationException("A session is already running.")
        }

        try {
            if (binder == null) {
                MediaService.start(application)
                val binder = MediaService.bind(application)
                this.binder = binder
                bindingJob = binder.session
                    .onEach { sessionMutable.value = it }
                    .launchIn(coroutineScope)
            }

            binder!!.openSession(navigator, bookId)
        } catch (e: CancellationException) {
            MediaService.stop(application)
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
            MediaService.stop(application)
        }
    }
}
