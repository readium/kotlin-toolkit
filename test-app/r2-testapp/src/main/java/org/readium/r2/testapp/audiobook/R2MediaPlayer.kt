package org.readium.r2.testapp.audiobook

import android.app.ProgressDialog
import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.readium.r2.shared.Link
import java.io.IOException


class R2MediaPlayer(private var items: MutableList<Link>, private var progress: ProgressDialog, private var callback: MediaPlayerCallback) : OnPreparedListener {

    private val uiScope = CoroutineScope(Dispatchers.Main)

    var mediaPlayer: MediaPlayer = MediaPlayer()

    val isPlaying: Boolean
        get() = mediaPlayer.isPlaying

    val duration: Double
        get() = mediaPlayer.duration.toDouble()

    val currentPosition: Double
        get() = mediaPlayer.currentPosition.toDouble()

    var isPaused: Boolean

    private var index: Int

    init {
        isPaused = false
        index = 0
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.release()
        }
        toggleProgress(true)
    }


    /**
     * Called when the media file is ready for playback.
     *
     * @param mp the MediaPlayer that is ready for playback
     */
    override fun onPrepared(mp: MediaPlayer?) {
        toggleProgress(false)
        this.start()
        callback.onPrepared()
    }

    fun startPlayer() {
        mediaPlayer.reset()
        try {
            mediaPlayer.setDataSource(Uri.parse(items[index].href).toString())
            mediaPlayer.setOnPreparedListener(this)
            mediaPlayer.prepareAsync()
            toggleProgress(true)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun toggleProgress(show: Boolean) {
        uiScope.launch {
            if (show) progress.show()
            else progress.hide()
        }
    }

    fun seekTo(progression: Any) {
        when (progression) {
            is Double -> mediaPlayer.seekTo(progression.toInt())
            is Int -> mediaPlayer.seekTo(progression)
            else -> mediaPlayer.seekTo(progression.toString().toInt())
        }
    }

    fun stop() {
        mediaPlayer.stop()
    }

    fun pause() {
        mediaPlayer.pause()
        isPaused = true
    }

    fun start() {
        mediaPlayer.start()
        isPaused = false
        mediaPlayer.setOnCompletionListener {
            callback.onComplete(index, it.currentPosition, it.duration)
        }
    }

    fun resume() {
        mediaPlayer.start()
        isPaused = false
    }

    fun goTo(index: Int) {
        this.index = index
        isPaused = false
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        toggleProgress(true)
    }

    fun previous() {
        index -= 1
        isPaused = false
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        toggleProgress(true)
    }

    fun next() {
        index += 1
        isPaused = false
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        toggleProgress(true)
    }

}

interface MediaPlayerCallback {
    fun onPrepared()
    fun onComplete(index: Int, currentPosition: Int, duration: Int)
}


