/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader.tts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.readium.r2.testapp.R
import org.readium.r2.testapp.utils.extensions.asStateWhenStarted

/**
 * TTS controls bar displayed at the bottom of the screen when speaking a publication.
 */
@Composable
fun TtsControls(
    model: TtsViewModel,
    onPreferences: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showControls by model.showControls.asStateWhenStarted()
    val isPlaying by model.isPlaying.asStateWhenStarted()

    if (showControls) {
        TtsControls(
            playing = isPlaying,
            onPlayPause = { if (isPlaying) model.pause() else model.play() },
            onStop = model::stop,
            onPrevious = model::previous,
            onNext = model::next,
            onPreferences = onPreferences,
            modifier = modifier
        )
    }
}

@Composable
fun TtsControls(
    playing: Boolean,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPreferences: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val largeButtonModifier = Modifier.size(40.dp)

            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = stringResource(R.string.tts_previous)
                )
            }

            IconButton(
                onClick = onPlayPause
            ) {
                Icon(
                    imageVector = if (playing) {
                        Icons.Default.Pause
                    } else {
                        Icons.Default.PlayArrow
                    },
                    contentDescription = stringResource(
                        if (playing) {
                            R.string.tts_pause
                        } else {
                            R.string.tts_play
                        }
                    ),
                    modifier = Modifier.then(largeButtonModifier)
                )
            }
            IconButton(
                onClick = onStop
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = stringResource(R.string.tts_stop),
                    modifier = Modifier.then(largeButtonModifier)
                )
            }
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = stringResource(R.string.tts_next)
                )
            }

            Spacer(modifier = Modifier.size(8.dp))

            IconButton(onClick = onPreferences) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.tts_settings)
                )
            }
        }
    }
}
