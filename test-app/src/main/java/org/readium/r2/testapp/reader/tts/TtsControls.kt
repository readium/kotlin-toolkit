/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalReadiumApi::class)

package org.readium.r2.testapp.reader.tts

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.readium.r2.navigator.media3.androidtts.AndroidTtsEngine
import org.readium.r2.navigator.media3.androidtts.AndroidTtsPreferencesEditor
import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language
import org.readium.r2.testapp.R
import org.readium.r2.testapp.shared.views.LanguageItem
import org.readium.r2.testapp.shared.views.MenuItem
import org.readium.r2.testapp.utils.extensions.asStateWhenStarted

/**
 * TTS controls bar displayed at the bottom of the screen when speaking a publication.
 */
@Composable
fun TtsControls(model: TtsViewModel, modifier: Modifier = Modifier) {
    val showControls by model.state.asStateWhenStarted { it.showControls }
    val isPlaying by model.state.asStateWhenStarted { it.isPlaying }
    val editor by model.editor.collectAsState()

    if (showControls) {
        TtsControls(
            playing = isPlaying,
            editor = editor,
            availableVoices = model.voices,
            commit = model::commit,
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
    editor: AndroidTtsPreferencesEditor,
    availableVoices: Set<AndroidTtsEngine.Voice>,
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
            speed = editor.speed,
            pitch = editor.pitch,
            language = editor.language,
            voices = editor.voices,
            availableVoices = availableVoices,
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

@Composable
private fun TtsPreferencesDialog(
    speed: RangePreference<Double>,
    pitch: RangePreference<Double>,
    language: Preference<Language?>,
    voices: Preference<Map<Language, String>>,
    availableVoices: Set<AndroidTtsEngine.Voice>,
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
        title = {
            Text(
                text = stringResource(R.string.tts_settings),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.h6,
                modifier = Modifier
                    .fillMaxWidth()
            )
        },
        text = {
            Column {
                MenuItem(
                    title = stringResource(R.string.speed_rate),
                    preference = speed.withSupportedValues(0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0),
                    formatValue = speed::formatValue,
                    commit = commit
                )
                MenuItem(
                    title = stringResource(R.string.pitch_rate),
                    preference = pitch.withSupportedValues(0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0),
                    formatValue = pitch::formatValue,
                    commit = commit
                )
                LanguageItem(
                    preference = language,
                    commit = commit
                )

                val context = LocalContext.current

                MenuItem(
                    title = stringResource(R.string.tts_voice),
                    preference = voices.map(
                        from = { voices ->
                            language.effectiveValue?.let { voices[it.removeRegion()] }
                        },
                        to = { voice ->
                            buildMap {
                                voices.value?.let { putAll(it) }
                                language.effectiveValue?.let {
                                    val withoutRegion = it.removeRegion()
                                    if (voice == null) {
                                        remove(withoutRegion)
                                    } else {
                                        put(withoutRegion, voice)
                                    }
                                }
                            }
                        }
                    ).withSupportedValues(
                        availableVoices
                            .filter { it.language.removeRegion() == language.effectiveValue }
                            .map { it.name }
                    ),
                    formatValue = { it  ?: context.getString(R.string.defaultValue) },
                    commit = commit
                )
            }
        }
    )
}
