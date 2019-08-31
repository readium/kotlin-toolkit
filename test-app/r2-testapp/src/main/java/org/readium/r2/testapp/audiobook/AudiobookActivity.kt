package org.readium.r2.testapp.audiobook


import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_audiobook.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.anko.indeterminateProgressDialog
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.toast
import org.json.JSONObject
import org.readium.r2.shared.Locations
import org.readium.r2.shared.Locator
import org.readium.r2.shared.LocatorText
import org.readium.r2.shared.Publication
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.testapp.*
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext


class AudiobookActivity : AppCompatActivity(), MediaPlayerCallback, CoroutineScope {
    /**
     * Context of this scope.
     */
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    private var mediaPlayer: R2MediaPlayer? = null

    private var startTime = 0.0
    private var finalTime = 0.0

    private val forwardTime = 10000
    private val backwardTime = 10000

    private var currentResource: Int = 0

    private var bookId: Long = -1
    private lateinit var publicationPath: String
    private lateinit var epubName: String
    private lateinit var publication: Publication
    private lateinit var publicationIdentifier: String
    private lateinit var preferences: SharedPreferences
    private lateinit var bookmarksDB: BookmarksDatabase
    private lateinit var progress: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audiobook)

        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)

        bookId = intent.getLongExtra("bookId", -1)
        publicationPath = intent.getStringExtra("publicationPath")
        publication = intent.getSerializableExtra("publication") as Publication
        epubName = intent.getStringExtra("epubName")
        publicationIdentifier = publication.metadata.identifier

        //Setting cover
        if (intent.hasExtra("cover")) {
            val byteArray = intent.getByteArrayExtra("cover")
            val bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray!!.size)
            findViewById<ImageView>(R.id.imageView).setImageBitmap(bmp)
        }

        launch {
            menuDrm?.isVisible = intent.getBooleanExtra("drm", false)
        }

        bookmarksDB = BookmarksDatabase(this)

        title = null

        val locations = Locations.fromJSON(JSONObject(preferences.getString("$publicationIdentifier-documentLocations", "{}")))

        val index = preferences.getInt("$publicationIdentifier-document", 0)

        chapterView!!.text = publication.readingOrder[index].title
        progress = indeterminateProgressDialog(getString(R.string.progress_wait_while_preparing_audiobook))

        mediaPlayer = R2MediaPlayer(this, publication.readingOrder, progress, this)

        Handler().postDelayed({

            //Picasso.with(this).load(publication.links[1].href).into(imageView)

            mediaPlayer?.goTo(index)

            locations.progression?.let { progression ->
                mediaPlayer?.seekTo(progression)
                seekLocation = locations
                isSeekNeeded = true
            }

            currentResource = index

            seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                /**
                 * Notification that the progress level has changed. Clients can use the fromUser parameter
                 * to distinguish user-initiated changes from those that occurred programmatically.
                 *
                 * @param seekBar The SeekBar whose progress has changed
                 * @param progress The current progress level. This will be in the range min..max where min
                 * and max were set by [ProgressBar.setMin] and
                 * [ProgressBar.setMax], respectively. (The default values for
                 * min is 0 and max is 100.)
                 * @param fromUser True if the progress change was initiated by the user.
                 */
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (!fromUser) {
                        return
                    }
                    mediaPlayer?.seekTo(progress)

                    if(progress == seekBar?.max) {
                        // Next track
                    }
                }

                /**
                 * Notification that the user has started a touch gesture. Clients may want to use this
                 * to disable advancing the seekbar.
                 * @param seekBar The SeekBar in which the touch gesture began
                 */
                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    // do nothing
                }

                /**
                 * Notification that the user has finished a touch gesture. Clients may want to use this
                 * to re-enable advancing the seekbar.
                 * @param seekBar The SeekBar in which the touch gesture began
                 */
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    // do nothing
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

    override fun onPrepared() {
        seekIfNeeded()
        updateUI()
    }

    override fun onComplete(index: Int, currentPosition: Int, duration: Int) {
        if (currentResource == index && currentPosition > 0 && currentResource < publication.readingOrder.size - 1 && currentPosition >= duration - 200) {
            Handler().postDelayed({
                if (currentResource < publication.readingOrder.size - 1) {
                    currentResource++
                }
                mediaPlayer?.next()
                play_pause!!.callOnClick()
            }, 100)
        } else if (currentPosition > 0 && currentResource == publication.readingOrder.size - 1) {
            mediaPlayer?.stop()
        }
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
            play_pause!!.setImageDrawable(ContextCompat.getDrawable(this@AudiobookActivity, R.drawable.ic_pause_white_24dp))
        } else {
            play_pause!!.setImageDrawable(ContextCompat.getDrawable(this@AudiobookActivity, R.drawable.ic_play_arrow_white_24dp))
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
    fun seekIfNeeded() {
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

    private var menuDrm: MenuItem? = null
    private var menuToc: MenuItem? = null
    private var menuBmk: MenuItem? = null
    private var menuSettings: MenuItem? = null

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_audio, menu)
        menuDrm = menu?.findItem(R.id.drm)
        menuToc = menu?.findItem(R.id.toc)
        menuBmk = menu?.findItem(R.id.bookmark)
        menuSettings = menu?.findItem(R.id.settings)

        menuSettings?.isVisible = false
        menuDrm?.isVisible = false
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.toc -> {
                val intent = Intent(this, R2OutlineActivity::class.java)
                intent.putExtra("publication", publication)
                intent.putExtra("bookId", bookId)
                startActivityForResult(intent, 2)
                return true
            }
            R.id.settings -> {
                // TODO do we need any settings ?
                return true
            }
            R.id.drm -> {
                startActivityForResult(intentFor<DRMManagementActivity>("publication" to publicationPath), 1)
                return true
            }
            R.id.bookmark -> {
                val resourceIndex = currentResource.toLong()

                val resource = publication.readingOrder[currentResource]
                val resourceHref = resource.href?: ""
                val resourceType = resource.typeLink?: ""
                val resourceTitle = resource.title?: ""
                
                val bookmark = Bookmark(
                        bookId,
                        publicationIdentifier,
                        resourceIndex,
                        resourceHref,
                        resourceType,
                        resourceTitle,
                        Locations(progression = seekBar!!.progress.toDouble()),
                        LocatorText()
                )

                bookmarksDB.bookmarks.insert(bookmark)?.let {
                    launch {
                        toast("Bookmark added")
                    }
                } ?: run {
                    launch {
                        toast("Bookmark already exists")
                    }
                }

                return true
            }

            else -> return false
        }

    }

    private fun storeProgression(locations: Locations?) {
        storeDocumentIndex()
        val publicationIdentifier = publication.metadata.identifier
        preferences.edit().putString("$publicationIdentifier-documentLocations", locations?.toJSON().toString()).apply()
    }

    private fun storeDocumentIndex() {
        val documentIndex = currentResource
        preferences.edit().putInt("$publicationIdentifier-document", documentIndex).apply()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val locator = data.getSerializableExtra("locator") as Locator

                // Set the progression fetched
                storeProgression(locator.locations)

                // href is the link to the page in the toc
                var href = locator.href

                if (href.indexOf("#") > 0) {
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

                seekLocation = locator.locations

                isSeekNeeded = true

                mediaPlayer?.goTo(currentResource)
                play_pause!!.callOnClick()

                chapterView!!.text = publication.readingOrder[currentResource].title

            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private val updateSeekTime = object : Runnable {
        override fun run() {
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
        progress.dismiss()
    }

}




