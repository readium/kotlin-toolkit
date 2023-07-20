# Navigator

## Overview

A navigator is a component of the [Readium architecture](https://readium.org/architecture/) used to render and navigate through the content of a publication. This is a core building block for adding a reader to your application.

The Readium toolkit comes with several navigator implementations that work with different publication formats. Some are Android `Fragment`s, designed to be added to your view hierarchy (e.g. `EpubNavigatorFragment`). Others are chromeless and can be used in the background, such as `TtsNavigator` and `AudioNavigator`.

|                          | EPUB               | PDF                | LCP PDF            | WebPub             | Audiobook          | CBZ                | Divina             |
|--------------------------|--------------------|--------------------|--------------------|--------------------|--------------------|--------------------|--------------------|
| `EpubNavigatorFragment`  | :heavy_check_mark: |                    |                    | :heavy_check_mark: |                    |                    |                    |
| `PdfNavigatorFragment`   |                    | :heavy_check_mark: | :heavy_check_mark: |                    |                    |                    |                    |
| `ImageNavigatorFragment` |                    |                    |                    |                    |                    | :heavy_check_mark: | :heavy_check_mark: |
| `AudioNavigator`         |                    |                    |                    |                    | :heavy_check_mark: |                    |                    |
| `TtsNavigator`           | :heavy_check_mark: |                    |                    |                    |                    |                    |                    |

### Navigator APIs

The navigators implement a set of shared interfaces to help reuse the reading logic across publication formats. For example, use the `Navigator` interface instead of specific implementations (e.g. `EpubNavigatorFragment`) to create a location history manager that works with all types of navigators.

You can write your own navigators and integrate them into your app with minimal changes by implementing the same interfaces.

#### `Navigator` interface

All navigators implement the `Navigator` interface, which provides the foundation for navigating resources in a `Publication`. Use it to traverse the content of the publication or observe the current location with `currentLocator`.

It does not specify how the content is presented to the user.

#### `VisualNavigator` interface

Navigators rendering the content visually on the screen should implement the `VisualNavigator` interface. It provides information about the nature of the presentation (e.g. scrolled, right-to-left, etc.) and can be used to observe input events, such as taps or keyboard strokes.

#### `MediaNavigator` interface

The `MediaNavigator` interface is implemented by navigators rendering a publication as audio or video content. You can use it to control the playback or observe its status.

[See the corresponding user guide for more information](media-navigator.md).

##### `TimeBasedMediaNavigator` interface

A time-based `MediaNavigator` renders an audio or video content with time locations. It is suited for audiobook or media overlay navigators.

##### `TextAwareMediaNavigator` interface

A text-aware `MediaNavigator` synchronizes utterances (e.g. sentences) with their corresponding audio or video clips. It can be used for text-to-speech, media overlays, and subtitled navigators.

#### `SelectableNavigator` interface

Navigators that enable users to select parts of the content, such as text or audio range selection, should implement `SelectableNavigator`. An app can use a selectable navigator to extract the `Locator` and content of the selected portion.

#### `DecorableNavigator` interface

A decorable navigator is able to render arbitrary decorations over a publication. It can be used to draw highlights over a publication. 

[See the corresponding proposal for more information](https://readium.org/architecture/proposals/008-decorator-api.html).

## Instantiating a navigator

### Visual navigators

The Visual navigators are implemented as `Fragment` and must be added to your Android view hierarchy.

#### `EpubNavigatorFragment`

First, create an `EpubNavigatorFactory` from your `Publication` instance. You can optionally provide custom defaults for the user preferences.

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

Then, you need to setup the `FragmentFactory` in your custom parent `Fragment`. See `EpubReaderFragment` in the Test App for a complete example.

:point_up: This is one way to set up the `EpubNavigatorFragment` in your view hierarchy. Choose what works best for your application.

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

### `AudioNavigator`

The audio navigator is chromeless and independent of the Android view hierarchy, making it much simpler to use than the visual navigators.

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

val navigator = navigatorFactory.createNavigator(initialLocator)
```

## Reading progression

### Saving and restoring the reading progression

Navigators don't store any data permanently. Therefore, it is your responsibility to save the last read location in your database and restore it when creating a new navigator.

At any time, you can read the `currentLocator` property of the navigator to know our current position in the publication. As it is a `StateFlow`, you can observe the changes and save it.

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

:point_up: `Locator` objects can be easily serialized to JSON by using `locator.toJSON().toString()`.

To restore the reading progression, pass the saved `Locator` to the `initialLocator` parameter when creating the navigator, as explained in [the previous section](#instantiating-a-navigator).

### Bookmarking the current location

Use a navigator's `currentLocator` property to persists the current location, for instance as a bookmark in your user interface.

When navigating to a bookmark, such as when the user selects it from the list, use `navigator.go(bookmark.locator)`.

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

Not all navigators offer positions, but most `VisualNavigator` implementations do.

To get the total number of positions in the publication, use `publication.positions().size`. While the current position can be obtained with `navigator.currentLocator.value.locations.position`.

## Navigating with edge taps and keyboard arrows

Readium provides a `DirectionalNavigationAdapter` helper to automatically turn pages when the user hit the arrow and space keys on their keyboard or tap the edge of the screen.

It's easy to set it up with any implementation of `VisualNavigator`:

```kotlin
navigator.addInputListener(DirectionalNavigationAdapter(
    animatedTransition = true
))
```

`DirectionalNavigationAdapter` offers a lot of customization options. Take a look at its API.
