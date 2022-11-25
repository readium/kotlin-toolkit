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
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.readium.navigator.media2.ExperimentalMedia2
import org.readium.navigator.media2.MediaNavigator
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.testapp.R
import org.readium.r2.testapp.databinding.FragmentAudiobookBinding
import org.readium.r2.testapp.utils.viewLifecycle
import timber.log.Timber

@OptIn(ExperimentalMedia2::class, ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class AudioReaderFragment : BaseReaderFragment(), SeekBar.OnSeekBarChangeListener {

    override lateinit var navigator: MediaNavigator

    private lateinit var displayedPlayback: MediaNavigator.Playback
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
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAudiobookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.publicationTitle.text = model.publication.metadata.title

        viewLifecycleOwner.lifecycleScope.launch {
            publication.cover()?.let {
                binding.coverView.setImageBitmap(it)
            }
        }

        displayedPlayback = navigator.playback.value

        viewLifecycleOwner.lifecycleScope.launch {
            navigator.playback.collectLatest { playback ->
                onPlaybackChanged(playback)
            }
        }
    }

    private fun onPlaybackChanged(playback: MediaNavigator.Playback) {
        Timber.v("onPlaybackChanged $playback")
        this.displayedPlayback = playback
        if (playback.state == MediaNavigator.Playback.State.Error) {
            onPlayerError()
            return
        }

        binding.playPause.isEnabled = true
        binding.timelineBar.isEnabled = true
        binding.timelineDuration.isEnabled = true
        binding.timelinePosition.isEnabled = true
        binding.playPause.setImageResource(
            if (playback.state == MediaNavigator.Playback.State.Playing)
                R.drawable.ic_baseline_pause_24
            else
                R.drawable.ic_baseline_play_arrow_24
        )
        if (seekingItem == null) {
            updateTimeline(playback.resource, playback.buffer.position)
        }
    }

    private fun updateTimeline(resource: MediaNavigator.Playback.Resource, buffered: Duration) {
        binding.timelineBar.max = resource.duration?.inWholeSeconds?.toInt() ?: 0
        binding.timelineDuration.text = resource.duration?.formatElapsedTime()
        binding.timelineBar.progress = resource.position.inWholeSeconds.toInt()
        binding.timelinePosition.text = resource.position.formatElapsedTime()
        binding.timelineBar.secondaryProgress = buffered.inWholeSeconds.toInt()
    }

    private fun Duration.formatElapsedTime(): String =
        DateUtils.formatElapsedTime(toLong(DurationUnit.SECONDS))

    private fun onPlayerError() {
        binding.playPause.isEnabled = false
        binding.timelineBar.isEnabled = false
        binding.timelinePosition.isEnabled = false
        binding.timelineDuration.isEnabled = false
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
        this.displayedPlayback.state == MediaNavigator.Playback.State.Finished

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            binding.timelinePosition.text = progress.seconds.formatElapsedTime()
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        Timber.d("onStartTrackingTouch")
        seekingItem = this.displayedPlayback.resource.index
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        Timber.d("onStopTrackingTouch")
        seekingItem?.let { index ->
            lifecycleScope.launch {
                navigator.seek(index, seekBar.progress.seconds)
                // Some timeline updates might have been missed during seeking.
                val playbackNow = navigator.playback.value
                updateTimeline(playbackNow.resource, playbackNow.buffer.position)
                seekingItem = null
            }
        }
    }

    private fun onPlayPause(@Suppress("UNUSED_PARAMETER") view: View) {
        return when (displayedPlayback.state) {
            MediaNavigator.Playback.State.Playing -> {
                model.viewModelScope.launch {
                    navigator.pause()
                }
                Unit
            }
            MediaNavigator.Playback.State.Paused -> {
                model.viewModelScope.launch {
                    navigator.play()
                }
                Unit
            }
            MediaNavigator.Playback.State.Finished -> {
                model.viewModelScope.launch {
                    navigator.seek(0, Duration.ZERO)
                    navigator.play()
                }
                Unit
            }
            MediaNavigator.Playback.State.Error -> {
                // Do nothing.
            }
        }
    }

    private fun onSkipForward(@Suppress("UNUSED_PARAMETER") view: View) {
        model.viewModelScope.launch {
            navigator.goForward()
        }
    }

    private fun onSkipBackward(@Suppress("UNUSED_PARAMETER") view: View) {
        model.viewModelScope.launch {
            navigator.goBackward()
        }
    }

    override fun go(locator: Locator, animated: Boolean) {
        model.viewModelScope.launch {
            navigator.go(locator)
            navigator.play()
        }
    }
}
