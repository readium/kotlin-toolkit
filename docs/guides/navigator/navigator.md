# Navigator

You can use a Readium Navigator to present the publication to the user. The `Navigator` renders resources on the screen and offers APIs and user interactions for navigating the contents.

:warning: Navigators do not have user interfaces besides the view that displays the publication. Applications are responsible for providing a user interface with bookmark buttons, a progress bar, etc.

## Default implementations

The Readium toolkit comes with several `Navigator` implementations for different publication profiles. Some are Android `Fragment`s, designed to be added to your view hierarchy, while others are chromeless and can be used in the background.

| Navigator                     | Supported publications                                                                 |
|-------------------------------|----------------------------------------------------------------------------------------|
| `EpubNavigatorFragment`       | `epub` profile (EPUB, Readium Web Publication)                                         |
| `PdfNavigatorFragment`        | `pdf` profile (PDF, LCP for PDF package)                                               |
| `ImageNavigatorFragment`      | `divina` profile (Zipped Comic Book, Readium Divina)                                   |
| `AudioNavigator`              | `audiobook` profile (Zipped Audio Book, Readium Audiobook, LCP for Audiobooks package) |
| `TtsNavigator`                | Any publication with a [`ContentService`](../content.md)                               |

To find out which Navigator is compatible with a publication, refer to its [profile](https://readium.org/webpub-manifest/profiles/). Use `publication.conformsTo()` to identify the publication's profile.

```kotlin
if (publication.conformsTo(Publication.Profile.EPUB)) {
    // Initialize an `EpubNavigatorFragment`.
}
```

### Navigator APIs

Navigators implement a set of shared interfaces to help reuse the reading logic across publication profiles. For example, instead of using specific implementations like `EpubNavigatorFragment`, use the `Navigator` interface to create a location history manager compatible with all Navigator types.

You can create custom Navigators and easily integrate them into your app with minimal modifications by implementing these interfaces.

#### `Navigator` interface

All Navigators implement the `Navigator` interface, which provides the foundation for navigating resources in a `Publication`. You can use it to move through the publication's content or to find the current position.

Note that this interface does not specify how the content is presented to the user.

#### `VisualNavigator` interface

Navigators rendering the content visually on the screen implement the `VisualNavigator` interface. This interface allows monitoring input events such as taps or keyboard strokes.

#### `OverflowableNavigator` interface

An `OverflowableNavigator` is a Visual Navigator whose content can extend beyond the viewport. This interface offers details about the overflow style, e.g., scrolled, scroll axis or the reading progression.

The user typically navigates through the publication by scrolling or tapping the viewport edges.

#### `MediaNavigator` interface

The `MediaNavigator` interface is implemented by Navigators rendering a publication as audio or video content. You can use it to control the playback or observe its status.

[Refer to the `MediaNavigator` guide for additional details](media-navigator.md).

##### `TimeBasedMediaNavigator` interface

A time-based `MediaNavigator` renders an audio or video content with time locations. It is suitable for audiobook or media overlays Navigators.

##### `TextAwareMediaNavigator` interface

A text-aware `MediaNavigator` synchronizes utterances (e.g. sentences) with their corresponding audio or video clips. It can be used for text-to-speech, media overlays, and subtitled Navigators.

#### `SelectableNavigator` interface

Navigators enabling users to select parts of the content implement `SelectableNavigator`. You can use it to extract the `Locator` and content of the selected portion.

#### `DecorableNavigator` interface

A Decorable Navigator is able to render decorations over a publication, such as highlights or margin icons.

[See the corresponding proposal for more information](https://readium.org/architecture/proposals/008-decorator-api.html).

## Instantiating a navigator

### Visual navigators

The Visual Navigators are implemented as `Fragment` and must be added to your Android view hierarchy to render the publication contents. 

#### `EpubNavigatorFragment`

Create an `EpubNavigatorFactory` using your `Publication` instance. Optionally, set custom defaults for user preferences.

```kotlin
val navigatorFactory = EpubNavigatorFactory(
    publication = publication,
    configuration = EpubNavigatorFactory.Configuration(
        defaults = EpubDefaults(
            pageMargins = 1.4
        )
    )
)
```

Then, you need to setup the `FragmentFactory` in your custom parent `Fragment`. Refer to `EpubReaderFragment` in the Test App for a complete example.

:point_up: This is one method to set up the `EpubNavigatorFragment` in your view hierarchy. Select the approach that suits your application best.

```kotlin
class EpubReaderFragment : Fragment(), EpubNavigatorFragment.Listener {

    lateinit var navigator: EpubNavigatorFragment
    private var binding: FragmentReaderBinding by viewLifecycle()

    override fun onCreate(savedInstanceState: Bundle?) {
        // You are responsible for creating/restoring the `NavigatorFactory`,
        // for example from an in-memory repository.
        // See `ReaderRepository` in the Test App for an example.
        val navigatorFactory = ...

        // You should restore the initial location from your view model.
        childFragmentManager.fragmentFactory =
            navigatorFactory.createFragmentFactory(
                initialLocator = viewModel.initialLocator,
                listener = this
            )

        // IMPORTANT: Set the `fragmentFactory` before calling `super`.
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReaderBinding.inflate(inflater, container, false)
        val view = binding.root
        val tag = "EpubNavigatorFragment"

        if (savedInstanceState == null) {
            childFragmentManager.commitNow {
                add(R.id.navigator_container, EpubNavigatorFragment::class.java, Bundle(), tag)
            }
        }

        navigator = childFragmentManager.findFragmentByTag(tag) as EpubNavigatorFragment

        return view
    }
}
```

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    >

    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/navigator_container"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        />
</FrameLayout>
```

#### `PdfNavigatorFragment`

Use the same approach as described with the `EpubNavigatorFragment`, using a `PdfNavigatorFactory` instead.

#### `ImageNavigatorFragment`

Use the same approach as described with the `EpubNavigatorFragment`, except that there is no `ImageNavigatorFactory`. Instead, you can build the `FragmentFactory` directly with:

```kotlin
childFragmentManager.fragmentFactory =
    ImageNavigatorFragment.createFactory(
        publication = publication,
        initialLocator = viewModel.initialLocator,
        listener = this
    )
```

#### Limitations of the current `Fragment` APIs

The current toolkit API has a limitation regarding the lifecycle of `Fragment`s. If your `Activity` is being recreated after Android terminates your application, you must still provide a `FragmentFactory` in `Activity.onCreate()`, even though you may no longer have access to a `Publication` or `NavigatorFactory` instance.

To work around this issue, we provide "dummy" factories that you can use to recover during the restoration, before immediately removing the fragment or finishing the activity. Here's an example with the `EpubNavigatorFragment`:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    val navigatorFactory = viewModel.navigatorFactory

    if (navigatorFactory == null) {
        // We provide a dummy fragment factory  if the Activity is restored after the
        // app process was killed because the view model is empty. In that case, finish
        // the activity as soon as possible and go back to the previous one.
        childFragmentManager.fragmentFactory = EpubNavigatorFragment.createDummyFactory()

        super.onCreate(savedInstanceState)

        requireActivity().finish()

        return
    }

    childFragmentManager.fragmentFactory =
        navigatorFactory.createFragmentFactory(...)

    super.onCreate(savedInstanceState)
}
```

### `AudioNavigator`

The `AudioNavigator` is chromeless and does not provide any user interface, allowing you to create your own custom UI.

First, create an instance of the `AudioNavigatorFactory`, with the audio engine provider you want to use.

```kotlin
val navigatorFactory = AudioNavigatorFactory(
    publication = publication,
    audioEngineProvider = ExoPlayerEngineProvider(
        application,
        defaults = ExoPlayerDefaults(
            pitch = 0.8
        )
    )
)
```

Then, simply request an instance of the `AudioNavigator` at the given initial location.

```kotlin
val navigator = navigatorFactory.createNavigator(initialLocator)
navigator.play()
```

### `TtsNavigator`

The text-to-speech navigator is very similar to the `AudioNavigator`.

```kotlin
val navigatorFactory = TtsNavigatorFactory(
    application,
    publication,
    defaults = AndroidTtsDefaults(
        pitch = 0.8
    )
)
if (navigatorFactory == null) {
    // This publication is not supported by the `TtsNavigator`.
    return
}

val navigator = navigatorFactory.createNavigator(initialLocator)
navigator.play()
```

## Navigating the contents of the publication

The `Navigator` interface offers various `go` APIs for navigating the publication. For instance:

* to a link from the `publication.tableOfContents` or `publication.readingOrder`: `navigator.go(Link)`
* to a locator from a search result: `navigator.go(Locator)`

Specialized interfaces add more navigation APIs. For instance, the `OverflowableNavigator` enables moving to previous or next pages using `goForward()` and `goBackward()`.

## Reading progression

### Saving and restoring the reading progression

Navigators don't store any data permanently. Therefore, it is your responsibility to save the last read location in your database and restore it when creating a new Navigator.

You can observe the current position in the publication with `Navigator.currentLocator`.

```kotlin
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            navigator.currentLocator
                .onEach { viewModel.saveReadingProgression(it) }
                .launchIn(this)
        }
    }
}
```

The `Locator` object may be serialized to JSON in your database, and deserialized to set the initial location when creating the navigator, as explained in [the previous section](#instantiating-a-navigator).

To restore the reading progression, pass the saved `Locator` to the `initialLocator` parameter when creating the navigator### Bookmarking the current location

### Bookmarking the current location

Use a Navigator's `currentLocator` property to persists the current position, for instance as a bookmark.

After the user selects a bookmark from your user interface, navigate to it using `navigator.go(bookmark.locator)`.

### Displaying a progression slider

To display a percentage-based progression slider, use the `locations.totalProgression` property of the `currentLocator`. This property holds the total progression across an entire publication.

Given a progression from 0 to 1, you can obtain a `Locator` object from the `Publication`. This can be used to navigate to a specific percentage within the publication.

```kotlin
publication.locateProgression(progression)?.let { locator ->
    navigator.go(locator)
}
```

### Displaying the number of positions

:warning: Readium does not have the concept of pages, as they are not useful when dealing with reflowable publications across different screen sizes. Instead, we use [**positions**](https://readium.org/architecture/models/locators/positions/) which remain stable even when the user changes the font size or device.

Not all Navigators provide positions, but most `VisualNavigator` implementations do. Verify if `publication.positions` is not empty to determine if it is supported.

To find the total positions in the publication, use `publication.positions().size`. You can get the current position with `navigator.currentLocator.value.locations.position`.

## Navigating with edge taps and keyboard arrows

Readium provides a `DirectionalNavigationAdapter` helper to automatically turn pages when the user hit the arrow and space keys on their keyboard or tap the edge of the screen.

It's easy to set it up with any implementation of `OverflowableNavigator`:

```kotlin
(navigator as? OverflowableNavigator)?.apply {
    addInputListener(DirectionalNavigationAdapter(this))
}
```

`DirectionalNavigationAdapter` offers a lot of customization options. Take a look at its API.

## User preferences

Readium Navigators support user preferences, such as font size or background color. Take a look at [the Preferences API guide](preferences.md) for more information.
