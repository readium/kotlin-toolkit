/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import android.annotation.SuppressLint
import android.media.AudioManager
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.readium.adapter.exoplayer.audio.ExoPlayerEngine
import org.readium.adapter.exoplayer.audio.ExoPlayerPreferences
import org.readium.adapter.exoplayer.audio.ExoPlayerSettings
import org.readium.navigator.media.audio.AudioNavigator
import org.readium.navigator.media.common.MediaNavigator
import org.readium.navigator.media.common.TimeBasedMediaNavigator
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.testapp.R
import org.readium.r2.testapp.databinding.FragmentAudiobookBinding
import org.readium.r2.testapp.domain.toUserError
import org.readium.r2.testapp.reader.preferences.UserPreferencesViewModel
import org.readium.r2.testapp.utils.UserError
import org.readium.r2.testapp.utils.viewLifecycle
import timber.log.Timber

@OptIn(ExperimentalReadiumApi::class)
class AudioReaderFragment : BaseReaderFragment(), SeekBar.OnSeekBarChangeListener {

    override lateinit var navigator: TimeBasedMediaNavigator<*, *, *>

    private var binding: FragmentAudiobookBinding by viewLifecycle()
    private var seekingItem: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val readerData = model.readerInitData as MediaReaderInitData
        navigator = readerData.mediaNavigator

        if (savedInstanceState == null) {
            model.viewModelScope.launch { navigator.play() }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAudiobookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        @Suppress("Unchecked_cast")
        (navigator as? Configurable<ExoPlayerSettings, ExoPlayerPreferences>)
            ?.let { navigator ->
                @Suppress("Unchecked_cast")
                (model.settings as UserPreferencesViewModel<ExoPlayerSettings, ExoPlayerPreferences>)
                    .bind(navigator, viewLifecycleOwner)
            }

        binding.publicationTitle.text = model.publication.metadata.title

        viewLifecycleOwner.lifecycleScope.launch {
            publication.cover()?.let {
                binding.coverView.setImageBitmap(it)
            }
        }

        navigator.playback
            .onEach { onPlaybackChanged(it) }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun onPlaybackChanged(
        playback: TimeBasedMediaNavigator.Playback,
    ) {
        Timber.v("onPlaybackChanged $playback")
        val failureState = playback.state as? AudioNavigator.State.Failure<*>
        if (failureState != null) {
            val error = failureState.error as ExoPlayerEngine.Error
            onPlayerError(error)
            return
        }

        binding.playPause.isEnabled = true
        binding.timelineBar.isEnabled = true
        binding.timelineDuration.isEnabled = true
        binding.timelinePosition.isEnabled = true
        binding.playPause.setImageResource(
            if (playback.playWhenReady) {
                R.drawable.ic_baseline_pause_24
            } else {
                R.drawable.ic_baseline_play_arrow_24
            }
        )

        if (seekingItem == null) {
            updateTimeline(playback)
        }
    }

    private fun updateTimeline(
        playback: TimeBasedMediaNavigator.Playback,
    ) {
        val currentItem = navigator.readingOrder.items[playback.index]
        binding.timelineBar.max = currentItem.duration?.inWholeSeconds?.toInt() ?: 0
        binding.timelineDuration.text = currentItem.duration?.formatElapsedTime()
        binding.timelineBar.progress = playback.offset.inWholeSeconds.toInt()
        binding.timelinePosition.text = playback.offset.formatElapsedTime()
        binding.timelineBar.secondaryProgress = playback.buffered?.inWholeSeconds?.toInt() ?: 0
    }

    private fun Duration.formatElapsedTime(): String =
        DateUtils.formatElapsedTime(toLong(DurationUnit.SECONDS))

    private fun onPlayerError(error: ExoPlayerEngine.Error) {
        binding.playPause.isEnabled = false
        binding.timelineBar.isEnabled = false
        binding.timelinePosition.isEnabled = false
        binding.timelineDuration.isEnabled = false
        val userError = when (error) {
            is ExoPlayerEngine.Error.Engine ->
                UserError(error.message, error)
            is ExoPlayerEngine.Error.Source ->
                error.cause.toUserError()
        }
        userError.show(requireActivity())
    }

    override fun onResume() {
        super.onResume()
        activity?.volumeControlStream = AudioManager.STREAM_MUSIC
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onStart() {
        super.onStart()
        binding.timelineBar.setOnTouchListener(this::forbidUserSeeking)
        binding.timelineBar.setOnSeekBarChangeListener(this)
        binding.playPause.setOnClickListener(this::onPlayPause)
        binding.skipForward.setOnClickListener(this::onSkipForward)
        binding.skipBackward.setOnClickListener(this::onSkipBackward)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun forbidUserSeeking(view: View, event: MotionEvent): Boolean =
        navigator.playback.value.state is MediaNavigator.State.Ended

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            binding.timelinePosition.text = progress.seconds.formatElapsedTime()
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        Timber.d("onStartTrackingTouch")
        seekingItem = navigator.playback.value.index
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        Timber.d("onStopTrackingTouch")
        navigator.skipTo(checkNotNull(seekingItem), seekBar.progress.seconds)
        seekingItem = null
    }

    private fun onPlayPause(@Suppress("UNUSED_PARAMETER") view: View) {
        return when (navigator.playback.value.state) {
            is MediaNavigator.State.Ready, is MediaNavigator.State.Buffering -> {
                model.viewModelScope.launch {
                    if (navigator.playback.value.playWhenReady) {
                        navigator.pause()
                    } else {
                        navigator.play()
                    }
                }
                Unit
            }
            is MediaNavigator.State.Ended -> {
                model.viewModelScope.launch {
                    navigator.skipTo(0, Duration.ZERO)
                    navigator.play()
                }
                Unit
            }
            is MediaNavigator.State.Failure -> {
                // Do nothing.
            }
        }
    }

    private fun onSkipForward(@Suppress("UNUSED_PARAMETER") view: View) {
        model.viewModelScope.launch {
            navigator.skipForward()
        }
    }

    private fun onSkipBackward(@Suppress("UNUSED_PARAMETER") view: View) {
        model.viewModelScope.launch {
            navigator.skipBackward()
        }
    }

    override fun go(locator: Locator, animated: Boolean) {
        model.viewModelScope.launch {
            navigator.go(locator)
            navigator.play()
        }
    }
}
