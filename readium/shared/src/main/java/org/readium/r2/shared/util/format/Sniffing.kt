/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.format

import java.nio.charset.Charset
import org.readium.r2.shared.util.FileExtension
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.Readable
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * Bundle of media type and file extension hints for the [FormatHintsSniffer].
 */
public data class FormatHints(
    val mediaTypes: List<MediaType> = emptyList(),
    val fileExtensions: List<FileExtension> = emptyList(),
) {
    public companion object {
        public operator fun invoke(
            mediaType: MediaType? = null,
            fileExtension: FileExtension? = null,
        ): FormatHints =
            FormatHints(
                mediaTypes = listOfNotNull(mediaType),
                fileExtensions = listOfNotNull(fileExtension)
            )

        public operator fun invoke(
            mediaTypes: List<String> = emptyList(),
            fileExtensions: List<String> = emptyList(),
        ): FormatHints =
            FormatHints(
                mediaTypes = mediaTypes.mapNotNull { MediaType(it) },
                fileExtensions = fileExtensions.map { FileExtension(it) }
            )
    }

    public operator fun plus(other: FormatHints): FormatHints =
        FormatHints(
            mediaTypes = mediaTypes + other.mediaTypes,
            fileExtensions = fileExtensions + other.fileExtensions
        )

    /**
     * Returns a new [FormatHints] after appending the given [fileExtension] hint.
     */
    public fun addFileExtension(fileExtension: String?): FormatHints {
        fileExtension ?: return this
        return copy(fileExtensions = fileExtensions + FileExtension(fileExtension))
    }

    /** Finds the first [Charset] declared in the media types' `charset` parameter. */
    public val charset: Charset? get() =
        mediaTypes.firstNotNullOfOrNull { it.charset }

    /** Returns whether this context has any of the given file extensions, ignoring case. */
    public fun hasFileExtension(vararg fileExtensions: String): Boolean {
        val fileExtensionsHints = this.fileExtensions.map { it.value.lowercase() }
        for (fileExtension in fileExtensions.map { it.lowercase() }) {
            if (fileExtensionsHints.contains(fileExtension)) {
                return true
            }
        }
        return false
    }

    /**
     * Returns whether this context has any of the given media type, ignoring case and extra
     * parameters.
     *
     * Implementation note: Use [MediaType] to handle the comparison to avoid edge cases.
     */
    public fun hasMediaType(vararg mediaTypes: String): Boolean {
        @Suppress("NAME_SHADOWING")
        val mediaTypes = mediaTypes.mapNotNull { MediaType(it) }
        for (mediaType in mediaTypes) {
            if (this.mediaTypes.any { mediaType.contains(it) }) {
                return true
            }
        }
        return false
    }
}

/**
 * Tries to guess a [Format] from media type and file extension hints.
 */
public interface FormatHintsSniffer {

    public fun sniffHints(
        hints: FormatHints,
    ): Format?
}

/**
 * Tries to refine a [Format] by sniffing a [Readable] blob.
 */
public interface BlobSniffer {

    public suspend fun sniffBlob(
        format: Format,
        source: Readable,
    ): Try<Format, ReadError>
}

/**
 * Tries to refine a [Format] by sniffing a [Container].
 */
public interface ContainerSniffer {

    public suspend fun sniffContainer(
        format: Format,
        container: Container<Readable>,
    ): Try<Format, ReadError>
}

/**
 * Tries to refine a [Format] by sniffing format hints or content.
 */
public interface FormatSniffer :
    FormatHintsSniffer,
    BlobSniffer,
    ContainerSniffer {

    public override fun sniffHints(
        hints: FormatHints,
    ): Format? = null

    public override suspend fun sniffBlob(
        format: Format,
        source: Readable,
    ): Try<Format, ReadError> =
        Try.success(format)

    public override suspend fun sniffContainer(
        format: Format,
        container: Container<Readable>,
    ): Try<Format, ReadError> =
        Try.success(format)
}

public class CompositeFormatSniffer(
    private val sniffers: List<FormatSniffer>,
) : FormatSniffer {

    public constructor(vararg sniffers: FormatSniffer) : this(sniffers.toList())

    override fun sniffHints(hints: FormatHints): Format? =
        sniffers.firstNotNullOfOrNull { it.sniffHints(hints) }

    override suspend fun sniffBlob(format: Format, source: Readable): Try<Format, ReadError> =
        sniffers.fold(Try.success(format)) { acc: Try<Format, ReadError>, sniffer ->
            when (acc) {
                is Try.Failure ->
                    acc
                is Try.Success ->
                    when (val new = sniffer.sniffBlob(acc.value, source)) {
                        is Try.Failure ->
                            new
                        is Try.Success ->
                            new.takeIf { it.value.refines(acc.value) } ?: acc
                    }
            }
        }

    override suspend fun sniffContainer(
        format: Format,
        container: Container<Readable>,
    ): Try<Format, ReadError> =
        sniffers.fold(Try.success(format)) { acc: Try<Format, ReadError>, sniffer ->
            when (acc) {
                is Try.Failure ->
                    acc
                is Try.Success ->
                    when (val new = sniffer.sniffContainer(acc.value, container)) {
                        is Try.Failure ->
                            new
                        is Try.Success ->
                            new.takeIf { it.value.refines(acc.value) } ?: acc
                    }
            }
        }
}
