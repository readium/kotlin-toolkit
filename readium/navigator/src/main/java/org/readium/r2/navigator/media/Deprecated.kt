/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media

import org.readium.r2.navigator.ExperimentalAudiobook

@Deprecated(
    message = "Moved to a new module readium-adapter-androidx-media",
    replaceWith = ReplaceWith("org.readium.adapters.androidx.media.MediaPlayer"),
    level = DeprecationLevel.ERROR
)
@ExperimentalAudiobook
interface MediaPlayer

@Deprecated(
    message = "Moved to a new module readium-adapter-androidx-media",
    replaceWith = ReplaceWith("org.readium.adapters.androidx.media.MediaService"),
    level = DeprecationLevel.ERROR
)
@ExperimentalAudiobook
class MediaService

@Deprecated(
    message = "Moved to a new module readium-adapter-androidx-media",
    replaceWith = ReplaceWith("org.readium.adapters.androidx.media.MediaSessionNavigator"),
    level = DeprecationLevel.ERROR
)
@ExperimentalAudiobook
class MediaSessionNavigator

@Deprecated(
    message = "Moved to a new module readium-adapter-androidx-media",
    replaceWith = ReplaceWith("org.readium.adapters.androidx.media.PendingMedia"),
    level = DeprecationLevel.ERROR
)
@ExperimentalAudiobook
class PendingMedia

@Deprecated(
    message = "Moved to a new module readium-adapter-exoplayer",
    replaceWith = ReplaceWith("org.readium.adapters.exoplayer.ExoMediaPlayer"),
    level = DeprecationLevel.ERROR
)
@ExperimentalAudiobook
class ExoMediaPlayer

