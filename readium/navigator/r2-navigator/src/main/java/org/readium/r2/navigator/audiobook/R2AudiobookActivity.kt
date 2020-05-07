package org.readium.r2.navigator.audiobook

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import kotlinx.android.synthetic.main.activity_r2_audiobook.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.readium.r2.navigator.*
import org.readium.r2.navigator.BuildConfig.*
import org.readium.r2.shared.extensions.destroyPublication
import org.readium.r2.shared.extensions.getPublication
import org.readium.r2.shared.publication.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

open class R2AudiobookActivity : AppCompatActivity(), CoroutineScope, IR2Activity, MediaPlayerCallback, VisualNavigator {

    override val currentLocation: Locator? get() =
        publication.readingOrder[currentResource].let { resource ->
            val progression = mediaPlayer
                ?.let { it.currentPosition / it.duration }
                ?: 0.0

            // FIXME: Add totalProgression
            Locator(
                href = resource.href,
                type = resource.type ?: "audio/*",
                title = resource.title,
                locations = Locator.Locations(
                    fragments = listOf(
                        "t=${mediaPlayer?.currentPosition?.toInt() ?: 0}"
                    ),
                    progression = progression
                )
            )
        }

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean {
        val resourceIndex = publication.readingOrder.indexOfFirstWithHref(locator.href)
            ?: return false

        currentResource = resourceIndex
        mediaPlayer?.goTo(currentResource)
        return true
    }

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean {
        if (currentResource < publication.readingOrder.size - 1) {
            currentResource++
        }

        mediaPlayer?.next()
        play_pause!!.callOnClick()
        return true
    }

    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean {
        if (currentResource > 0) {
            currentResource--
        }

        mediaPlayer?.previous()
        play_pause!!.callOnClick()
        return true
    }

    override val readingProgression: ReadingProgression
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun goLeft(animated: Boolean, completion: () -> Unit): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun goRight(animated: Boolean, completion: () -> Unit): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

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

    var mediaPlayer: R2MediaPlayer? = null

