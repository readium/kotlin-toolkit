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

// Android-only aliases for the remaining kotlinx.parcelize APIs used by shared. The files using
// them are still in androidMain; they will grow common equivalents when those files move.

public typealias Parceler<T> = kotlinx.parcelize.Parceler<T>

public typealias TypeParceler<T, P> = kotlinx.parcelize.TypeParceler<T, P>

public typealias WriteWith<P> = kotlinx.parcelize.WriteWith<P>
