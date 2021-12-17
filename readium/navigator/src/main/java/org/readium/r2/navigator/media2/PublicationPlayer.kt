package org.readium.r2.navigator.media2

import android.content.Context
import androidx.media2.common.SessionPlayer
import org.readium.r2.navigator.ExperimentalAudiobook
import org.readium.r2.shared.publication.Publication
import kotlin.time.ExperimentalTime

@ExperimentalAudiobook
interface PublicationPlayer {

    fun open(context: Context, publication: Publication): SessionPlayer?
}
