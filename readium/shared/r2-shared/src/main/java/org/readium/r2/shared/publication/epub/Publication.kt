/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication.epub

import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.Link

// EPUB extensions for [Publication].
// https://readium.org/webpub-manifest/schema/extensions/epub/subcollections.schema.json
// https://idpf.github.io/epub-vocabs/structure/#navigation

/**
 * Provides navigation to positions in the Publication content that correspond to the locations of
 * page boundaries present in a print source being represented by this EPUB Publication.
 */
val Publication.pageList: List<Link> get() = linksWithRole("pageList")

/**
 * Identifies fundamental structural components of the publication in order to enable Reading
 * Systems to provide the User efficient access to them.
 */
val Publication.landmarks: List<Link> get() = linksWithRole("landmarks")

val Publication.listOfAudioClips: List<Link> get() = linksWithRole("loa")
val Publication.listOfIllustrations: List<Link> get() = linksWithRole("loi")
val Publication.listOfTables: List<Link> get() = linksWithRole("lot")
val Publication.listOfVideoClips: List<Link> get() = linksWithRole("lov")
