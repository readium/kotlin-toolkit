/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.resource

import java.io.FilterInputStream
import org.readium.r2.shared.util.datasource.DataSourceInputStream

/**
 * Input stream reading a [Resource]'s content.
 *
 * If you experience bad performances, consider wrapping the stream in a BufferedInputStream. This
 * is particularly useful when streaming deflated ZIP entries.
 *
 * Raises [ResourceException]s when [ResourceError]s occur.
 */
public class ResourceInputStream private constructor(
    dataSourceInputStream: DataSourceInputStream<ResourceError>
) : FilterInputStream(dataSourceInputStream) {

    public constructor(resource: Resource, range: LongRange? = null) :
        this(DataSourceInputStream(resource.asDataSource(), ::ResourceException, range))
}
