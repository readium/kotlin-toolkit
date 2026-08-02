# New Web Navigators

The Readium toolkit offers new navigators based on [Jetpack Compose](https://developer.android.com/jetpack/compose): `ReflowableWebRendition` and `FixedWebRendition`.

Unlike the legacy `EpubNavigatorFragment`, these new navigators are built as Composable functions and provide separate state and controller objects for better integration with modern Android apps.

| Composable               | State                         | Supported publications |
|--------------------------|-------------------------------|------------------------|
| `ReflowableWebRendition` | `ReflowableWebRenditionState` | Reflowable EPUB        |
| `FixedWebRendition`      | `FixedWebRenditionState`      | Fixed-layout EPUB      |

> [!WARNING]
> These new navigators are still experimental and have not been battle tested. The API are subject to change and
> you may face bugs that didn't exist in the legacy EPUB navigator.


## Instantiating a Rendition

To use the new navigators, you first create a `Factory` for your publication type, then use it to create a `RenditionState`.

### 1. Create a Factory

The factory is responsible for creating the state.

```kotlin
val navigatorFactory = ReflowableWebRenditionFactory(
    application = application,
    publication = publication,
    configuration = ReflowableWebConfiguration(...)
)
```

### 2. Create the Rendition State

The `RenditionState` holds the internal state of the navigator and is used by the Composable.

```kotlin
val renditionState = navigatorFactory.createRenditionState(
    initialPreferences = initialPreferences,
    initialLocation = initialLocation
).getOrThrow()
```

### 3. Compose the Rendition

Finally, call the rendition Composable in your UI. The rendition will fill the maximum size provided by its parent.

```kotlin
ReflowableWebRendition(
    state = renditionState
)
```

## Input and Hyperlinks

You can observe user interactions by providing listeners to the rendition Composable.

```kotlin
val inputListener = object : InputListener {
    override fun onTap(event: TapEvent, context: TapContext) {
        // Handle tap
    }
}

val hyperlinkListener = object : HyperlinkListener {
    override fun onResourceActivated(url: Url, href: String): Boolean {
        // Handle link activation
        return true
    }
}
```


## Navigating the contents

The `RenditionState` provides a `controller` property that becomes available after the first composition. This controller implements `NavigationController` and other interfaces for interacting with the rendition.

```kotlin
val controller = renditionState.controller ?: return

// Navigate to a specific href
controller.goTo(url)

// Navigate forward or backward
controller.moveForward()
controller.moveBackward()
```

## User preferences

You update the user preferences through the `controller`, which implements `PreferencesController`.

### `ReflowableWebPreferences` and `FixedWebPreferences`

These classes hold the user-selected preferences (e.g., font size, theme). They are immutable data classes.

```kotlin
val preferences = ReflowableWebPreferences(
    fontSize = 1.5,
    scroll = true
)
```

### Updating Preferences

To update the preferences, simply assign a new `Preferences` object to the `controller.preferences` property. The controller will automatically resolve the new settings and update the rendition.

```kotlin
val controller = renditionState.controller ?: return
controller.preferences = controller.preferences.copy(
    fontSize = 2.0
)
```

### Observing Settings

If you need to access the resolved settings (the values actually used by the rendition after merging preferences with defaults and publication metadata), you can use the `controller.settings` property.

```kotlin
val currentSettings = controller?.settings
val actualFontSize = currentSettings?.fontSize
```

### Preference constraints

Depending on the publication, defaults and other preferences, some reflowable preferences might be ignored or have limited effect.


#### Scroll vs paginated

The `columnCount` preference is available only when in paginated mode (`scroll = false`).

#### Language specific preferences

Some preferences are not available for all languages and layout.

| Preference        | LTR                | RTL                | CJK |
|-------------------|--------------------|--------------------|-----|
| `paragraphIndent` | :white_check_mark: | :white_check_mark: |     |
| `textAlign`       | :white_check_mark: | :white_check_mark: |     |
| `ligatures`       | :white_check_mark: | :white_check_mark: |     |
| `letterSpacing`   | :white_check_mark: |                    |     |
| `wordSpacing`     | :white_check_mark: |                    |     |
| `hyphens`         | :white_check_mark: |                    |     |


## Decorations

The new navigators support decorations, allowing you to render highlights or other annotations over the content.

```kotlin
val highlights = listOf(
    ReflowableWebDecoration(
        id = Decoration.Id("highlight-1"),
        location = location,
        style = Decoration.Style.Highlight(tint = Color.Yellow)
    )
)

controller.decorations.set("highlights", highlights)
```
