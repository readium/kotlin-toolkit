package org.readium.r2.navigator.audiobook

import android.media.MediaPlayer
import org.readium.r2.shared.publication.Link

@Deprecated("Build your own UI upon AudiobookNavigator instead.", level = DeprecationLevel.ERROR)
open class R2AudiobookActivity

@Deprecated("Use the new MediaNavigator API.", level = DeprecationLevel.ERROR)
class R2MediaPlayer(private var items: List<Link>) :
    MediaPlayer.OnPreparedListener {

    override fun onPrepared(mp: MediaPlayer?) {
    }
}

@Deprecated("Use the new MediaNavigator API.", level = DeprecationLevel.ERROR)
interface MediaPlayerCallback {
    fun onPrepared()
    fun onComplete(index: Int, currentPosition: Int, duration: Int)
}
