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
import org.readium.r2.navigator.tts.PublicationSpeechSynthesizer.Configuration
import org.readium.r2.navigator.tts.TtsEngine.Voice
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language
import org.readium.r2.testapp.R
import org.readium.r2.testapp.shared.views.SelectorListItem
import org.readium.r2.testapp.utils.extensions.asStateWhenStarted
import java.text.DecimalFormat

/**
 * TTS controls bar displayed at the bottom of the screen when speaking a publication.
 */
@OptIn(ExperimentalReadiumApi::class)
@Composable
fun TtsControls(model: TtsViewModel, modifier: Modifier = Modifier) {
    val showControls by model.state.asStateWhenStarted { it.showControls }
    val isPlaying by model.state.asStateWhenStarted { it.isPlaying }
    val settings by model.state.asStateWhenStarted { it.settings }

    if (showControls) {
        TtsControls(
            playing = isPlaying,
            availableRates = listOf(0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0)
                .filter { it in settings.rateRange },
            availableLanguages = settings.availableLanguages,
            availableVoices = settings.availableVoices,
            config = settings.config,
            onConfigChange = model::setConfig,
            onPlayPause = model::pauseOrResume,
            onStop = model::stop,
            onPrevious = model::previous,
            onNext = model::next,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalReadiumApi::class)
@Composable
fun TtsControls(
    playing: Boolean,
    availableRates: List<Double>,
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
            availableRates = availableRates,
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
            val largeButtonModifier = Modifier.size(40.dp)

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
                    modifier = Modifier.then(largeButtonModifier)
                )
            }
            IconButton(
                onClick = onStop,
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

            IconButton(onClick = { showSettings = true }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.tts_settings)
                )
            }
        }
    }
}

@OptIn(ExperimentalReadiumApi::class)
@Composable
private fun TtsSettingsDialog(
    availableRates: List<Double>,
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
                if (availableRates.size > 1) {
                    SelectorListItem(
                        label = stringResource(R.string.tts_rate),
                        values = availableRates,
                        selection = config.rateMultiplier,
                        titleForValue = { rate ->
                            DecimalFormat("x#.##").format(rate)
                        },
                        onSelected = {
                            onConfigChange(config.copy(rateMultiplier = it))
                        }
                    )
                }

                SelectorListItem(
                    label = stringResource(R.string.language),
                    values = availableLanguages,
                    selection = config.defaultLanguage,
                    titleForValue = { language ->
                        language?.locale?.displayName
                            ?: stringResource(R.string.auto)
                    },
                    onSelected = { onConfigChange(config.copy(defaultLanguage = it, voiceId = null)) }
                )

                SelectorListItem(
                    label = stringResource(R.string.tts_voice),
                    values = availableVoices,
                    selection = availableVoices.firstOrNull { it.id == config.voiceId },
                    titleForValue = { it?.name ?: it?.id ?: stringResource(R.string.auto) },
                    onSelected = { onConfigChange(config.copy(voiceId = it?.id)) }
                )
            }
        }
    )
}