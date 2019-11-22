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
import kotlinx.android.synthetic.main.activity_r2_audiobook.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
import org.readium.r2.navigator.R
import org.readium.r2.navigator.R2ActivityListener
import org.readium.r2.navigator.ReadingProgression
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.shared.Link
import org.readium.r2.shared.Locations
import org.readium.r2.shared.Locator
import org.readium.r2.shared.Publication
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

open class R2AudiobookActivity : AppCompatActivity(), CoroutineScope, R2ActivityListener, MediaPlayerCallback {

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

    lateinit var currentLocations: Locations
    var currentResource = 0

    var startTime = 0.0
    var finalTime = 0.0

    val forwardTime = 10000
    val backwardTime = 10000

    var mediaPlayer: R2MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_r2_audiobook)

        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)

        publicationPath = intent.getStringExtra("publicationPath")
        publication = intent.getSerializableExtra("publication") as Publication
        publicationFileName = intent.getStringExtra("publicationFileName")
        publicationIdentifier = publication.metadata.identifier!!

        currentLocations = Locations.fromJSON(JSONObject(preferences.getString("$publicationIdentifier-documentLocations", "{}")))
        currentResource = preferences.getInt("$publicationIdentifier-document", 0)

        title = null

        chapterView!!.text = publication.readingOrder[currentResource].title

        Handler().postDelayed({

            mediaPlayer = R2MediaPlayer(publication.readingOrder, this)

            mediaPlayer?.goTo(currentResource)

            currentLocations.progression?.let { progression ->
                mediaPlayer?.seekTo(progression)
                seekLocation = currentLocations
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
                    Timber.tag("AUDIO").d("progress $progress")
                }

                /**
                 * Notification that the user has started a touch gesture. Clients may want to use this
                 * to disable advancing the seekbar.
                 * @param seekBar The SeekBar in which the touch gesture began
                 */
                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    // do nothing
                    isSeekTracking = true
                    Timber.tag("AUDIO").d("start tracking")
                }

                /**
                 * Notification that the user has finished a touch gesture. Clients may want to use this
                 * to re-enable advancing the seekbar.
                 * @param seekBar The SeekBar in which the touch gesture began
                 */
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    // do nothing
                    isSeekTracking = false
                    Timber.tag("AUDIO").d("stop tracking")
                }

            })

            play_pause!!.setOnClickListener { view ->
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

            next_chapter!!.setOnClickListener { view ->
                if (currentResource < publication.readingOrder.size - 1) {
                    currentResource++
                }

                mediaPlayer?.next()
                play_pause!!.callOnClick()
            }

            prev_chapter!!.setOnClickListener { view ->
                if (currentResource > 0) {
                    currentResource--
                }

                mediaPlayer?.previous()
                play_pause!!.callOnClick()
            }

        }, 100)
    }

    fun storeProgression(locations: Locations?) {
        storeDocumentIndex()
        val publicationIdentifier = publication.metadata.identifier
        preferences.edit().putString("$publicationIdentifier-documentLocations", locations?.toJSON().toString()).apply()
    }

    private fun storeDocumentIndex() {
        val documentIndex = currentResource
        preferences.edit().putInt("$publicationIdentifier-document", documentIndex).apply()
    }

    fun updateUI() {

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

        storeProgression(Locations(progression = seekBar!!.progress.toDouble()))

    }

    var seekLocation: Locations? = null
    var isSeekNeeded = false
    var isSeekTracking = false
    private fun seekIfNeeded() {
        if (isSeekNeeded) {
            val time = seekLocation?.fragment?.let {
                var time = it
                if (time.startsWith("#t=")) {
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

                storeProgression(Locations(progression = seekBar!!.progress.toDouble()))

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val locator = data.getSerializableExtra("locator") as Locator

                // Set the progression fetched
                storeProgression(locator.locations)

                currentLocations = locator.locations!!

                // href is the link to the page in the toc
                var href = locator.href

                if (href!!.indexOf("#") > 0) {
                    href = href.substring(0, href.indexOf("#"))
                }

                var index = 0
                for (resource in publication.readingOrder) {
                    if (resource.href!!.endsWith(href)) {
                        currentResource = index
                        break
                    }
                    index++
                }
                seekLocation = currentLocations

                isSeekNeeded = true

                mediaPlayer?.goTo(currentResource)

                play_pause!!.callOnClick()

                chapterView!!.text = publication.readingOrder[currentResource].title

            }
        }

    }

}
