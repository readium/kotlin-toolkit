# Media Navigator

A `MediaNavigator` implementation can play media-based reading orders, such as audiobooks, text-to-speech rendition, and Media overlays. It enables you to reuse your UI, media controls, and logic related to media playback.

## Controlling the playback

A media navigator provides the API you need to pause or resume playback.

```kotlin
navigator.pause()
check(!navigator.playback.value.playWhenReady)

navigator.play()
check(navigator.playback.value.playWhenReady)
```

## Observing the playback changes

You can observe the changes in the playback with the `navigator.playback` flow property.

`playWhenReady` indicates whether the media is playing or will start playing once the required conditions are met (e.g. buffering). You will typically use this to change the icon of a play/pause button.

The `state` property gives more information about the status of the playback:

* `Ready` when the media is ready to be played if `playWhenReady` is true.
* `Ended` after reaching the end of the reading order items.
* `Buffering` if the navigator cannot play because the buffer is starved.
* `Error` occurs when an error preventing the playback happened.

By combining the two, you can determine if the media is really playing: `playWhenReady && state == Ready`.

Finally, you can use the `index` property to know which `navigator.readingOrder` item is set to be played.

```kotlin
navigator.playback
    .onEach { playback ->
        playPauseButton.toggle(playback.playWhenReady)
        
        val playingItem = navigator.readingOrder.items[playback.index]

        if (playback.state is MediaNavigator.State.Failure) {
            // Alert
        }
    }
    .launchIn(scope)
```

`MediaNavigator` implementations may provide additional playback properties.

## Specializations of `MediaNavigator`

### Audio Navigator

The `AudioNavigator` interface is a specialized version of `MediaNavigator` for publications based on pre-recorded audio resources, such as audiobooks. It provides additional time-based APIs and properties.

```kotlin
audioNavigator.playback
    .onEach { playback ->
        print("At duration ${playback.offset} in the resource, buffered ${playback.buffered}")
    }
    .launchIn(scope)

// Jump to a particular duration offset in the resource item at index 4.
audioNavigator.seek(index = 4, offset = 5.seconds)
```

### Text-aware Media Navigator

`TextAwareMediaNavigator` specializes `MediaNavigator` for media-based resources that are synchronized with text utterances, such as sentences. It offers additional APIs and properties to determine which utterances are playing. This interface is helpful for a text-to-speech or a Media overlays navigator.

```kotlin
textAwareNavigator.playback
    .onEach { playback ->
        print("Playing the range ${playback.range} in text ${playback.utterance}")
    }
    .launchIn(scope)
    
// Get additional context by observing the location instead of the playback.
textAwareNavigator.location
    .onEach { location ->
        // Highlight the portion of text being played.
        visualNavigator.applyDecorations(
            listOf(Decoration(
                locator = location.utteranceLocator,
                style = Decoration.Style.Highlight(tint = Color.RED)
            )),
            "highlight"
        )
    }
    .launchIn(scope)

// Skip the current utterance.
if (textAwareNavigator.hasNextUtterance()) {
    textAwareNavigator.goToNextUtterance()
}
```

## Background playback and media notification

The Readium Kotlin toolkit provides implementations of `MediaNavigator` powered by Jetpack media3. This allows for continuous playback in the background and displaying Media-style notifications with playback controls. 

To accomplish this, you must create your own `MediaSessionService`. Get acquainted with [the concept behind media3](https://developer.android.com/guide/topics/media/media3) first.

### Configuration

Add the following [Jetpack media3](https://developer.android.com/jetpack/androidx/releases/media3) dependencies to your `build.gradle`, after checking for the latest version.

```groovy
dependencies {
    implementation "androidx.media3:media3-common:1.0.2"
    implementation "androidx.media3:media3-session:1.0.2"
    implementation "androidx.media3:media3-exoplayer:1.0.2"
}
```

### Add the `MediaSessionService`

Create a new implementation of `MediaSessionService` in your application. For an example, take a look at `MediaService` in the Test App. You can access the media3 `Player` from the navigator with `navigator.asMedia3Player()`.

Don't forget to declare this new service in your `AndroidManifest.xml`.

```xml
<manifest ...>

    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <!-- If targeting Android SDK 34, you will need this permission -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />

    <application ...>
        ...

        <!-- Update android:name to match your service package -->
        <service android:name=".reader.MediaService" 
            android:enabled="true"
            android:exported="true"
            android:foregroundServiceType="mediaPlayback"
            tools:ignore="ExportedSerddvice"
            >

            <intent-filter>
                <action android:name="androidx.media3.session.MediaSessionService"/>
                <action android:name="androidx.media2.session.MediaSessionService"/>
                <action android:name="android.media.session.MediaSessionService" />
            </intent-filter>
        </service>
    </application>
</manifest>
```

### Customizing the notification metadata

By default, the navigators will use the publication's metadata to display playback information in the Media-style notification. If you want to customize this, for example by retrieving metadata from your database, you can provide a custom `MediaMetadataFactory` implementation when creating the navigator.

Here's an example for the `AndroidTtsNavigator`.

```kotlin
val navigatorFactory = AndroidTtsNavigatorFactory(
    application, publication,
    metadataProvider = { pub ->
        DatabaseMediaMetadataFactory(
            context = application,
            scope = application,
            bookId = bookId,
            trackCount = pub.readingOrder.size
        )
    }
)

/**
 * Factory of media3 metadata for the local publication with given [bookId].
 */
class DatabaseMediaMetadataFactory(
    private val context: Context,
    scope: CoroutineScope,
    private val bookId: Int,
    private val trackCount: Int
) : MediaMetadataFactory {

    private class Metadata(
        val title: String,
        val author: String,
        val cover: ByteArray
    )

    private val metadata: Deferred<Metadata?> = scope.async {
        Database.getInstance(context).bookDao().get(bookId)?.let { book ->
            Metadata(
                title = book.title,
                author = book.author,
                // Byte arrays will go cross processes and should be kept small
                cover = book.cover.scaleToFit(400, 400).toPng()
            )
        }
    }

    override suspend fun publicationMetadata(): MediaMetadata =
        builder()?.build() ?: MediaMetadata.EMPTY

    override suspend fun resourceMetadata(index: Int): MediaMetadata =
        builder()?.setTrackNumber(index)?.build() ?: MediaMetadata.EMPTY

    private suspend fun builder(): MediaMetadata.Builder? {
        val metadata = metadata.await() ?: return null

        return MediaMetadata.Builder()
            .setTitle(metadata.title)
            .setTotalTrackCount(trackCount)
            .setArtist(metadata.artist)
            // We can't yet directly use a `content://` or `file://` URI with `setArtworkUri`.
            // See https://github.com/androidx/media/issues/271
            .setArtworkData(metadata.cover, PICTURE_TYPE_FRONT_COVER) }
    }
}
```
