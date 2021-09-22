/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.readium.r2.navigator.ExperimentalAudiobook
import org.readium.r2.navigator.R
import org.readium.r2.navigator.audio.PublicationDataSource
import org.readium.r2.navigator.extensions.timeWithDuration
import org.readium.r2.shared.extensions.asInstance
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.*
import timber.log.Timber
import java.net.UnknownHostException
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * An implementation of [MediaPlayer] using ExoPlayer.
 */
@ExperimentalAudiobook
@OptIn(ExperimentalTime::class)
class ExoMediaPlayer(
    context: Context,
    mediaSession: MediaSessionCompat,
    media: PendingMedia,
    cache: Cache? = null
) : MediaPlayer, CoroutineScope by MainScope() {

    override var listener: MediaPlayer.Listener? = null

    private val publication: Publication = media.publication
    private val publicationId: PublicationId = media.publicationId

    private val dataSourceFactory by lazy {
        var factory: DataSource.Factory = PublicationDataSource.Factory(publication)

        if (cache != null) {
            factory = CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(factory)
                // Disable writing to the cache by the player. We'll handle downloads through the
                // service.
                .setCacheWriteDataSinkFactory(null)
        }

        factory
    }

    private val player: ExoPlayer = SimpleExoPlayer.Builder(context)
        .setSeekBackIncrementMs(Duration.seconds(30).inWholeMilliseconds)
        .setSeekForwardIncrementMs(Duration.seconds(30).inWholeMilliseconds)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .setAudioAttributes(AudioAttributes.Builder()
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()
        .apply {
            addListener(PlayerListener())
    //        addAnalyticsListener(EventLogger(null))
        }

    // FIXME: ExoPlayer's media session connector doesn't handle the playback speed yet, so I used a custom solution until we create our own connector
    override var playbackRate: Double
        get() = player.playbackParameters.speed.toDouble()
        set(speed) {
            val pitch = player.playbackParameters.pitch
            player.playbackParameters = PlaybackParameters(speed.toFloat(), pitch)
        }

    private val notificationManager =
        PlayerNotificationManager.Builder(
            context,
            MEDIA_NOTIFICATION_ID,
            MEDIA_CHANNEL_ID
        )
        .setChannelNameResourceId(R.string.r2_media_notification_channel_name)
        .setChannelDescriptionResourceId(R.string.r2_media_notification_channel_description)
        .setMediaDescriptionAdapter(DescriptionAdapter(mediaSession.controller, media))
        .setNotificationListener(NotificationListener())
        .setRewindActionIconResourceId(R.drawable.r2_media_notification_rewind)
        .setFastForwardActionIconResourceId(R.drawable.r2_media_notification_fastforward)
        .build()
        .apply {
            setMediaSessionToken(mediaSession.sessionToken)
            setPlayer(player)
            setSmallIcon(R.drawable.exo_notification_small_icon)
            setUsePlayPauseActions(true)
            setUseStopAction(false)
            setUseChronometer(false)
            setUseRewindAction(true)
            setUseRewindActionInCompactView(true)
        }

    private val mediaSessionConnector = MediaSessionConnector(mediaSession)

    init {
        mediaSessionConnector.apply {
            setPlaybackPreparer(PlaybackPreparer())
            setQueueNavigator(QueueNavigator(mediaSession))
            setPlayer(player)
        }

        prepareTracklist()
        seekTo(media.locator)
    }

    override fun onDestroy() {
        cancel()
        mediaSessionConnector.setPlayer(null)
        notificationManager.setPlayer(null)
        player.stop()
        player.clearMediaItems()
        player.release()
    }

    private fun prepareTracklist() {
        player.setMediaItems(publication.readingOrder.map { link ->
            MediaItem.fromUri(link.href)
        })
        player.prepare()
    }

    private fun seekTo(locator: Locator) {
        val readingOrder = publication.readingOrder
        val index = readingOrder.indexOfFirstWithHref(locator.href) ?: 0

        val duration = readingOrder[index].duration?.let { Duration.seconds(it) }
        val time = locator.locations.timeWithDuration(duration)
        player.seekTo(index, time?.inWholeMilliseconds ?: 0)
    }

    private inner class PlayerListener : Player.Listener {

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (playbackState == Player.STATE_IDLE) {
                listener?.onPlayerStopped()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            var resourceException: Resource.Exception? = error.asInstance<Resource.Exception>()
            if (resourceException == null && (error.cause as? HttpDataSource.HttpDataSourceException)?.cause is UnknownHostException) {
                resourceException = Resource.Exception.Offline
            }

            if (resourceException != null) {
                player.currentMediaItem?.mediaId
                    ?.let { href -> publication.linkWithHref(href) }
                    ?.let { link ->
                        listener?.onResourceLoadFailed(link, resourceException)
                    }
            } else {
                Timber.e(error)
            }
        }

    }

    private inner class PlaybackPreparer : MediaSessionConnector.PlaybackPreparer {

        override fun onCommand(player: Player, controlDispatcher: ControlDispatcher, command: String, extras: Bundle?, cb: ResultReceiver?): Boolean =
            listener?.onCommand(command, extras, cb) ?: false

        override fun getSupportedPrepareActions(): Long =
            PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
            PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID

        override fun onPrepare(playWhenReady: Boolean) {}

        override fun onPrepareFromMediaId(mediaId: String, playWhenReady: Boolean, extras: Bundle?) {
            val locator = listener?.locatorFromMediaId(mediaId, extras) ?: return
            player.playWhenReady = playWhenReady
            seekTo(locator)
        }

        override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) {}

        override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) {}

    }

    private inner class QueueNavigator(mediaSession: MediaSessionCompat) : TimelineQueueNavigator(mediaSession) {

        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat =
            createMediaMetadata(publication.readingOrder[windowIndex]).description

    }

    private inner class NotificationListener : PlayerNotificationManager.NotificationListener {

        override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
            if (ongoing) {
                listener?.onNotificationPosted(notificationId, notification)
            }
        }

        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            listener?.onNotificationCancelled(notificationId)
        }

    }

    private inner class DescriptionAdapter(private val controller: MediaControllerCompat, private val media: PendingMedia) : PlayerNotificationManager.MediaDescriptionAdapter {

        var cover: Bitmap? = null

        override fun createCurrentContentIntent(player: Player): PendingIntent? =
            controller.sessionActivity

        override fun getCurrentContentText(player: Player): CharSequence =
            publication.metadata.title

        override fun getCurrentContentTitle(player: Player): CharSequence =
            controller.metadata.description.title ?: publication.metadata.title

        override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback): Bitmap? {
            if (cover != null) {
                return cover
            }

            launch {
                cover = listener?.coverOfPublication(media.publication, media.publicationId)
                cover?.let { callback.onBitmap(it) }
            }
            return null
        }

    }

    private fun createMediaMetadata(link: Link): MediaMetadataCompat {
        val metadata = listener?.onCreateNotificationMetadata(publication, publicationId, link)
            ?: MediaPlayer.NotificationMetadata(publication, link)

        return MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "${publicationId}#${link.href}")
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, metadata.trackTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, metadata.publicationTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, metadata.authors)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, metadata.publicationTitle)
            .build()
    }
}

private const val MEDIA_CHANNEL_ID = "org.readium.r2.navigator.media"
private const val MEDIA_NOTIFICATION_ID = 0xb339 // Arbitrary number used to identify our notification
