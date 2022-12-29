package org.readium.r2.navigator.audiobook

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.BuildConfig.DEBUG
import org.readium.r2.navigator.IR2Activity
import org.readium.r2.navigator.NavigatorDelegate
import org.readium.r2.navigator.R
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.databinding.ActivityR2AudiobookBinding
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.extensions.getPublication
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.services.isRestricted
import timber.log.Timber

open class R2AudiobookActivity : AppCompatActivity(), CoroutineScope, IR2Activity, MediaPlayerCallback, VisualNavigator {

    override val currentLocator: StateFlow<Locator> get() = _currentLocator
    private val _currentLocator = MutableStateFlow(Locator(href = "#", type = ""))
    private lateinit var binding: ActivityR2AudiobookBinding

    private fun notifyCurrentLocation() {
        val locator = publication.readingOrder[currentResource].let { resource ->
            val progression = mediaPlayer
                .takeIf { it.duration > 0 }
                ?.let { it.currentPosition / it.duration }
                ?: 0.0

            // FIXME: Add totalProgression
            Locator(
                href = resource.href,
                type = resource.type ?: "audio/*",
                title = resource.title,
                locations = Locator.Locations(
                    fragments = listOf(
                        "t=${TimeUnit.MILLISECONDS.toSeconds(mediaPlayer.currentPosition.toLong())}"
                    ),
                    progression = progression
                )
            )
        }

        if (locator == _currentLocator.value) {
            return
        }

        _currentLocator.value = locator
        navigatorDelegate?.locationDidChange(navigator = this, locator = locator)
    }

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean {
        loadedInitialLocator = true
        currentResource = publication.readingOrder.indexOfFirstWithHref(locator.href) ?: return false
        mediaPlayer.goTo(currentResource)
        seek(locator.locations)

        binding.playPause.callOnClick()
        notifyCurrentLocation()

        return true
    }

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean {
        val locator = publication.locatorFromLink(link) ?: return false
        return go(locator)
    }

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean {
        if (currentResource < publication.readingOrder.size - 1) {
            currentResource++
        }

        mediaPlayer.next()
        binding.playPause.callOnClick()
        return true
    }

    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean {
        if (currentResource > 0) {
            currentResource--
        }

        mediaPlayer.previous()
        binding.playPause.callOnClick()
        return true
    }

    @Deprecated("Use `presentation.value.readingProgression` instead", replaceWith = ReplaceWith("presentation.value.readingProgression"))
    override val readingProgression: ReadingProgression
        get() = TODO("not implemented") // To change initializer of created properties use File | Settings | File Templates.

    @ExperimentalReadiumApi
    override val presentation: StateFlow<VisualNavigator.Presentation>
        get() = TODO("Not yet implemented")

    /**
     * Context of this scope.
     */
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    override lateinit var preferences: SharedPreferences
    override lateinit var publication: Publication
    override lateinit var publicationIdentifier: String
    override lateinit var publicationFileName: String
    override lateinit var publicationPath: String
    override var bookId: Long = -1

    var currentResource = 0

    var startTime = 0.0
    private var finalTime = 0.0

    private val forwardTime = 10000
    private val backwardTime = 10000

    lateinit var mediaPlayer: R2MediaPlayer
    private var loadedInitialLocator = false

