/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

/**
 * Multiplatform stand-in for `kotlinx.parcelize.Parcelize`.
 *
 * On Android it generates the `Parcelable` implementation; on other platforms it is a no-op.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public expect annotation class Parcelize()

/**
 * Multiplatform stand-in for `android.os.Parcelable`.
 */
public expect interface Parcelable

/**
 * Multiplatform stand-in for `kotlinx.parcelize.IgnoredOnParcel`.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
public expect annotation class IgnoredOnParcel()
