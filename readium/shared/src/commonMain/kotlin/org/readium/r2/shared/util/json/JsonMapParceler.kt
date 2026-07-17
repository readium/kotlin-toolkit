/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.json

import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.util.Parceler

/**
 * Implementation of a [Parceler] to be used with `@Parcelize` to serialize maps of JSON values,
 * through their JSON string representation.
 */
@InternalReadiumApi
public expect object JsonMapParceler : Parceler<Map<String, Any>>
