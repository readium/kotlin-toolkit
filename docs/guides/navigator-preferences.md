# Configuring the Navigator

:warning: The Navigator Setting API is still experimental and currently only available with `EpubNavigatorFragment` and `PdfNavigatorFragment`.

Take a look at the [migration guide](../migration-guide.md) if you are already using the legacy EPUB settings.

## Overview

The Readium Navigator can be configured dynamically, as it implements the `Configurable` interface.

You cannot directly overwrite the Navigator settings. Instead, you submit a set of `Preferences` to the Navigator, which will then recalculate its settings and update the presentation.

For instance: "font size" is a **setting**, and the application can submit the font size value `150%` as a **preference**.

<img src="assets/settings-flow.svg">

```kotlin
// 1. Create a set of preferences.
val preferences = EpubPreferences(
    fontFamily = FontFamily.SERIF,
    fontSize = 2.0,
    publisherStyles = false
)

// 2. Submit the preferences, the Navigator will update its settings and the presentation.
navigator.submitPreferences(preferences)
```

### Editing preferences

To assist you in building a preferences user interface or modifying existing preferences, navigators can offer a `PreferencesEditor`. Each implementation includes rules for adjusting preferences, such as the supported values or ranges.

```kotlin
// 1. Create a preferences editor.
val editor = EpubNavigatorFactory(publication).createPreferencesEditor(preferences)
    
// 2. Modify the preferences through the editor.
editor.apply {
    fontFamily.set(FontFamily.SERIF)
    fontSize.increment()
    publisherStyles.toggle()
}

// 3. Submit the edited preferences
navigator.submitPreferences(editor.preferences)
```

### Preferences are low-level

Preferences are low-level technical properties. While some of them can be exposed directly to the user, such as the font size, others should not be displayed as-is.

For instance, in EPUB, we can simulate two pages side by side using the `columnCount` (`auto`, `1`, `2`) property for reflowable resources, and the `spread` (`auto`, `never`, `always`) property for fixed-layout publications. Rather than displaying both of these settings with all of their possible values in the user interface, you might prefer to show a single switch button to enable a dual-page mode, which will set both settings appropriately.

### Inactive settings

A setting may be inactive if its activation conditions are not met in a set of preferences. The Navigator will ignore inactive settings when updating its presentation. For example, with the EPUB navigator, the word spacing setting requires the publisher styles to be disabled in order to take effect.

You can check if a setting is effective for a set of preferences using the `PreferencesEditor`:

```kotlin
val editor = EpubNavigatorFactory(publication)
    .createPreferencesEditor(preferences)

editor.wordSpacing.isEffective
```

## Setting the initial Navigator preferences and app defaults

When opening a publication, you can immediately apply the user preferences by providing them to the Navigator constructor. The API for doing this varies depending on the Navigator implementation, but generally looks like this:

```kotlin
val navigatorFactory = EpubNavigatorFactory(
    publication = publication,
    configuration = EpubNavigatorFactory.Configuration(
        defaults = EpubDefaults(
            pageMargins = 1.5,
            scroll = true
        )
    )
)

navigatorFactory.createFragmentFactory(
    initialPreferences = EpubPreferences(
        language = "fr"
    )
)
```

The `defaults` are used as fallback values when the default Navigator settings are not suitable for your application.

## Build a user settings interface

