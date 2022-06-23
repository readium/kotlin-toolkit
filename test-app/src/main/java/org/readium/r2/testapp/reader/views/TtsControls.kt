/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.readium.r2.testapp.R
import org.readium.r2.testapp.reader.ReaderViewModel

@Composable
fun TtsControls(viewModel: ReaderViewModel, modifier: Modifier = Modifier) {
    TtsControls(
        playing = viewModel.isTtsPlaying.collectAsState().value,
        onPlayPause = { viewModel.ttsPlayPause() },
        onStop = { viewModel.ttsStop() },
        onPrevious = { viewModel.ttsPrevious() },
        onNext = { viewModel.ttsNext() },
        modifier = modifier
    )
}

@Composable
fun TtsControls(
    playing: Boolean,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = stringResource(R.string.tts_previous),
                )
            }

            IconButton(
                onClick = onPlayPause,
            ) {
                Icon(
                    imageVector = if (playing) Icons.Default.Pause
                    else Icons.Default.PlayArrow,
                    contentDescription = stringResource(
                        if (playing) R.string.tts_pause
                        else R.string.tts_play
                    ),
                    modifier = Modifier.then(IconButtonLargeSizeModifier)
                )
            }
            IconButton(
                onClick = onStop,
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = stringResource(R.string.tts_stop),
                    modifier = Modifier.then(IconButtonLargeSizeModifier)
                )
            }
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = stringResource(R.string.tts_next)
                )
            }
        }
    }
}

private val IconButtonLargeSizeModifier = Modifier.size(40.dp)

@Preview(showBackground = true)
@Composable
fun PreviewTtsControls() {
    TtsControls(
        playing = true,
        onPlayPause = {},
        onStop = {},
        onPrevious = {},
        onNext = {}
    )
}