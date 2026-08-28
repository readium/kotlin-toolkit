/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.format

import org.readium.r2.shared.util.FileExtension
import org.readium.r2.shared.util.mediatype.MediaType

public data class Format(
    public val specification: FormatSpecification,
    public val mediaType: MediaType,
    public val fileExtension: FileExtension,
) {
    public fun conformsTo(specification: Specification): Boolean =
        this.specification.conformsTo(specification)

    public fun conformsToAny(specifications: Collection<Specification>): Boolean =
        this.specification.conformsToAny(specifications)

    public fun conformsToAll(specifications: Collection<Specification>): Boolean =
        this.specification.conformsToAll(specifications)

    internal fun conformsTo(other: Format): Boolean =
        specification.conformsTo(other.specification)

    internal fun refines(other: Format) =
        specification != other.specification &&
            specification.conformsToAll(other.specification.specifications)
}

@JvmInline
public value class FormatSpecification(public val specifications: Set<Specification>) {

    public constructor(vararg specifications: Specification) : this(specifications.toSet())

    public operator fun plus(specification: Specification): FormatSpecification =
        FormatSpecification(specifications + specification)

    public operator fun plus(specifications: Collection<Specification>): FormatSpecification =
        FormatSpecification(this.specifications + specifications)

    public fun conformsTo(specification: Specification): Boolean =
        specification in specifications

    public fun conformsToAny(vararg specifications: Specification): Boolean =
        conformsToAny(*specifications)

    public fun conformsToAny(specifications: Collection<Specification>): Boolean =
        specifications.any { spec -> this.specifications.contains(spec) }

    public fun conformsToAll(specifications: Collection<Specification>): Boolean =
        this.specifications.containsAll(specifications)

    public fun conformsToAll(vararg specifications: Specification): Boolean =
        conformsToAll(*specifications)

    internal fun conformsTo(other: FormatSpecification): Boolean =
        specifications.containsAll(other.specifications)
}

public interface Specification {

    public companion object {}

    /*
     * Archive specifications
     */
    public object Zip : Specification
    public object Rar : Specification

    /*
     * Syntax specifications
     */
    public object Json : Specification
    public object Xml : Specification

    /*
     * Publication manifest specifications
     */
    public object W3cPubManifest : Specification
    public object Rwpm : Specification

    /*
     * Technical document specifications
     */
    public object ProblemDetails : Specification
    public object LcpLicense : Specification

    /*
     * Media format specifications
     */
    public object Pdf : Specification
    public object Html : Specification

    /*
     * Drm specifications
     */
    public object Lcp : Specification
    public object Adept : Specification

    /*
     * Bitmap specifications
     */
    public object Avif : Specification
    public object Bmp : Specification
    public object Gif : Specification
    public object Jpeg : Specification
    public object Jxl : Specification
    public object Png : Specification
    public object Tiff : Specification
    public object Webp : Specification

    /*
     * Audio specifications
     */
    public object Aac : Specification
    public object Aiff : Specification
    public object Flac : Specification
    public object Mp4 : Specification
    public object Mp3 : Specification
    public object Ogg : Specification
    public object Opus : Specification
    public object Wav : Specification
    public object Webm : Specification

    /*
     * Publication package specifications
     */
    public object Epub : Specification
    public object Rpf : Specification
    public object Lpf : Specification
    public object InformalAudiobook : Specification
    public object InformalComic : Specification

    /*
     * Opds specifications
     */
    public object Opds1Catalog : Specification
    public object Opds1Entry : Specification
    public object Opds2Catalog : Specification
    public object Opds2Publication : Specification
    public object OpdsAuthentication : Specification

    /*
     * Language specifications
     */

    public object JavaScript : Specification
    public object Css : Specification
}
