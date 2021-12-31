package org.readium.r2.navigator.media2

import org.readium.r2.shared.util.Try

typealias MediaNavigatorResult = Try<Unit, MediaNavigatorException>

internal fun MediaControllerResult.toNavigatorResult(): MediaNavigatorResult =
    if (isSuccess)
        Try.success(Unit)
    else
        this.mapFailure { MediaNavigatorException(it.error) }

class MediaNavigatorException internal constructor(
    internal val error: MediaControllerError
) : Exception()
