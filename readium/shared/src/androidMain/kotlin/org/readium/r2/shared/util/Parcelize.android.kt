/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

// Not a typealias to `kotlinx.parcelize.Parcelize`: the Parcelize compiler plugin is pointed at
// this annotation's FQN through its `additionalAnnotation` option (see
// readium.multiplatform-conventions.gradle.kts). Aliasing the real `Parcelize` on top of that
// makes the plugin see the same annotation under two names and fail with
// "Resolution of the annotation type is ambiguous".
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public actual annotation class Parcelize

public actual typealias Parcelable = android.os.Parcelable

public actual typealias IgnoredOnParcel = kotlinx.parcelize.IgnoredOnParcel

public actual typealias Parceler<T> = kotlinx.parcelize.Parceler<T>

public actual typealias TypeParceler<T, P> = kotlinx.parcelize.TypeParceler<T, P>

public actual typealias WriteWith<P> = kotlinx.parcelize.WriteWith<P>
