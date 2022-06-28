/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader.tts

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.readium.r2.navigator.tts.TtsEngine
import org.readium.r2.navigator.tts.TtsEngine.Configuration
import org.readium.r2.navigator.tts.TtsEngine.Voice
import org.readium.r2.shared.util.Language
import org.readium.r2.testapp.R
import org.readium.r2.testapp.shared.views.SelectorListItem
import org.readium.r2.testapp.utils.extensions.flowWithLocalLifecycle
import java.text.DecimalFormat
import java.util.*

@Composable
fun TtsControls(model: TtsViewModel, modifier: Modifier = Modifier) {
    val state by model.state
        .flowWithLocalLifecycle()
        .collectAsState()

    val settings by model.settings
        .flowWithLocalLifecycle()
        .collectAsState()

    if (state.showControls) {
        TtsControls(
            playing = state.isPlaying,
            rateRange = settings.rateRange,
            availableLanguages = settings.availableLanguages,
            availableVoices = settings.availableVoices,
            config = settings.config,
            onConfigChange = model::setConfig,
            onPlayPause = model::playPause,
            onStop = model::stop,
            onPrevious = model::previous,
            onNext = model::next,
            modifier = modifier
        )
    }
}

@Composable
fun TtsControls(
    playing: Boolean,
    rateRange: ClosedRange<Double>,
    availableLanguages: List<Language>,
    availableVoices: List<Voice>,
    config: Configuration?,
    onConfigChange: (Configuration) -> Unit,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSettings by remember { mutableStateOf(false) }

    if (config != null && showSettings) {
        TtsSettingsDialog(
            rateRange = rateRange,
            availableLanguages = availableLanguages,
            availableVoices = availableVoices,
            config = config,
            onConfigChange = onConfigChange,
            onDismiss = { showSettings = false }
        )
    }

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

            Spacer(modifier = Modifier.size(8.dp))

            IconButton(onClick = { showSettings = true }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.tts_settings)
                )
            }
        }
    }
}

private val IconButtonLargeSizeModifier = Modifier.size(40.dp)

private val availableRates = listOf(0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0)

@Composable
private fun TtsSettingsDialog(
    rateRange: ClosedRange<Double>,
    availableLanguages: List<Language>,
    availableVoices: List<Voice>,
    config: Configuration,
    onConfigChange: (Configuration) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.close))
            }
        },
        title = { Text(stringResource(R.string.tts_settings)) },
        text = {
            Column {
                if (rateRange.start < rateRange.endInclusive) {
                    SelectorListItem(
                        label = stringResource(R.string.tts_rate),
                        values = availableRates
                            .filter { it in rateRange },
                        selection = config.rate,
                        titleOfSelection = { DecimalFormat("x#.##").format(it) },
                        onSelected = {
                            onConfigChange(config.copy(rate = it))
                        }
                    )
                }

                SelectorListItem(
                    label = stringResource(R.string.language),
                    values = availableLanguages,
                    selection = config.defaultLanguage,
                    titleOfSelection = { it?.locale?.displayName ?: stringResource(R.string.auto) },
                    onSelected = { onConfigChange(config.copy(defaultLanguage = it, voice = null)) }
                )

                SelectorListItem(
                    label = stringResource(R.string.voice),
                    values = availableVoices,
                    selection = config.voice,
                    titleOfSelection = { it?.name ?: stringResource(R.string.auto) },
                    onSelected = { onConfigChange(config.copy(voice = it)) }
                )
            }
        }
    )
}