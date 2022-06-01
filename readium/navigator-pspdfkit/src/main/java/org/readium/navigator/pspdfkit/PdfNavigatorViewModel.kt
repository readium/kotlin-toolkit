/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.pspdfkit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.util.createViewModelFactory
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.pdf.PdfDocumentFactory

@PdfSupport
internal class PdfNavigatorViewModel(
    application: Application,
    private val publication: Publication,
    initialLocator: Locator?,
    private val documentFactory: PdfDocumentFactory
) : AndroidViewModel(application) {

//    sealed class State {
//        data class Loading(val locator: Locator) : State()
//    }
//
//    val state: StateFlow<State> get() = _currentState
//    private val _currentState = MutableStateFlow(State.Loading(initialLocator))

    companion object {
        @OptIn(InternalReadiumApi::class)
        fun createFactory(
            application: Application,
            publication: Publication,
            initialLocator: Locator?,
            documentFactory: PdfDocumentFactory
        ) = createViewModelFactory {
            PdfNavigatorViewModel(application, publication, initialLocator, documentFactory)
        }
    }
}