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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.readium.r2.navigator.ExperimentalAudiobook
import org.readium.r2.navigator.media2.MediaNavigator
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.testapp.R
import org.readium.r2.testapp.R2App
import org.readium.r2.testapp.databinding.FragmentAudiobookBinding
import timber.log.Timber
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalAudiobook::class, ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class AudioReaderFragment2 : BaseReaderFragment(), SeekBar.OnSeekBarChangeListener {

    override val model: ReaderViewModel by activityViewModels()
    override val navigator: MediaNavigator get() = _navigator

    private lateinit var _navigator: MediaNavigator
    private lateinit var displayedPlayback: MediaNavigator.Playback
    private var binding: FragmentAudiobookBinding? = null
    private var seekingItem: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _navigator = (requireActivity().application as R2App).getMediaNavigator()

        if (savedInstanceState == null) {
            lifecycleScope.launch { navigator.play() }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentAudiobookBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = requireNotNull(binding)

        binding.publicationTitle.text = model.publication.metadata.title

        viewLifecycleOwner.lifecycleScope.launch {
            model.publication.cover()?.let {
                binding.coverView.setImageBitmap(it)
            }
        }

        displayedPlayback = _navigator.playback.value

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

        val binding = checkNotNull(binding)
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
            updateTimeline(playback.currentLink, playback.currentPosition, playback.bufferedPosition)
        }
    }

    private fun updateTimeline(link: Link, position: Duration?, buffered: Duration?) {
        val binding = checkNotNull(binding)
        binding.timelineBar.max = link.duration?.toInt() ?: 0
        binding.timelineDuration.text = link.formattedDuration
        binding.timelineBar.progress = position?.inWholeSeconds?.toInt() ?: 0
        binding.timelinePosition.text = position?.formatElapsedTime()
        binding.timelineBar.secondaryProgress = buffered?.inWholeSeconds?.toInt() ?: 0
    }

    private val Link.formattedDuration: String?
        get() = duration?.let { DateUtils.formatElapsedTime(it.roundToLong()) }

    private fun Duration.formatElapsedTime(): String =
        DateUtils.formatElapsedTime(toLong(DurationUnit.SECONDS))

    private fun onPlayerError() {
        val binding = checkNotNull(binding)
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
        val binding = checkNotNull(binding)
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
            val binding = checkNotNull(binding)
            binding.timelinePosition.text = progress.seconds.formatElapsedTime()
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        Timber.d("onStartTrackingTouch")
        seekingItem = this.displayedPlayback.currentIndex
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        Timber.d("onStopTrackingTouch")
        seekingItem?.let { index ->
            lifecycleScope.launch {
                navigator.seek(index, seekBar.progress.seconds)
                // Some timeline updates might have been missed during seeking.
                val playbackNow = navigator.playback.value
                updateTimeline(playbackNow.currentLink, playbackNow.currentPosition, playbackNow.bufferedPosition)
                seekingItem = null
            }
        }
    }

    private fun onPlayPause(@Suppress("UNUSED_PARAMETER") view: View) {
        return when (displayedPlayback.state) {
            MediaNavigator.Playback.State.Playing -> {
                lifecycleScope.launch {
                    navigator.pause()
                }
                Unit
            }
            MediaNavigator.Playback.State.Paused -> {
                lifecycleScope.launch {
                    navigator.play()
                }
                Unit
            }
            MediaNavigator.Playback.State.Finished -> {
                lifecycleScope.launch {
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
        lifecycleScope.launch {
            navigator.goForward()
        }
    }

    private fun onSkipBackward(@Suppress("UNUSED_PARAMETER") view: View) {
        lifecycleScope.launch {
            navigator.goBackward()
        }
    }

    override fun go(locator: Locator, animated: Boolean){
        model.viewModelScope.launch {
            navigator.go(locator)
            navigator.play() }
    }
}
