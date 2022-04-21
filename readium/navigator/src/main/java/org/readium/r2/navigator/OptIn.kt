/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator

@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "Support for the Decorator API is still experimental. The API may be changed in the future without notice."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.TYPEALIAS, AnnotationTarget.PROPERTY)
annotation class ExperimentalDecorator

@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "The new Audiobook navigator is still experimental. The API may be changed in the future without notice."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.TYPEALIAS, AnnotationTarget.PROPERTY)
annotation class ExperimentalAudiobook

@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "The new dragging gesture is still experimental. The API may be changed in the future without notice."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.TYPEALIAS, AnnotationTarget.PROPERTY)
annotation class ExperimentalDragGesture
