/*
 * Module: r2-streamer-kotlin
 * Developers: Quentin Gliosca, Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser

import android.net.Uri
import org.readium.r2.shared.publication.Publication
import org.readium.r2.streamer.container.Container
import java.io.File
import java.net.URI
import java.net.URLDecoder

data class PubBox(var publication: Publication, var container: Container)

interface PublicationParser {

    fun parse(fileAtPath: String, title: String = File(fileAtPath).name): PubBox?

}
