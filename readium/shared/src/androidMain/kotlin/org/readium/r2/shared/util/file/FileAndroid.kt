/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.file

import java.io.File
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.toUrl

/**
 * Creates a [FileResource] to access the given [file].
 */
public fun FileResource(file: File): FileResource =
    FileResource(file.toUrl(isDirectory = false))

/**
 * Creates a [DirectoryContainer] serving the files of the directory at [root].
 */
public suspend fun DirectoryContainer(root: File): Try<DirectoryContainer, FileSystemError> =
    DirectoryContainer(root.toUrl(isDirectory = true))
