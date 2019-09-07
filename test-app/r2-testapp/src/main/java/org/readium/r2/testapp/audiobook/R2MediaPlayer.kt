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

class R2MediaPlayer(var mediaActivity: AudiobookActivity, var items: MutableList<Link>, var progress: ProgressDialog, var callback: MediaPlayerCallback) : OnPreparedListener {

    private val uiScope = CoroutineScope(Dispatchers.Main)

    var mediaPlayer: MediaPlayer = MediaPlayer()

    val isPlaying: Boolean
        get() = mediaPlayer.isPlaying

    val duration: Double
        get() = if (isPrepared) {mediaPlayer.duration.toDouble()}else {0.0}

    val currentPosition: Double
        get() = if (isPrepared) {mediaPlayer.currentPosition.toDouble()}else {0.0}

    var isPaused: Boolean
    var isPrepared: Boolean

    var index:Int

    init {
        isPaused = false
        isPrepared = false
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
        isPrepared = true
    }

    fun startPlayer() {
        mediaPlayer.setOnPreparedListener(this)
        mediaPlayer.reset()
        try {
            mediaPlayer.setDataSource(mediaActivity, Uri.parse(items[index].href))
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

    fun toggleProgress(show: Boolean) {
        uiScope.launch {
            if (show) progress.show()
            else progress.hide()
        }
    }

    fun seekTo(progression: Any) {
        if (isPrepared) {
            when (progression) {
                is Double -> mediaPlayer.seekTo(progression.toInt())
                is Int -> mediaPlayer.seekTo(progression)
                else -> mediaPlayer.seekTo(progression.toString().toInt())
            }
        }
    }

    fun stop() {
        if (isPrepared) {
            mediaPlayer.stop()
            isPrepared = false
        }
    }

    fun pause() {
        if (isPrepared) {
            mediaPlayer.pause()
            isPaused = true
        }
    }

    fun release() {
        mediaPlayer.release()
    }

    fun start() {
        mediaPlayer.start()
        isPaused = false
        isPrepared = false
        mediaPlayer.setOnCompletionListener {
            callback.onComplete(index, it.currentPosition, it.duration)
        }
    }

    fun resume() {
        if (isPrepared) {
            mediaPlayer.start()
            isPaused = false
        }
    }

    fun goTo(index: Int) {
        this.index = index
        isPaused = false
        isPrepared = false
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        toggleProgress(true)
    }

    fun previous() {
        index -= 1
        isPaused = false
        isPrepared = false
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        toggleProgress(true)
    }
    fun next() {
        index += 1
        isPaused = false
        isPrepared = false
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        toggleProgress(true)
    }


}

interface MediaPlayerCallback {
    fun onPrepared()
}


