# Text-to-speech

:warning: The API described in this guide will be changed in the next version of the Kotlin toolkit to support background TTS playback and media notifications. It is recommended that you wait before integrating it in your app.

Text-to-speech can be used to read aloud a publication using a synthetic voice. The Readium toolkit ships with a TTS implementation based on the native [Android TTS engine](https://developer.android.com/reference/android/speech/tts/TextToSpeech), but it is opened for extension if you want to use a different TTS engine.

## Glossary

* **engine** – a TTS engine takes an utterance and transforms it into audio using a synthetic voice
* **rate** - speech speed of a synthetic voice
* **tokenizer** - algorithm splitting the publication text content into individual utterances, usually by sentences
* **utterance** - a single piece of text played by a TTS engine, such as a sentence
* **voice** – a synthetic voice is used by a TTS engine to speak a text using rules pertaining to the voice's language and region

## Reading a publication aloud

To read a publication, you need to create an instance of `PublicationSpeechSynthesizer`. It orchestrates the rendition of a publication by iterating through its content, splitting it into individual utterances using a `ContentTokenizer`, then using a `TtsEngine` to read them aloud. Not all publications can be read using TTS, therefore the constructor returns a nullable object. You can also check whether a publication can be played beforehand using `PublicationSpeechSynthesizer.canSpeak(publication)`.

```kotlin
val synthesizer = PublicationSpeechSynthesizer(
    publication = publication,
    config = PublicationSpeechSynthesizer.Configuration(
        rateMultiplier = 1.25
    ),
    listener = object : PublicationSpeechSynthesizer.Listener { ... }
)
```

Then, begin the playback from a given starting `Locator`. When missing, the playback will start from the beginning of the publication.

```kotlin
synthesizer.start()
```

You should now hear the TTS engine speak the utterances from the beginning. `PublicationSpeechSynthesizer` provides the APIs necessary to control the playback from the app:

* `stop()` - stops the playback ; requires start to be called again
* `pause()` - interrupts the playback temporarily
* `resume()` - resumes the playback where it was paused
* `pauseOrResume()` - toggles the pause
* `previous()` - skips to the previous utterance
* `next()` - skips to the next utterance

Look at `TtsControls` in the Test App for an example of a view calling these APIs.

:warning: Once you are done with the synthesizer, you should call `close()` to release held resources.

## Observing the playback state

The `PublicationSpeechSynthesizer` should be the single source of truth to represent the playback state in your user interface. You can observe the `synthesizer.state` property to keep your user interface synchronized with the playback. The possible states are:

* `Stopped` when idle and waiting for a call to `start()`.
* `Paused(utterance: Utterance)` when interrupted while playing `utterance`.
* `Playing(utterance: Utterance, range: Locator?)` when speaking `utterance`. This state is updated repeatedly while the utterance is spoken, updating the `range` property with the portion of utterance being played (usually the current word).

When pairing the `PublicationSpeechSynthesizer` with a `Navigator`, you can use the `utterance.locator` and `range` properties to highlight spoken utterances and turn pages automatically.

## Configuring the TTS

The `PublicationSpeechSynthesizer` offers some options to configure the TTS engine. Note that the support of each configuration option depends on the TTS engine used.

Update the configuration by setting it directly. The configuration is not applied right away but for the next utterance.

```kotlin
synthesizer.setConfig(synthesizer.config.copy(
    defaultLanguage = Language(Locale.FRENCH)
))
```

To keep your settings user interface up to date when the configuration changes, observe the `PublicationSpeechSynthesizer.config` property. Look at `TtsControls` in the Test App for an example of a TTS settings screen.

### Default language

The language used by the synthesizer is important, as it determines which TTS voices are used and the rules to tokenize the publication text content.

By default, `PublicationSpeechSynthesizer` will use any language explicitly set on a text element (e.g. with `lang="fr"` in HTML) and fall back on the global language declared in the publication manifest. You can override the fallback language with `Configuration.defaultLanguage` which is useful when the publication language is incorrect or missing.

### Speech rate

The `rateMultiplier` configuration sets the speech speed as a multiplier, 1.0 being the normal speed. The available range depends on the TTS engine and can be queried with `synthesizer.rateMultiplierRange`.

```kotlin
PublicationSpeechSynthesizer.Configuration(
    rateMultiplier = multiplier.coerceIn(synthesizer.rateMultiplierRange)
)
```

### Voice

The `voice` setting can be used to change the synthetic voice used by the engine. To get the available list, use `synthesizer.availableVoices`. Note that the available voices can change during runtime, observe `availableVoices` to keep your interface up to date.

To restore a user-selected voice, persist the unique voice identifier returned by `voice.id`.

Users do not expect to see all available voices at all time, as they depend on the selected language. You can group the voices by their language and filter them by the selected language using the following snippet.

```kotlin
// Supported voices grouped by their language.
val voicesByLanguage: Flow<Map<Language, List<Voice>>> =
    synthesizer.availableVoices
        .map { voices -> voices.groupBy { it.language } }

// Supported voices for the language selected in the configuration.
val voicesForSelectedLanguage: Flow<List<Voice>> =
    combine(
        synthesizer.config.map { it.defaultLanguage },
        voicesByLanguage,
    ) { language, voices ->
        language
            ?.let { voices[it] }
            ?.sortedBy { it.name ?: it.id }
            ?: emptyList()
    }
```

## Installing missing voice data

:point_up: This only applies if you use the default `AndroidTtsEngine`.

Sometimes the device does not have access to all the data required by a selected voice, in which case the user needs to download it manually. You can catch the `TtsEngine.Exception.LanguageSupportIncomplete` error and call `synthesizer.engine.requestInstallMissingVoice()` to start the system voice download activity.

```kotlin
val synthesizer = PublicationSpeechSynthesizer(context, publication)

synthesizer.listener = object : PublicationSpeechSynthesizer.Listener {
    override fun onUtteranceError( utterance: PublicationSpeechSynthesizer.Utterance, error: PublicationSpeechSynthesizer.Exception) {
        handle(error)
    }

    override fun onError(error: PublicationSpeechSynthesizer.Exception) {
        handle(error)
    }

    private fun handle(error: PublicationSpeechSynthesizer.Exception) {
        when (error) {
            is PublicationSpeechSynthesizer.Exception.Engine ->
                when (val err = error.error) {
                    is TtsEngine.Exception.LanguageSupportIncomplete -> {
                        synthesizer.engine.requestInstallMissingVoice(context)
                    }

                    else -> {
                        ...
                    }
                }
            }
        }
    }
```

## Synchronizing the TTS with a Navigator

While `PublicationSpeechSynthesizer` is completely independent from `Navigator` and can be used to play a publication in the background, most apps prefer to render the publication while it is being read aloud. The `Locator` core model is used as a means to synchronize the synthesizer with the navigator.

### Starting the TTS from the visible page

`PublicationSpeechSynthesizer.start()` takes a starting `Locator` for parameter. You can use it to begin the playback from the currently visible page in a `VisualNavigator` using `firstVisibleElementLocator()`.

```kotlin
val start = (navigator as? VisualNavigator)?.firstVisibleElementLocator()
synthesizer.start(fromLocator = start)
```

### Highlighting the currently spoken utterance

If you want to highlight or underline the current utterance on the page, you can apply a `Decoration` on the utterance locator with a `DecorableNavigator`.

```kotlin
val navigator: DecorableNavigator

synthesizer.state
    .map { (it as? State.Playing)?.utterance }
    .distinctUntilChanged()
    .onEach { utterance ->
        navigator.applyDecorations(listOf(
            Decoration(
                id = "tts-utterance",
                locator = utterance.locator,
                style = Decoration.Style.Highlight(tint = Color.RED)
            )
        ), group = "tts")
    }
    .launchIn(scope)
```

### Turning pages automatically

You can use the same technique as described above to automatically synchronize the `Navigator` with the played utterance, using `navigator.go(utterance.locator)`.

However, this will not turn pages mid-utterance, which can be annoying when speaking a long sentence spanning two pages. To address this, you can go to the `State.Playing.range` locator instead, which is updated regularly while speaking each word of an utterance. Note that jumping to the `range` locator for every word can severely impact performances. To alleviate this, you can throttle the flow using [`throttleLatest`](https://github.com/Kotlin/kotlinx.coroutines/issues/1107#issuecomment-1083076517).

```kotlin
synthesizer.state
    .filterIsInstance<State.Playing>()
    .map { it.range ?: it.utterance.locator }
    .throttleLatest(1.seconds)
    .onEach { locator ->
        navigator.go(locator, animated = false)
    }
    .launchIn(scope)
```

## Using a custom utterance tokenizer

By default, the `PublicationSpeechSynthesizer` will split the publication text into sentences to create the utterances. You can customize this for finer or coarser utterances using a different tokenizer.

For example, this will speak the content word-by-word:

```kotlin
val synthesizer = PublicationSpeechSynthesizer(context, publication,
    tokenizerFactory = { language ->
        TextContentTokenizer(
            defaultLanguage = language,
            unit = TextUnit.Word
        )
    }
)
```

For completely custom tokenizing or to improve the existing tokenizers, you can implement your own `ContentTokenizer`.

## Using a custom TTS engine

`PublicationSpeechSynthesizer` can be used with any TTS engine, provided they implement the `TtsEngine` interface. Take a look at `AndroidTtsEngine` for an example implementation.

```kotlin
val synthesizer = PublicationSpeechSynthesizer(publication,
    engineFactory = { listener -> MyCustomEngine(listener) }
)
```

