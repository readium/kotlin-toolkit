/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Process
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.widget.Toast
import androidx.media.MediaBrowserServiceCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.readium.r2.navigator.ExperimentalAudiobook
import org.readium.r2.navigator.extensions.let
import org.readium.r2.navigator.extensions.splitAt
import org.readium.r2.navigator.media.extensions.publicationId
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.services.cover
import timber.log.Timber
import kotlin.reflect.KMutableProperty0

/**
 * [MediaBrowserServiceCompat] implementation holding the current [MediaSessionNavigator] for
 * background playback.
 *
 * You should override this service in your app and declare it in your AndroidManifest.xml.
 *
 * See https://developer.android.com/guide/topics/media-apps/audio-app/building-a-mediabrowserservice
 */
@ExperimentalAudiobook
@OptIn(ExperimentalCoroutinesApi::class)
open class MediaService : MediaBrowserServiceCompat(), CoroutineScope by MainScope() {

    /**
     * Creates the instance of [MediaPlayer] which will be used for playing the given [media].
     *
     * The default implementation uses ExoPlayer.
     */
    open fun onCreatePlayer(mediaSession: MediaSessionCompat, media: PendingMedia): MediaPlayer =
        ExoMediaPlayer(this, mediaSession, media)

    /**
     * Called when the underlying [MediaPlayer] was stopped.
     */
    open fun onPlayerStopped() {}

    /**
     * Creates the [PendingIntent] which will be used to start the media activity when the user
     * activates the media notification.
     */
    open suspend fun onCreateNotificationIntent(publicationId: PublicationId, publication: Publication): PendingIntent? = null

    /**
     * Creates the [MediaPlayer.NotificationMetadata] for the given resource [link].
     *
     * The metadata will be used for the media-style notification.
     */
    open fun onCreateNotificationMetadata(publicationId: PublicationId, publication: Publication, link: Link): MediaPlayer.NotificationMetadata =
        MediaPlayer.NotificationMetadata(publication, link)

    /**
     * Returns the cover for the given [publication] which should be used in media notifications.
     */
    open suspend fun coverOfPublication(publicationId: PublicationId, publication: Publication): Bitmap? =
        publication.cover()

    /**
     * Handles a custom command delivered by [MediaSessionCompat.Callback.onCommand].
     *
     * @return Whether the custom command was handled.
     */
    open fun onCommand(command: String, args: Bundle?, cb: ResultReceiver?): Boolean = false

    /**
     * Called when a resource failed to be loaded, for example because the Internet connection
     * is offline and the resource is streamed.
     *
     * You should present the exception to the user.
     */
    open fun onResourceLoadFailed(link: Link, error: Resource.Exception) {
        Toast.makeText(this, error.getUserMessage(this), Toast.LENGTH_LONG).show()
    }

    /**
     * Override to control which app can access the MediaSession through the MediaBrowserService.
     * By default, only our own app can.
     *
     * @param packageName The package name of the application which is requesting access.
     * @param uid The UID of the application which is requesting access.
     */
    open fun isClientAuthorized(packageName: String, uid: Int): Boolean =
        (uid == Process.myUid())

    protected val mediaSession: MediaSessionCompat get() = getMediaSession(this, javaClass)

    private var player: MediaPlayer? = null
        set(value) {
            field?.apply {
                onDestroy()
                listener = null
            }

            field = value?.apply {
                listener = mediaPlayerListener
            }

            currentNavigator.value?.player = value
        }


    private var notificationId: Int? = null
    private var notification: Notification? = null

    private val mediaPlayerListener = object : MediaPlayer.Listener {

        /**
         * MediaSession works with media IDs associated with a bundle of extras. We map this to
         * a [Publication] by using for the media ID `publicationId#resourceHref`, and then putting
         * a locator in a `locator` extra.
         */
        override fun locatorFromMediaId(mediaId: String, extras: Bundle?): Locator? {
            val navigator = currentNavigator.value ?: return null
            val (publicationId, href) = mediaId.splitAt("#")

            if (navigator.publicationId != publicationId) {
                return null
            }

            val locator = (extras?.getParcelable(EXTRA_LOCATOR) as? Locator)
                ?: href?.let { navigator.publication.linkWithHref(it)?.toLocator() }

            if (locator != null && href != null && locator.href != href) {
                Timber.e("Ambiguous playback location provided. HREF `$href` doesn't match locator $locator.")
            }

            return locator
        }

        override suspend fun coverOfPublication(publication: Publication, publicationId: PublicationId): Bitmap? =
            this@MediaService.coverOfPublication(publicationId, publication)

        override fun onNotificationPosted(notificationId: Int, notification: Notification) {
            this@MediaService.notificationId = notificationId
            this@MediaService.notification = notification
            startForeground(notificationId, notification)
        }

        override fun onNotificationCancelled(notificationId: Int) {
            this@MediaService.notificationId = null
            this@MediaService.notification = null
            stopForeground(true)

            if (currentNavigator.value?.isPlaying == false) {
                onPlayerStopped()
            }
        }

        override fun onCreateNotificationMetadata(publication: Publication, publicationId: PublicationId, link: Link): MediaPlayer.NotificationMetadata =
            this@MediaService.onCreateNotificationMetadata(publicationId, publication, link)

        override fun onCommand(command: String, args: Bundle?, cb: ResultReceiver?): Boolean =
            this@MediaService.onCommand(command, args, cb)

        override fun onPlayerStopped() {
            mediaSession.publicationId = null
            player = null
            currentNavigator.value = null
            this@MediaService.onPlayerStopped()
        }

        override fun onResourceLoadFailed(link: Link, error: Resource.Exception) {
            this@MediaService.onResourceLoadFailed(link, error)
        }

    }