    protected var navigatorDelegate: NavigatorDelegate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_r2_audiobook)

        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)

        publicationPath = intent.getStringExtra("publicationPath") ?: throw Exception("publicationPath required")
        publicationFileName = intent.getStringExtra("publicationFileName") ?: throw Exception("publicationFileName required")

        publication = intent.getPublication(this)
        publicationIdentifier = publication.metadata.identifier!!

        title = null

        Handler().postDelayed({
            
        if (this.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {

            mediaPlayer = R2MediaPlayer(publication.readingOrder, this)

            mediaPlayer?.goTo(currentResource)

            currentLocation?.locations?.progression?.let { progression ->
                mediaPlayer?.seekTo(progression)
                seekLocation = currentLocation?.locations
                isSeekNeeded = true
            }

            seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                /**
                 * Notification that the progress level has changed. Clients can use the fromUser parameter
                 * to distinguish user-initiated changes from those that occurred programmatically.
                 *
                 * @param seekBar The SeekBar whose progress has changed
                 * @param progress The current progress level. This will be in the range min..max where min
                 * @param fromUser True if the progress change was initiated by the user.
                 */
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (!fromUser) {
                        return
                    }
                    mediaPlayer?.seekTo(progress)
                    if (DEBUG) Timber.tag("AUDIO").d("progress $progress")
                }

                /**
                 * Notification that the user has started a touch gesture. Clients may want to use this
                 * to disable advancing the seekbar.
                 * @param seekBar The SeekBar in which the touch gesture began
                 */
                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    // do nothing
                    isSeekTracking = true
                    if (DEBUG) Timber.tag("AUDIO").d("start tracking")
                }

                /**
                 * Notification that the user has finished a touch gesture. Clients may want to use this
                 * to re-enable advancing the seekbar.
                 * @param seekBar The SeekBar in which the touch gesture began
                 */
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    // do nothing
                    isSeekTracking = false
                    if (DEBUG) Timber.tag("AUDIO").d("stop tracking")
                }

            })

            play_pause!!.setOnClickListener {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        it.pause()
                    } else {
                        if (it.isPaused) {
                            it.resume()
                        } else {
                            it.startPlayer()
                        }
                        Handler().postDelayed(updateSeekTime, 100)
                    }
                    this.updateUI()
                }
            }

            play_pause!!.callOnClick()

            fast_forward!!.setOnClickListener {
                if (startTime.toInt() + forwardTime <= finalTime) {
                    startTime += forwardTime
                    mediaPlayer?.seekTo(startTime)
                }
            }

            fast_back!!.setOnClickListener {
                if (startTime.toInt() - backwardTime > 0) {
                    startTime -= backwardTime
                    mediaPlayer?.seekTo(startTime)
                }
            }

            next_chapter!!.setOnClickListener {
                goForward(false) {}
            }

            prev_chapter!!.setOnClickListener {
                goBackward(false) {}
            }

        }
        }, 100)
    }

    private fun updateUI() {

        if (currentResource == publication.readingOrder.size - 1) {
            next_chapter!!.isEnabled = false
            next_chapter!!.alpha = .5f

        } else {
            next_chapter!!.isEnabled = true
            next_chapter!!.alpha = 1.0f
        }
        if (currentResource == 0) {
            prev_chapter!!.isEnabled = false
            prev_chapter!!.alpha = .5f

        } else {
            prev_chapter!!.isEnabled = true
            prev_chapter!!.alpha = 1.0f
        }

        val current = publication.readingOrder[currentResource]
        chapterView!!.text = current.title


        if (mediaPlayer!!.isPlaying) {
            play_pause!!.setImageDrawable(ContextCompat.getDrawable(this@R2AudiobookActivity, R.drawable.ic_pause_white_24dp))
        } else {
            play_pause!!.setImageDrawable(ContextCompat.getDrawable(this@R2AudiobookActivity, R.drawable.ic_play_arrow_white_24dp))
        }

        finalTime = mediaPlayer!!.duration
        startTime = mediaPlayer!!.currentPosition

        seekBar!!.max = finalTime.toInt()

        chapterTime!!.text = String.format("%d:%d",
                TimeUnit.MILLISECONDS.toMinutes(finalTime.toLong()),
                TimeUnit.MILLISECONDS.toSeconds(finalTime.toLong()) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(finalTime.toLong())))

        progressTime!!.text = String.format("%d:%d",
                TimeUnit.MILLISECONDS.toMinutes(startTime.toLong()),
                TimeUnit.MILLISECONDS.toSeconds(startTime.toLong()) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(startTime.toLong())))

        seekBar!!.progress = startTime.toInt()

        val resource = publication.readingOrder[currentResource]
        val resourceHref = resource.href
        val resourceType = resource.type ?: ""

        navigatorDelegate?.locationDidChange(locator = Locator(resourceHref, resourceType, publication.metadata.title, Locator.Locations(progression = seekBar!!.progress.toDouble())))

    }

    private var seekLocation: Locator.Locations? = null
    private var isSeekNeeded = false
    var isSeekTracking = false
    private fun seekIfNeeded() {
        if (isSeekNeeded) {
            val time = seekLocation?.fragments?.firstOrNull()?.let {
                var time = it
                if (time.startsWith("#t=") || time.startsWith("t=")) {
                    time = time.substring(time.indexOf('=') + 1)
                }
                time
            }
            time?.let {
                mediaPlayer?.seekTo(TimeUnit.SECONDS.toMillis(it.toLong()).toInt())
            } ?: run {
                seekLocation?.progression?.let { progression ->
                    mediaPlayer?.seekTo(progression)
                }
            }
            seekLocation = null
            isSeekNeeded = false
        }
    }

    override fun onPrepared() {
        seekIfNeeded()
        Handler().postDelayed(updateSeekTime, 100)
        updateUI()
    }

    override fun onComplete(index: Int, currentPosition: Int, duration: Int) {
        if (currentResource == index && currentPosition > 0 && currentResource < publication.readingOrder.size - 1 && currentPosition >= duration - 200 && !isSeekTracking) {
            Handler().postDelayed({
                if (currentResource < publication.readingOrder.size - 1) {
                    currentResource++
                }
                mediaPlayer?.next()
                play_pause!!.callOnClick()
            }, 100)
        } else if (currentPosition > 0 && currentResource == publication.readingOrder.size - 1) {
            mediaPlayer?.pause()
            play_pause!!.setImageDrawable(ContextCompat.getDrawable(this@R2AudiobookActivity, R.drawable.ic_play_arrow_white_24dp))
        } else {
            mediaPlayer?.pause()
            play_pause!!.setImageDrawable(ContextCompat.getDrawable(this@R2AudiobookActivity, R.drawable.ic_play_arrow_white_24dp))
        }
    }

    private val updateSeekTime = object : Runnable {
        override fun run() {
            if (mediaPlayer!!.isPrepared) {
                mediaPlayer?.let {
                    startTime = it.mediaPlayer.currentPosition.toDouble()
                }
                progressTime!!.text = String.format("%d:%d",
                        TimeUnit.MILLISECONDS.toMinutes(startTime.toLong()),
                        TimeUnit.MILLISECONDS.toSeconds(startTime.toLong()) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(startTime.toLong())))
                seekBar!!.progress = startTime.toInt()

                val resource = publication.readingOrder[currentResource]
                val resourceHref = resource.href
                val resourceType = resource.type ?: ""

                navigatorDelegate?.locationDidChange(locator = Locator(resourceHref, resourceType, publication.metadata.title, Locator.Locations(progression = seekBar!!.progress.toDouble())))

                Handler().postDelayed(this, 100)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer?.resume()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        intent.destroyPublication(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val locator = data.getParcelableExtra("locator") as Locator

                // Set the progression fetched
                navigatorDelegate?.locationDidChange(locator = locator)

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
                seekLocation = locator.locations

                isSeekNeeded = true

                mediaPlayer?.goTo(currentResource)

                play_pause!!.callOnClick()

                chapterView!!.text = publication.readingOrder[currentResource].title

            }
        }

    }

}
