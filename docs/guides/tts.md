# Text-to-speech

Text-to-speech can read aloud a publication using a synthetic voice. The Readium toolkit includes an implementation based on the [Android TTS engine](https://developer.android.com/reference/android/speech/tts/TextToSpeech), but it can be extended to use a different TTS engine.

## Glossary

* **utterance** - a single piece of text played by a TTS engine, such as a sentence
* **tokenizer** - algorithm splitting the publication text content into individual utterances, usually by sentences
* **engine** – a TTS engine takes an utterance and transforms it into audio using a synthetic voice
* **voice** – a synthetic voice is used by a TTS engine to speak a text in a way suitable for the language and region

## Getting started

:warning: Apps targeting Android 11 that use the native text-to-speech must declare `INTENT_ACTION_TTS_SERVICE` in the queries elements of their manifest.

```xml
<queries>
  <intent>
      <action android:name="android.intent.action.TTS_SERVICE" />
  </intent>
</queries>
```

The text-to-speech feature is implemented as a standalone `Navigator`, which can render any publication with a [Content Service](content.md), such as an EPUB. This means you don't need an `EpubNavigatorFragment` open to read the publication; you can use the TTS navigator in the background.

To get a new instance of `TtsNavigator`, first create an `AndroidTtsNavigatorFactory` to use the default Android TTS engine.

```kotlin
val factory = AndroidTtsNavigatorFactory(application, publication)
    ?: throw Exception("This publication cannot be played with the TTS navigator")

val navigator = factory.createNavigator()
navigator.play()
```

`TtsNavigator` implements `MediaNavigator`, so you can use all the APIs available for media-based playback. Check out the [dedicated user guide](media-navigator.md) to learn how to control `TtsNavigator` and observe playback notifications.

## Configuring the Android TTS navigator

The `AndroidTtsNavigator` implements [`Configurable`](navigator-preferences.md) and provides various settings to customize the text-to-speech experience.

```kotlin
navigator.submitPreferences(AndroidTtsPreferences(
    language = Language("fr"),
    pitch = 0.8f,
    speed = 1.5f
))
```

A `PreferencesEditor` is available to help you construct your user interface and modify the preferences.

```kotlin
val factory = AndroidTtsNavigatorFactory(application, publication)
    ?: throw Exception("This publication cannot be played with the TTS navigator")

val navigator = factory.createNavigator()

val editor = factory.createPreferencesEditor(preferences)
editor.pitch.increment()
navigator.submitPreferences(editor.preferences)
```

### Language preference

The language set in the preferences determines the default voice used and how the publication text content is tokenized – i.e. split in utterances.

By default, the TTS navigator uses any language explicitly set on a text element (e.g. `lang="fr"` in HTML) and, if none is set, it falls back on the language declared in the publication manifest. Providing an explicit language preference is useful when the publication language is incorrect or missing.

### Voices preference

The Android TTS engine supports multiple voices. To allow users to choose their preferred voice for each language, they are stored as a dictionary `Map<Language, AndroidTtsEngine.Voice.Id?>` in `AndroidTtsPreferences`.

Use the `voices` property of the `AndroidTtsNavigator` instance to get the full list of available voices.

Users don't expect to see all available voices at once, as they depend on the selected language. To get an `EnumPreference<AndroidTtsEngine.Voice.Id?>` based on the current `language` preference, you can use the following snippet.

```kotlin
// We remove the region to show all the voices for a given language, no matter the region (e.g. Canada, France).
val currentLanguage = editor.language.effectiveValue?.removeRegion()

val voice: EnumPreference<AndroidTtsEngine.Voice.Id?> = editor.voices
    .map(
        from = { voices ->
            currentLanguage?.let { voices[it] }
        },
        to = { voice ->
            currentLanguage
                ?.let { editor.voices.value.orEmpty().update(it, voice) }
                ?: editor.voices.value.orEmpty()
        }
    )
    .withSupportedValues(
        navigator.voices
            .filter { it.language.removeRegion() == currentLanguage }
            .map { it.id }
    )

fun <K, V> Map<K, V>.update(key: K, value: V?): Map<K, V> =
    buildMap {
        putAll(this@update)
        if (value == null) {
            remove(key)
        } else {
            put(key, value)
        }
    }
```

#### Installing missing voice data

:point_up: This only applies if you use the default `AndroidTtsEngine`.

If the device lacks the data necessary for the chosen voice, the user needs to manually download it. To do so, call the `AndroidTtsEngine.requestInstallVoice()` helper when the `AndroidTtsEngine.Error.LanguageMissingData` error occurs. This will launch the system voice download activity.

```kotlin
navigator.playback
    .onEach { playback ->
        (playback?.state as? TtsNavigator.State.Failure.EngineError<*>)
            ?.let { it.error as? AndroidTtsEngine.Error.LanguageMissingData }
            ?.let { error ->
                Timber.e("Missing data for language ${error.language}")
                AndroidTtsEngine.requestInstallVoice(context)
            }
    }
    .launchIn(viewModelScope)
```

## Synchronizing the TTS navigator with a visual navigator

`TtsNavigator` is a standalone navigator that can be used to play a publication in the background. However, most apps prefer to display the publication while it is being read aloud. To do this, you can open the publication with a visual navigator (e.g. `EpubNavigatorFragment`) alongside the `TtsNavigator`. Then, synchronize the progression between the two navigators and use the Decorator API to highlight the spoken utterances.

For concrete examples, take a look at `TtsViewModel` in the Test App.

### Starting the TTS from the visible page

To start the TTS from the currently visible page, you can use the `VisualNavigator.firstVisibleElementLocator()` API to feed the initial locator of the `TtsNavigator`.

```kotlin
val ttsNavigator = ttsNavigatorFactory.createNavigator(
    initialLocator = (navigator as? VisualNavigator)?.firstVisibleElementLocator()
)
```

### Highlighting the currently spoken utterance

To highlight the current utterance on the page, you can apply a `Decoration` on the utterance locator if the visual navigator implements `DecorableNavigator`.

```kotlin
val visualNavigator: DecorableNavigator

ttsNavigator.location
    .map { it.utteranceLocator }
    .distinctUntilChanged()
    .onEach { locator ->
        navigator.applyDecorations(listOf(
            Decoration(
                id = "tts-utterance",
                locator = locator,
                style = Decoration.Style.Highlight(tint = Color.RED)
            )
        ), group = "tts")
    }
    .launchIn(scope)
```

### Turning pages automatically

To keep the visual navigator in sync with the utterance being played, observe the navigator's current `location` as described above and use `navigator.go(location.utteranceLocator)`.

However, this won't turn pages in the middle of an utterance, which can be irritating when speaking a lengthy sentence that spans two pages. To tackle this issue, you can use `location.tokenLocator` when available. It is updated constantly while you speak each word of an utterance.

Jumping to the token locator for every word can significantly reduce performance. To address this, it is recommended to use [`throttleLatest`](https://github.com/Kotlin/kotlinx.coroutines/issues/1107#issuecomment-1083076517).


```kotlin
ttsNavigator.location
    .throttleLatest(1.seconds)
    .map { it.tokenLocator ?: it.utteranceLocator }
    .distinctUntilChanged()
    .onEach { locator ->
        navigator.go(locator, animated = false)
    }
    .launchIn(scope)
```

## Advanced customizations

### Utterance tokenizer

By default, the `TtsNavigator` splits the publication text into sentences, but you can supply your own tokenizer to customize how the text is divided.

For example, this will speak the content word by word:

```kotlin
val navigatorFactory = TtsNavigatorFactory(
    application, publication,
    tokenizerFactory = { language ->
        DefaultTextContentTokenizer(unit = TextUnit.Word, language = language)
    }
)
```

### Custom TTS engine

`TtsNavigator` is compatible with any TTS engine if you provide an adapter implementing the `TtsEngine` interface. For an example, take a look at `AndroidTtsEngine`.

```kotlin
val navigatorFactory = TtsNavigatorFactory(
    application, publication,
    engineProvider = MyEngineProvider()
)
```
