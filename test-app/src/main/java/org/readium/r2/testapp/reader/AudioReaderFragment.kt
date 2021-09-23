package org.readium.r2.testapp.reader

import android.media.AudioManager
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.activity.addCallback
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.readium.r2.navigator.ExperimentalAudiobook
import org.readium.r2.navigator.MediaNavigator
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.media.MediaService
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.testapp.R
import org.readium.r2.testapp.databinding.FragmentAudiobookBinding
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalAudiobook::class, ExperimentalTime::class)
class AudioReaderFragment : BaseReaderFragment() {

    override val model: ReaderViewModel by activityViewModels()
    override val navigator: Navigator get() = mediaNavigator

    private lateinit var mediaNavigator: MediaNavigator
    private lateinit var mediaService: MediaService.Connection

    private var binding: FragmentAudiobookBinding? = null
    private var isSeeking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val context = requireContext()

        mediaService = MediaService.connect(AudiobookService::class.java)

        // Get the currently playing navigator from the media service, if it is the same pub ID.
        // Otherwise, ask to switch to the new publication.
        mediaNavigator = mediaService.currentNavigator.value?.takeIf { it.publicationId == model.publicationId }
            ?: mediaService.getNavigator(context, model.publication, model.publicationId, model.initialLocation)

        mediaNavigator.play()

        super.onCreate(savedInstanceState)
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

        binding?.run {
            publicationTitle.text = model.publication.metadata.title

            viewLifecycleOwner.lifecycleScope.launch {
                model.publication.cover()?.let {
                    coverView.setImageBitmap(it)
                }
            }

            mediaNavigator.playback.asLiveData().observe(viewLifecycleOwner) { playback ->
                playPause.setImageResource(
                    if (playback.isPlaying) R.drawable.ic_baseline_pause_24
                    else R.drawable.ic_baseline_play_arrow_24
                )

                with(playback.timeline) {
                    if (!isSeeking) {
                        timelineBar.max = duration?.inWholeSeconds?.toInt() ?: 0
                        timelineBar.progress = position.inWholeSeconds.toInt()
                        buffered?.let { timelineBar.secondaryProgress = it.inWholeSeconds.toInt() }
                    }
                    timelinePosition.text = position.formatElapsedTime()
                    timelineDuration.text = duration?.formatElapsedTime() ?: ""
                }
            }

            timelineBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}

                override fun onStartTrackingTouch(p0: SeekBar?) {
                    isSeeking = true
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                    isSeeking = false
                    p0?.let { seekBar ->
                        mediaNavigator.seekTo(Duration.seconds(seekBar.progress))
                    }
                }

            })

            playPause.setOnClickListener { mediaNavigator.playPause() }
            skipForward.setOnClickListener { mediaNavigator.goForward() }
            skipBackward.setOnClickListener { mediaNavigator.goBackward() }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.volumeControlStream = AudioManager.STREAM_MUSIC
    }
}

@ExperimentalTime
private fun Duration.formatElapsedTime(): String =
    DateUtils.formatElapsedTime(toLong(DurationUnit.SECONDS))
