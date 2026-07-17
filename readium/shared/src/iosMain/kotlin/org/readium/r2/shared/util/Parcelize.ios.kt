/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public actual annotation class Parcelize

public actual interface Parcelable

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
public actual annotation class IgnoredOnParcel
