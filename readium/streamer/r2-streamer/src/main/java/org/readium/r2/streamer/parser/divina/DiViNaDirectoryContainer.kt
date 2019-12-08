/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.parser.divina

import org.readium.r2.streamer.container.Container
import org.readium.r2.streamer.container.DirectoryContainer

interface DiViNaContainer: Container {

}


class ContainerDiViNa(path: String) : DiViNaContainer, DirectoryContainer(path, DiViNaConstant.mimetype) {

}