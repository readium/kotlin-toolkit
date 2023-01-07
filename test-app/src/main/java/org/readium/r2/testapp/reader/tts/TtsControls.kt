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
import java.text.DecimalFormat
import org.readium.r2.navigator.media3.androidtts.AndroidTtsPreferencesEditor
import org.readium.r2.navigator.tts.TtsEngine.Voice
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language
import org.readium.r2.testapp.R
import org.readium.r2.testapp.shared.views.SelectorListItem
import org.readium.r2.testapp.utils.extensions.asStateWhenStarted

/**
 * TTS controls bar displayed at the bottom of the screen when speaking a publication.
 */
@OptIn(ExperimentalReadiumApi::class)
@Composable
fun TtsControls(model: TtsViewModel, modifier: Modifier = Modifier) {
    val showControls by model.state.asStateWhenStarted { it.showControls }
    val isPlaying by model.state.asStateWhenStarted { it.isPlaying }
    // val settings by model.state.asStateWhenStarted { it.settings }

    val editor = remember { mutableStateOf(model.preferencesEditor, policy = neverEqualPolicy()) }
    val commit: () -> Unit = { editor.value = editor.value ; model.commitPreferences() }

    if (showControls) {
        TtsControls(
            playing = isPlaying,
            availableRates = listOf(0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0),
            // .filter { it in settings.rateRange },
            availableLanguages = emptyList(), // settings.availableLanguages,
            availableVoices = emptyList(), // settings.availableVoices,
            editor = editor.value,
            commit = commit,
            onPlayPause = { if (isPlaying) model.pause() else model.play() },
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
    editor: AndroidTtsPreferencesEditor,
    commit: () -> Unit,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        TtsPreferencesDialog(
            availableRates = availableRates,
            availableLanguages = availableLanguages,
            availableVoices = availableVoices,
            editor = editor,
            commit = commit,
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
private fun TtsPreferencesDialog(
    availableRates: List<Double>,
    availableLanguages: List<Language>,
    availableVoices: List<Voice>,
    editor: AndroidTtsPreferencesEditor,
    commit: () -> Unit,
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
                        selection = editor.speedRate.value,
                        titleForValue = { rate ->
                            DecimalFormat("x#.##").format(rate)
                        },
                        onSelected = {
                            editor.speedRate.set(it)
                            commit()
                        }
                    )
                }

                SelectorListItem(
                    label = stringResource(R.string.language),
                    values = availableLanguages,
                    selection = editor.language.value,
                    titleForValue = { language ->
                        language?.locale?.displayName
                            ?: stringResource(R.string.auto)
                    },
                    onSelected = {
                        editor.language.set(it)
                        commit()
                    }
                )

                SelectorListItem(
                    label = stringResource(R.string.tts_voice),
                    values = availableVoices,
                    selection = availableVoices.firstOrNull { it.id == editor.voiceId.value },
                    titleForValue = { it?.name ?: it?.id ?: stringResource(R.string.auto) },
                    onSelected = {
                        editor.voiceId.set(it?.id)
                        commit()
                    }
                )
            }
        }
    )
}
