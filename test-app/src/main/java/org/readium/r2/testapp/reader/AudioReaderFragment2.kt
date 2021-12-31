package org.readium.r2.testapp.reader

import android.media.AudioManager
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.readium.r2.navigator.ExperimentalAudiobook
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.media2.MediaNavigatorPlayback
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.testapp.R
import org.readium.r2.testapp.databinding.FragmentAudiobookBinding
import timber.log.Timber
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalAudiobook::class, ExperimentalTime::class)
class AudioReaderFragment2 : BaseReaderFragment(), SeekBar.OnSeekBarChangeListener {

    override val model: ReaderViewModel by activityViewModels()
    private val audioModel: AudioReaderFragmentViewModel by viewModels(factoryProducer = {
        AudioReaderFragmentViewModel.Factory(
            requireActivity().application,
            model.bookId,
            model.publication,
            model.initialLocation
        )
    })

    override val navigator: Navigator get() = audioModel.navigator

    private var binding: FragmentAudiobookBinding? = null
    private var seekingItem: Int? = null
    private var playerState: MediaNavigatorPlayback? = null

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

        viewLifecycleOwner.lifecycleScope.launch {
            audioModel.navigator.playback.collectLatest { playback ->
               onPlaybackChanged(playback)
            }
        }
    }

    private fun onPlaybackChanged(playback: MediaNavigatorPlayback?) {
        Timber.v("onPlaybackChanged $playback")
        this.playerState = playback
        return when (playback) {
            null, MediaNavigatorPlayback.Error ->
                onPlaybackNotPlaying()
            is MediaNavigatorPlayback.Playing ->
                onPlaybackPlaying(playback)
            is MediaNavigatorPlayback.Finished -> {
                // Do nothing.
            }
        }
    }

    private fun onPlaybackPlaying(playback: MediaNavigatorPlayback.Playing) {
        val binding = checkNotNull(binding)
        binding.playPause.isEnabled = true
        binding.timelineBar.isEnabled = true
        binding.timelineDuration.isEnabled = true
        binding.timelinePosition.isEnabled = true
        binding.playPause.setImageResource(
            if (playback.paused) R.drawable.ic_baseline_play_arrow_24
            else R.drawable.ic_baseline_pause_24
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

    private fun onPlaybackNotPlaying() {
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

    override fun onStart() {
        super.onStart()
        val binding = checkNotNull(binding)
        binding.timelineBar.setOnSeekBarChangeListener(this)
        binding.playPause.setOnClickListener(this::onPlayPause)
        binding.skipForward.setOnClickListener(this::onSkipForward)
        binding.skipBackward.setOnClickListener(this::onSkipBackward)
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            val binding = checkNotNull(binding)
            binding.timelinePosition.text = progress.seconds.formatElapsedTime()
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        Timber.d("onStartTrackingTouch")
        val stateNow = playerState
        if (stateNow is MediaNavigatorPlayback.Playing) {
            seekingItem = stateNow.currentIndex
        }
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        Timber.d("onStopTrackingTouch")
        seekingItem?.let { index ->
            lifecycleScope.launch {
                audioModel.navigator.seek(index, seekBar.progress.seconds)
                // Some timeline updates might have been missed during seeking.
                when (val stateNow = audioModel.navigator.playback.value) {
                    is MediaNavigatorPlayback.Playing -> {
                        updateTimeline(stateNow.currentLink, stateNow.currentPosition, stateNow.bufferedPosition)
                    }
                    MediaNavigatorPlayback.Finished -> {
                        val lastItem = audioModel.navigator.playlist!!.last()
                        updateTimeline(lastItem, lastItem.duration?.seconds, lastItem.duration?.seconds)
                    }
                    MediaNavigatorPlayback.Error, null -> {
                        // Do nothing
                    }
                }
                seekingItem = null
            }
        }
    }

    private fun onPlayPause(@Suppress("UNUSED_PARAMETER") view: View) {
        return when (val stateNow = playerState) {
            null -> {
                lifecycleScope.launch {
                    audioModel.navigator.pause()
                }
                Unit
            }
            is MediaNavigatorPlayback.Playing -> {
                if (stateNow.paused)
                    lifecycleScope.launch {
                        audioModel.navigator.play()
                    }
                else
                    lifecycleScope.launch {
                        audioModel.navigator.pause()
                    }
                Unit
            }
            is MediaNavigatorPlayback.Finished -> {
                lifecycleScope.launch {
                    audioModel.navigator.seek(0, Duration.ZERO)
                }
                Unit
            }
            is MediaNavigatorPlayback.Error -> {
                // Do nothing.
            }
        }
    }

    private fun onSkipForward(@Suppress("UNUSED_PARAMETER") view: View) {
        return when (playerState) {
            null, is MediaNavigatorPlayback.Playing -> {
                lifecycleScope.launch { navigator.goForward() }
                Unit
            }
            is MediaNavigatorPlayback.Finished, is MediaNavigatorPlayback.Error -> {
                // Do nothing
            }
        }
    }

    private fun onSkipBackward(@Suppress("UNUSED_PARAMETER") view: View) {
        return when (playerState) {
            is MediaNavigatorPlayback.Playing, is MediaNavigatorPlayback.Finished -> {
                lifecycleScope.launch { navigator.goBackward() }
                Unit
            }
            null, is MediaNavigatorPlayback.Error ->  {
                // Do nothing
            }
        }
    }

    private val Link.formattedDuration: String?
        get() = duration?.let { DateUtils.formatElapsedTime(it.roundToLong()) }

    private fun Duration.formatElapsedTime(): String =
        DateUtils.formatElapsedTime(toLong(DurationUnit.SECONDS))
}
