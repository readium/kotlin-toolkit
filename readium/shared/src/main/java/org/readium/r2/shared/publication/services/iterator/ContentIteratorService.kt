package org.readium.r2.shared.publication.services.iterator

import android.icu.text.BreakIterator
import android.os.Build
import androidx.annotation.RequiresApi
import org.readium.r2.shared.UserException
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.SuspendingCloseable
import org.readium.r2.shared.util.Try

typealias ContentIteratorTry<SuccessT> = Try<SuccessT, ContentIteratorException>

/**
 * Represents an error which might occur while iterating through a publication's content.
 */
sealed class ContentIteratorException private constructor(
    override val message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    class UnsupportedOption(message: String) : ContentIteratorException(message)
}

interface Content

interface ContentIteratorService : Publication.Service {
    suspend fun <O, T : Content> iterator(startLocator: Locator?, options: O? = null): ContentIteratorTry<ContentIterator<T, O>>
}

/**
 * Iterates through a publication's content.
 */
interface ContentIterator<T, O> : SuspendingCloseable {

    /**
     * Retrieves the next piece of content.
     *
     * @return Null when reaching the end of the publication, or an error in case of failure.
     */
    suspend fun next(): ContentIteratorTry<T?>

    /**
     * Closes any resources allocated for the search query, such as a cursor.
     * To be called when the user dismisses the search.
     */
    override suspend fun close() {}
}