    // Service

    override fun onCreate() {
        super.onCreate()

        sessionToken = mediaSession.sessionToken
        mediaSession.isActive = true

        launch {
            pendingNavigator.receiveAsFlow().collect {
                player = onCreatePlayer(mediaSession, it.media)
                mediaSession.publicationId = it.media.publicationId
                currentNavigator.value = it.navigator
            }
        }

        launch {
            currentNavigator.collect { nav ->
                nav?.player = player

                // Set the activity intent to be started when the user tap on the media notification.
                mediaSession.setSessionActivity(
                    nav?.let { onCreateNotificationIntent(it.publicationId, it.publication) }
                )
            }
        }

        // Ensure the [MediaService] won't be destroyed by Android when there's some audio playing
        // in the background.
        launch {
            currentNavigator
                .flatMapLatest { navigator ->
                    navigator?.playback?.map { it.state }
                        ?: flowOf(MediaPlayback.State.Idle)
                }
                .distinctUntilChanged()
                .collect {
                    if (it.isPlaying) {
                        let(notificationId, notification) { id, note ->
                            startForeground(id, note)
                        }
                    } else {
                        stopForeground(false)
                    }
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        cancel()
        releaseMediaSession()
        currentNavigator.value = null
        player = null
    }

    // MediaBrowserServiceCompat

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        if (!isClientAuthorized(packageName = clientPackageName, uid = clientUid)) {
            return null
        }

        return BrowserRoot(ROOT_ID, null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(mutableListOf())
    }

    companion object {

        internal const val EVENT_PUBLICATION_CHANGED = "org.readium.r2.navigator.EVENT_PUBLICATION_CHANGED"
        internal const val EXTRA_PUBLICATION_ID = "org.readium.r2.navigator.EXTRA_PUBLICATION_ID"

        @Volatile private var connection: Connection? = null
        @Volatile private var mediaSession: MediaSessionCompat? = null

        private val currentNavigator = MutableStateFlow<MediaSessionNavigator?>(null)
        private val pendingNavigator = Channel<PendingNavigator>(Channel.CONFLATED)

        val navigator = currentNavigator.asStateFlow()

        fun connect(serviceClass: Class<*> = MediaService::class.java): Connection =
            createIfNull(this::connection, this) {
                Connection(serviceClass)
            }

        private fun getMediaSession(context: Context, serviceClass: Class<*>): MediaSessionCompat =
            createIfNull(this::mediaSession, this) {
                MediaSessionCompat(context, /* log tag */ serviceClass.simpleName)
            }

        private fun releaseMediaSession() {
            mediaSession?.apply {
                isActive = false
                release()
            }
            mediaSession = null
        }

    }

    /**
     * Connection to any running [MediaService] instance.
     *
     * Use a [Connection] to get a [MediaSessionNavigator] from a [Publication].
     * It will start the service if needed.
     */
    class Connection internal constructor(private val serviceClass: Class<*>) {

        val currentNavigator: StateFlow<MediaSessionNavigator?> get() = navigator

        fun getNavigator(context: Context, publication: Publication, publicationId: PublicationId, initialLocator: Locator?): MediaSessionNavigator {
            context.startService(Intent(context, serviceClass))

            currentNavigator.value
                ?.takeIf { it.publicationId == publicationId }
                ?.let { return it }

            val navigator = MediaSessionNavigator(publication, publicationId, getMediaSession(context, serviceClass).controller)
            pendingNavigator.trySend(PendingNavigator(
                navigator = navigator,
                media = PendingMedia(publication, publicationId, locator = initialLocator ?: publication.readingOrder.first().toLocator())
            ))

            return navigator
        }

    }

    private class PendingNavigator(val navigator: MediaSessionNavigator, val media: PendingMedia)

}

// FIXME: Move to r2-shared
internal fun <T> createIfNull(property: KMutableProperty0<T?>, owner: Any, factory: () -> T): T =
    property.get() ?: synchronized(owner) {
        property.get() ?: factory().also {
            property.set(it)
        }
    }

private const val ROOT_ID = "/"
private const val EXTRA_LOCATOR = "locator"
