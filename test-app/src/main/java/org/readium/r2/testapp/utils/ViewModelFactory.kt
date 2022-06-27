/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import timber.log.Timber

/**
 * Creates a [ViewModelProvider.Factory] for a single type of [ViewModel] using the result of the
 * given [factory] closure.
 */
inline fun <reified T : ViewModel> createViewModelFactory(crossinline factory: () -> T): ViewModelProvider.Factory =

    object : ViewModelProvider.Factory {
        override fun <V : ViewModel> create(modelClass: Class<V>): V {
            if (!modelClass.isAssignableFrom(T::class.java)) {
                throw IllegalAccessException("Unknown ViewModel class")
            }
            @Suppress("UNCHECKED_CAST")
            return factory() as V
        }
    }

/**
 * A [ViewModelProvider.Factory] which will iterate over a provided list of [factories] until
 * finding one instantiating successfully the requested [ViewModel].
 */
class CompositeViewModelFactory(private vararg val factories: ViewModelProvider.Factory) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        for (factory in factories) {
            try {
                return factory.create(modelClass)
            } catch (e: IllegalAccessException) {
                // Ignored, because the factory didn't handle this model class.
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

        throw IllegalAccessException("Unknown ViewModel class")
    }
}
