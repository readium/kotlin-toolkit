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
    public val fileExtension: FileExtension
) {
    public fun conformsTo(specification: Specification): Boolean =
        this.specification.conformsTo(specification)

    public fun conformsToAny(specifications: Collection<Specification>): Boolean =
        this.specification.conformsToAny(specifications)

    public fun conformsToAll(specifications: Collection<Specification>): Boolean =
        this.specification.conformsToAll(specifications)

    internal fun conformsTo(other: Format): Boolean =
        specification.conformsTo(other.specification)
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

public interface Specification

/*
 * Archive specifications
 */
public object ZipSpecification : Specification
public object RarSpecification : Specification

/*
 * Syntax specifications
 */
public object JsonSpecification : Specification
public object XmlSpecification : Specification

/*
 * Publication manifest specifications
 */
public object W3cPubManifestSpecification : Specification
public object RwpmSpecification : Specification

/*
 * Technical document specifications
 */
public object ProblemDetailsSpecification : Specification
public object LcpLicenseSpecification : Specification

/*
 * Media format specifications
 */
public object PdfSpecification : Specification
public object HtmlSpecification : Specification

/*
 * Drm specifications
 */
public object LcpSpecification : Specification
public object AdeptSpecification : Specification

/*
 * Bitmap specifications
 */
public object AvifSpecification : Specification
public object BmpSpecification : Specification
public object GifSpecification : Specification
public object JpegSpecification : Specification
public object JxlSpecification : Specification
public object PngSpecification : Specification
public object TiffSpecification : Specification
public object WebpSpecification : Specification

/*
 * Audio specifications
 */
public object AacSpecification : Specification
public object AiffSpecification : Specification
public object FlacSpecification : Specification
public object Mp4Specification : Specification
public object Mp3Specification : Specification
public object OggSpecification : Specification
public object OpusSpecification : Specification
public object WavSpecification : Specification
public object WebmSpecification : Specification

/*
 * Publication package specifications
 */
public object EpubSpecification : Specification
public object RpfSpecification : Specification
public object LpfSpecification : Specification
public object InformalAudiobookSpecification : Specification
public object InformalComicSpecification : Specification

/*
 * Opds specifications
 */
public object Opds1CatalogSpecification : Specification
public object Opds1EntrySpecification : Specification
public object Opds2CatalogSpecification : Specification
public object Opds2PublicationSpecification : Specification
public object OpdsAuthenticationSpecification : Specification

/*
 * Language specifications
 */

public object JavaScriptSpecification : Specification

public object CssSpecification : Specification
