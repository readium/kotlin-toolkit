/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import org.readium.r2.shared.publication.Publication
import org.readium.r2.testapp.utils.EventChannel

class ReaderViewModel(context: Context, arguments: ReaderContract.Input) : ViewModel() {

    val publication: Publication = arguments.publication
    val persistence: BookData = BookData(context, arguments.bookId, publication)
    val channel = EventChannel(Channel<Event>(Channel.BUFFERED), viewModelScope)

    class Factory(private val context: Context, private val arguments: ReaderContract.Input)
        : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            modelClass.getDeclaredConstructor(Context::class.java, ReaderContract.Input::class.java)
                .newInstance(context.applicationContext, arguments)
    }

    sealed class Event {

        object OpenOutlineRequested : Event()

        object OpenDrmManagementRequested : Event()
    }
}

