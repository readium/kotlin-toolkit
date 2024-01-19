/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.units

@JvmInline
public value class Hz(public val value: Double)

public val Double.hz: Hz get() = Hz(this)