:question: The following examples use [Jetpack Compose](https://developer.android.com/jetpack/compose), but could be implemented using regular Android views as well.

### `PreferencesEditor`

Although you could create and modify `Preferences` objects directly before submitting them to the Navigator, a `PreferenceEditor` can assist you by providing helpers for dealing with each preference type when building the user interface.

`PreferencesEditor` implementations are specific to each Navigator, but they all provide `Preference<T>` properties for every setting (e.g. theme or font size). 

### Stateless `UserPreferences` composable

You can use the `PreferencesEditor` type to determine which (media type agnostic) view to create.

```kotlin
@Composable
fun <P: Configurable.Preferences> UserPreferences(
    editor: PreferencesEditor<P>,
    commit: () -> Unit
) {
    Column {
        Button(
            onClick = {
                editor.clear()
                commit()
            },
        ) {
            Text("Reset")
        }

        when (editor) {
            is PsPdfKitPreferencesEditor ->
                FixedLayoutUserPreferences(
                    commit = commit,
                    scroll = editor.scroll,
                    fit = editor.fit,
                    pageSpacing = editor.pageSpacing,
                    ...
                )
            is EpubPreferencesEditor ->
                ReflowableUserPreferences(
                    commit = commit,
                    ...
                )
        }
    }
}
```

The `commit` parameter is a closure used to save the edited preferences to the data store, before submitting them to the Navigator.

:question: The individual `PsPdfKitPreferencesEditor` properties are passed to `FixedLayoutUserPreferences` to that it can be reused with other fixed-layout publication types, such as FXL EPUB or comic books.

### User settings composable for fixed-layout publications

This stateless composable displays the actual preferences for a fixed-layout publication. The `Preference` parameters are nullable as they might not be available at all times or for all media types. It delegates the rendering of individual preferences to more specific composables.

```kotlin
@Composable
fun FixedLayoutUserPreferences(
    commit: () -> Unit,
    scroll: Preference<Boolean>? = null,
    fit: EnumPreference<Fit>? = null,
    pageSpacing: RangePreference<Double>? = null
) {
    if (scroll != null) {
        SwitchItem("Scroll mode", scroll, commit)
    }

    if (fit != null) {
        MenuItem("Page fit", fit, commit) { value ->
            when (value) {
                Fit.WIDTH -> "Width"
                Fit.HEIGHT -> "Height"
                Fit.CONTAIN -> "Width and height"
                Fit.COVER -> "Cover"
            }
        }
    }

    if (pageSpacing != null) {
        StepperItem("Page spacing", pageSpacing, commit)
    }
}
```

### Composable for a boolean `Preference`

A `Preference<Boolean>` can be represented as a simple switch button.

```kotlin
@Composable
fun SwitchItem(
    title: String,
    preference: Preference<Boolean>, 
    commit: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .clickable {
                preference.toggle()
                commit() 
            },
        text = { Text(title) },
        trailing = {
            Switch(
                checked = preference.value ?: preference.effectiveValue,
                onCheckedChange = { checked ->
                    preference.set(checked)
                    commit()
                }
            )
        }
    )
}
```

This composable uses the helpers in `Preference<Boolean>` to edit the preference in two different ways:

* `toggle()` will reverse the current preference when tapping on the entire list item.
* `set(checked)` sets an explicit value provided by the `Switch`'s `onCheckedChange` callback.

### `value` vs `effectiveValue`, which one to use?

In the previous example, you may have noticed the use of `preference.value ?: preference.effectiveValue` for the current value.

* `value` holds the user-selected preference, which may be `null`.
* `effectiveValue` is the setting value that will actually be used by the Navigator once the preferences are submitted. It may be different from the user-selected value if it is incompatible or invalid.

This is a common pattern with this API because it is less confusing to display the user-selected value, even if it will not actually be used by the Navigator.

### Composable for a `RangePreference<T>`

A `RangePreference<T>` can be represented as a stepper component with decrement and increment buttons.

```kotlin
@Composable
fun  <T: Comparable<T>> StepperItem(
    title: String,
    preference: RangePreference<T>,
    commit: () -> Unit,
) {
    ListItem(
        text = { Text(title) },
        trailing = {
            Row {
                IconButton(
                    onClick = {
                        preference.decrement(setting)
                        commit()
                    }
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease")
                }

                val currentValue = preferences.value ?: preference.effectiveValue
                Text(preference.formatValue(currentValue))

                IconButton(
                    onClick = {
                        preference.increment(setting)
                        commit()
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase")
                }
            }
        },
    )
}
```

This composable uses the `increment()` and `decrement()` range helpers of `RangePreference`, but it is also possible to set a value manually.

Between the two buttons, we display the current value using the `RangeSetting<T>.formatValue()` helper. This will automatically format the value to a human-readable string, such as a percentage or a value with units (e.g. 30px).

### Composable for an `EnumPreference<T>`

An `EnumPreference<T>` is a preference accepting a closed set of values. It can be displayed using various UI components, such as:

* a dropdown menu for a large enum
* [segmented buttons](https://m3.material.io/components/segmented-buttons/overview) for a small one

In the following example, we chose a dropdown menu built with `preference.supportedValues`, which returns the allowed enum members.

```kotlin
@Composable
fun <T> MenuItem(
    title: String,
    preference: EnumPreference<T>,
    commit: () -> Unit,
    formatValue: (T) -> String
) {
    val currentValue = preference.value ?: preference.effectiveValue

    ListItem(
        text = { Text(title) },
        trailing = {
            DropdownMenuButton(
                text = { Text(formatValue(currentValue)) }
            ) {
                for (value in setting.values) {
                    DropdownMenuItem(
                        onClick = {
                            preference.set(setting, value)
                            commit()
                        }
                    ) {
                        Text(formatValue(value))
                    }
                }
            }
        },
    )
}

@Composable
fun DropdownMenuButton(
    text: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    fun dismiss() { isExpanded = false }

    OutlinedButton(
        onClick = { isExpanded = true },
    ) {
        text()
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            content(::dismiss)
        }
    }
}
```

## Save and restore the user preferences

Having a user settings screen is not useful if you cannot save and restore the selected preferences for future sessions. Each navigator includes a JSON serialization helper that you can choose to use or not.

```kotlin
val serializer = EpubPreferencesSerializer()

val jsonString = serializer.serialize(preferences)
```

When you are ready to restore the user preferences, construct a new `Preferences` object from the JSON string.

```kotlin
val preferences = serializer.deserialize(jsonString)
```

In the Test App, `UserPreferencesViewModel` delegates the preferences state hoisting and persistence to a `PreferencesManager`, which acts as a single source of truth.

### Splitting and merging preferences

The way you store user preferences can affect the available features. You could have, for example:

* A unique set of preferences for each publication.
* Preferences shared between publications with the same profile or media type (EPUB, PDF, etc.).
* Global preferences shared with all publications (e.g. theme).
* Several user setting profiles/themes that the user can switch between and modify independently.
* Some settings that are not stored as JSON and will need to be reconstructed (e.g. the publication language).

To assist you, the toolkit provides suggested filters for each navigator. You can combine preference filters with the `+` operator.

```kotlin
// The suggested filter for the preferences that should be tied to a
// publication and not shared:
val publicationFilter = EpubPublicationPreferencesFilter

// The suggested filter for the preferences that will be shared between
// publications of the same type.
// Note that in this example, we combine it with an inline custom filter
// to remove the `theme` preference which will be stored globally.
val sharedFilter = EpubSharedPreferencesFilter
    + { it.copy(theme = null) }

// A custom filter to extract the settings which be stored globally.
val globalFilter = PreferencesFilter<EpubPreferences> {
    EpubPreferences(theme = it.theme)
}

val publicationPrefs = preferencesFilter.filter(preferences)
val sharedPrefs = sharedFilter.filter(preferences)
val globalPrefs = globalFilter.filter(preferences)

// You can reconstruct the original preferences by combining the filtered ones.
val combinedPrefs = publicationPrefs + sharedPrefs + globalPrefs
```

:warning: Some preferences are closely tied to a specific publication and should never be shared between multiple publications, such as the language. It is recommended that you store these preferences separately per book, which is what the suggested filters will do if you use them.

## Appendix: Preference constraints

### EPUB

#### Reflowable vs fixed-layout

EPUB comes in two very different flavors: **reflowable** which allows a lot of customization, and **fixed-layout** which is similar to a PDF or a comic book. Depending on the EPUB being rendered, the Navigator will ignore some of the preferences.

| Setting              | Reflowable         | Fixed Layout       |
|----------------------|--------------------|--------------------|
| `backgroundColor`    | :white_check_mark: | :white_check_mark: |
| `columnCount`        | :white_check_mark: |                    |
| `fontFamily`         | :white_check_mark: |                    |
| `fontSize`           | :white_check_mark: |                    |
| `fontWeight`         | :white_check_mark: |                    |
| `hyphens`            | :white_check_mark: |                    |
| `imageFilter`        | :white_check_mark: |                    |
| `language`           | :white_check_mark: | :white_check_mark: |
| `letterSpacing`      | :white_check_mark: |                    |
| `ligatures`          | :white_check_mark: |                    |
| `lineHeight`         | :white_check_mark: |                    |
| `pageMargins`        | :white_check_mark: |                    |
| `paragraphIndent`    | :white_check_mark: |                    |
| `paragraphSpacing`   | :white_check_mark: |                    |
| `publisherStyles`    | :white_check_mark: |                    |
| `readingProgression` | :white_check_mark: | :white_check_mark: |
| `scroll`             | :white_check_mark: |                    |
| `spread`             |                    | :white_check_mark: |
| `textAlign`          | :white_check_mark: |                    |
| `textColor`          | :white_check_mark: |                    |
| `textNormalization`  | :white_check_mark: |                    |
| `theme`              | :white_check_mark: |                    |
| `typeScale`          | :white_check_mark: |                    |
| `verticalText`       | :white_check_mark: |                    |
| `wordSpacing`        | :white_check_mark: |                    |

#### Publisher styles

The following advanced preferences require `publisherStyles` to be explicitly set to `false`. Make sure you convey this in your user interface.

* `hyphens`
* `letterSpacing`
* `ligatures`
* `lineHeight`
* `paragraphIndent`
* `paragraphSpacing`
* `textAlign`
* `typeScale`
* `wordSpacing`

#### Scroll vs paginated

The `columnCount` preference is available only when in paginated mode (`scroll = false`).

#### Dark theme specific preferences

The `imageFilter` preference is available only in dark mode (`theme = Theme.DARK`).

#### Language specific preferences

Some preferences are not available for all languages and layout.

| Preference        | LTR                | RTL                | CJK |
|-------------------|--------------------|--------------------|-----|
| `paragraphIndent` | :white_check_mark: | :white_check_mark: |     |
| `textAlign`       | :white_check_mark: | :white_check_mark: |     |
| `letterSpacing`   | :white_check_mark: |                    |     |
| `wordSpacing`     | :white_check_mark: |                    |     |
| `hyphens`         | :white_check_mark: |                    |     |
| `ligatures`       |                    | :white_check_mark: |     |

### PDF (PSPDFKit)

#### Scroll vs paginated

Some preferences are available only in scroll or paginated mode (`scroll = false`).

| Preference        | Scroll             | Paginated          |
|-------------------|--------------------|--------------------|
| `offsetFirstPage` |                    | :white_check_mark: |
| `spread`          |                    | :white_check_mark: |
| `scrollAxis`      | :white_check_mark: |                    |

