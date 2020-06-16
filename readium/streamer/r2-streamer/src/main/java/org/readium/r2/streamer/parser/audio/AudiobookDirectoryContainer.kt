/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Irteza Sheikh
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.audio

import org.readium.r2.shared.format.MediaType
import org.readium.r2.streamer.container.Container
import org.readium.r2.streamer.container.DirectoryContainer

interface AudioBookContainer : Container

class AudioBookDirectoryContainer(path: String) : AudioBookContainer, DirectoryContainer(path, MediaType.READIUM_AUDIOBOOK.toString())