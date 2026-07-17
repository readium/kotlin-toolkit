/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util

import kotlin.time.Instant
import org.readium.r2.shared.InternalReadiumApi

/**
 * Implementation of a [Parceler] to be used with `@Parcelize` to serialize [Instant] values.
 */
@InternalReadiumApi
public expect object InstantParceler : Parceler<Instant>