    protected var navigatorDelegate: NavigatorDelegate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityR2AudiobookBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)

        publicationPath = intent.getStringExtra("publicationPath") ?: throw Exception("publicationPath required")
        publicationFileName = intent.getStringExtra("publicationFileName") ?: throw Exception("publicationFileName required")
        val baseUrl = intent.getStringExtra("baseUrl") ?: throw Exception("Intent extra `baseUrl` is required. Provide the URL returned by Server.addPublication()")

        publication = intent.getPublication(this)
        publicationIdentifier = publication.metadata.identifier ?: publication.metadata.title

        require(!publication.isRestricted) { "The provided publication is restricted. Check that any DRM was properly unlocked using a Content Protection." }

        title = null

        val readingOrderOverHttp = publication.readingOrder.map { it.withBaseUrl(baseUrl) }
        mediaPlayer = R2MediaPlayer(readingOrderOverHttp, this)

        Handler(mainLooper).postDelayed({

            if (this.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {

                if (!loadedInitialLocator) {
                    go(publication.readingOrder.first())
                }

                binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    /**
                     * Notification that the progress level has changed. Clients can use the fromUser parameter
                     * to distinguish user-initiated changes from those that occurred programmatically.
                     *
                     * @param seekBar The SeekBar whose progress has changed
                     * @param progress The current progress level. This will be in the range min..max where min
                     * @param fromUser True if the progress change was initiated by the user.
                     */
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        if (!fromUser) {
                            return
                        }
                        mediaPlayer.seekTo(progress)
                        if (DEBUG) Timber.d("progress $progress")
                    }

                    /**
                     * Notification that the user has started a touch gesture. Clients may want to use this
                     * to disable advancing the seekbar.
                     * @param seekBar The SeekBar in which the touch gesture began
                     */
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                        // do nothing
                        isSeekTracking = true
                        if (DEBUG) Timber.d("start tracking")
                    }

                    /**
                     * Notification that the user has finished a touch gesture. Clients may want to use this
                     * to re-enable advancing the seekbar.
                     * @param seekBar The SeekBar in which the touch gesture began
                     */
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        // do nothing
                        isSeekTracking = false
                        if (DEBUG) Timber.d("stop tracking")
                    }
                })

                binding.playPause.setOnClickListener {
                    mediaPlayer.let {
                        if (it.isPlaying) {
                            it.pause()
                        } else {
                            if (it.isPaused) {
                                it.resume()
                            } else {
                                it.startPlayer()
                            }
                            Handler(mainLooper).postDelayed(updateSeekTime, 100)
                        }
                        this.updateUI()
                    }
                }

                binding.playPause.callOnClick()

                binding.fastForward.setOnClickListener {
                    if (startTime.toInt() + forwardTime <= finalTime) {
                        startTime += forwardTime
                        mediaPlayer.seekTo(startTime)
                    }
                }

                binding.fastBack.setOnClickListener {
                    if (startTime.toInt() - backwardTime > 0) {
                        startTime -= backwardTime
                        mediaPlayer.seekTo(startTime)
                    }
                }

                binding.nextChapter.setOnClickListener {
                    goForward(false) {}
                }

                binding.prevChapter.setOnClickListener {
                    goBackward(false) {}
                }
            }
        }, 100)
    }

    private fun updateUI() {

        if (currentResource == publication.readingOrder.size - 1) {
            binding.nextChapter.isEnabled = false
            binding.nextChapter.alpha = .5f
        } else {
            binding.nextChapter.isEnabled = true
            binding.nextChapter.alpha = 1.0f
        }
        if (currentResource == 0) {
            binding.prevChapter.isEnabled = false
            binding.prevChapter.alpha = .5f
        } else {
            binding.prevChapter.isEnabled = true
            binding.prevChapter.alpha = 1.0f
        }

        val current = publication.readingOrder[currentResource]
        binding.chapterView.text = current.title

        if (mediaPlayer.isPlaying) {
            binding.playPause.setImageDrawable(ContextCompat.getDrawable(this@R2AudiobookActivity, R.drawable.ic_pause_white_24dp))
        } else {
            binding.playPause.setImageDrawable(ContextCompat.getDrawable(this@R2AudiobookActivity, R.drawable.ic_play_arrow_white_24dp))
        }

        finalTime = mediaPlayer.duration
        startTime = mediaPlayer.currentPosition

        binding.seekBar.max = finalTime.toInt()

        binding.chapterTime.text = String.format(
            "%d:%d",
            TimeUnit.MILLISECONDS.toMinutes(finalTime.toLong()),
            TimeUnit.MILLISECONDS.toSeconds(finalTime.toLong()) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(finalTime.toLong()))
        )

        binding.progressTime.text = String.format(
            "%d:%d",
            TimeUnit.MILLISECONDS.toMinutes(startTime.toLong()),
            TimeUnit.MILLISECONDS.toSeconds(startTime.toLong()) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(startTime.toLong()))
        )

        binding.seekBar.progress = startTime.toInt()

        notifyCurrentLocation()
    }

    private fun seek(locations: Locator.Locations) {
        if (!mediaPlayer.isPrepared) {
            pendingSeekLocation = locations
            return
        }

        pendingSeekLocation = null

        val time = locations.fragments.firstOrNull()?.let {
            var time = it
            if (time.startsWith("#t=") || time.startsWith("t=")) {
                time = time.substring(time.indexOf('=') + 1)
            }
            time
        }
        time?.let {
            mediaPlayer.seekTo(TimeUnit.SECONDS.toMillis(it.toLong()).toInt())
        } ?: run {
            val progression = locations.progression
            val duration = mediaPlayer.duration
            Timber.d("progression used")
            if (progression != null) {
                Timber.d("ready to seek")
                mediaPlayer.seekTo(progression * duration)
            }
        }
    }

    private var pendingSeekLocation: Locator.Locations? = null
    var isSeekTracking = false

    private fun seekIfNeeded() {
        pendingSeekLocation?.let { locations ->
            seek(locations)
        }
    }

    override fun onPrepared() {
        seekIfNeeded()
        Handler(mainLooper).postDelayed(updateSeekTime, 100)
        updateUI()
    }

    override fun onComplete(index: Int, currentPosition: Int, duration: Int) {
        if (currentResource == index && currentPosition > 0 && currentResource < publication.readingOrder.size - 1 && currentPosition >= duration - 200 && !isSeekTracking) {
            Handler(mainLooper).postDelayed({
                if (currentResource < publication.readingOrder.size - 1) {
                    currentResource++
                }
                mediaPlayer.next()
                binding.playPause.callOnClick()
            }, 100)
        } else if (currentPosition > 0 && currentResource == publication.readingOrder.size - 1) {
            mediaPlayer.pause()
            binding.playPause.setImageDrawable(ContextCompat.getDrawable(this@R2AudiobookActivity, R.drawable.ic_play_arrow_white_24dp))
        } else {
            mediaPlayer.pause()
            binding.playPause.setImageDrawable(ContextCompat.getDrawable(this@R2AudiobookActivity, R.drawable.ic_play_arrow_white_24dp))
        }
    }

    private val updateSeekTime = object : Runnable {
        override fun run() {
            if (mediaPlayer.isPrepared) {
                mediaPlayer.let {
                    startTime = it.mediaPlayer.currentPosition.toDouble()
                }
                binding.progressTime.text = String.format(
                    "%d:%d",
                    TimeUnit.MILLISECONDS.toMinutes(startTime.toLong()),
                    TimeUnit.MILLISECONDS.toSeconds(startTime.toLong()) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(startTime.toLong()))
                )
                binding.seekBar.progress = startTime.toInt()

                notifyCurrentLocation()

                Handler(mainLooper).postDelayed(this, 100)
            }
        }
    }

    override fun finish() {
        setResult(Activity.RESULT_OK, intent)
        super.finish()
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer.resume()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer.pause()
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.stop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val locator = data.getParcelableExtra("locator") as? Locator

                locator?.let {
                    // href is the link to the page in the toc
                    var href = locator.href

                    if (href.indexOf("#") > 0) {
                        href = href.substring(0, href.indexOf("#"))
                    }

                    var index = 0
                    for (resource in publication.readingOrder) {
                        if (resource.href.endsWith(href)) {
                            currentResource = index
                            break
                        }
                        index++
                    }
                    go(locator)
                }
            }
        }
    }
}

internal fun Link.withBaseUrl(baseUrl: String): Link {
    // Already an absolute URL?
    if (Uri.parse(href).scheme != null) {
        return this
    }

    check(!baseUrl.endsWith("/"))
    check(href.startsWith("/"))
    return copy(href = baseUrl + href)
}
