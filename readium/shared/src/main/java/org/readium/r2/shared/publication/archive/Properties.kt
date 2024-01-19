/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.publication.archive

import org.readium.r2.shared.publication.Properties

public data class ArchiveProperties(
    val entryLength: Long,
    val isEntryCompressed: Boolean
)

@Deprecated("Not used anymore", level = DeprecationLevel.ERROR)
public val Properties.archive: ArchiveProperties?
    get() = throw NotImplementedError()
