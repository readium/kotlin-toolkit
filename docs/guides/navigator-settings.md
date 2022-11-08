# Configuring the Navigator

:warning: The Navigator Setting API is still experimental and currently only available with `EpubNavigatorFragment` and `PdfNavigatorFragment`.

Take a look at the [migration guide](../migration-guide.md) if you are already using the legacy EPUB settings.

## Overview

A few Readium components – such as the Navigator – support dynamic configuration through the `Configurable` interface.

The application cannot explicitly set the Navigator settings. Instead, you can submit a set of `Preferences` to the Navigator (`Configurable`) which will in turn recompute its settings and refresh the presentation.

For a concrete example: "font size" is a **setting**, the application can submit the font size value `150%` which is a **preference**.

<img src="assets/settings-flow.svg">

```kotlin
// 1. Create a set of preferences.
val preferences = EpubPreferences(
    fontFamily = FontFamily.SERIF,
    fontSize = 2,
    publisherStyles = false
)

// 2. Submit the preferences, the Navigator will update the presentation and its settings. 
navigator.submitPreferences(preferences)
```
In addition, Readium provides a `PreferencesEditor` for each `Configurable` navigator which enables you to easily build a user settings interface.

```kotlin
// 1. Create a preferences editor
val preferencesEditor = navigatorFactory.createPreferencesEditor(initialPreferences)
    
// 2. Let the user update preferences through the preferences editor
preferencesEditor.apply {
    fontFamily.set(FontFamily.SERIF)
    fontSize.increment()
    publisherStyles.toggle()
}

// 3. Submit the updated preferences
navigator.submitPreferences(preferencesEditor.preferences)
```

### Preferences Editors

The `PreferencesEditor` object is unique for each Navigator implementation and provides helpers to deal with each specific preference. Each `Preference` property represents a single configurable property of the Navigator, such as the font size or the theme. It provides access to the current value of the preference, the value that the navigator will compute as the corresponding setting, as well as additional metadata and constraints depending on the preference type.

Here are some of the available preference types:

* `TogglePreference` - a simple boolean preference, e.g. whether or not the publisher styles are enabled.
* `RangePreference<V>` - a preference for numbers constrained in a range, e.g. the page margins as a `RangeSetting<Int>` could range from 0px to 200px.
* `EnumPreference<V>` - a preference whose value is a member of the enum `V`, e.g. the theme (`light`, `dark`, `sepia`) or the font family.

#### `Preferences` are low-level

The `Preferences` objects are technical low-level properties. While some of them can be directly exposed to the user, such as the font size, other preferences should not be displayed as-is.

For example in EPUB, we simulate two pages side by side with `columnCount` (`auto`, `1`, `2`) for reflowable resources and `spread` (`auto`, `never`, `always`) for a fixed layout publication. Instead of showing both settings with all their possible values in the user interface, you might prefer showing a single switch button to enable a dual-page mode which will set both settings appropriately.

### Preferences

The `Preferences` object holds the values which should be preferred by the Navigator when computing its `Settings`. Preferences can be combined by the app from different sources:

* Static app defaults.
* User preferences restored from JSON.
* User preferences interface.

#### Inactive settings

A setting can be inactive if its activation conditions are not met in a set of preferences. The Navigator will ignore inactive settings when refreshing its presentation. For instance with the EPUB navigator, the word spacing setting requires the publisher styles to be disabled to take effect.

You can check if a setting is effective with:

```kotlin
preference.isEffective
```

## Setting the initial Navigator preferences and app defaults

When opening a publication, you want to apply the user preferences right away. You can do that by providing them to the Navigator constructor. The API depends on each Navigator implementation, but looks like this:

```kotlin
val navigatorFactory = EpubNavigatorFactory(
    publication = publication,
    configuration = EpubNavigatorFactory.Configuration(
        defaults = EpubDefaults(language="fr")
    )
)

navigatorFactory.createFragmentFactory(
    ...,
    initialPreferences = EpubPreferences(
        scroll = true
    )
)
```

The `defaults` are used as fallback values when the default Navigator settings are not suitable for your application.

## Build a user settings interface

:question: The following examples are using [Jetpack Compose](https://developer.android.com/jetpack/compose), but could be implemented with regular Android views.

You can use the `PreferencesEditor` API to build a user settings interface dynamically. As this API is agnostic to the type of publication (excepted the editor itself), you can reuse parts of the user settings screen across Navigator implementations or media types.

For example, you could group the user preferences per nature of publications:

* `ReflowableUserPreferences` for a visual publication with adjustable fonts and dimensions, such as a reflowable EPUB, HTML document or PDF with reflow mode enabled.
* `FixedLayoutUserPreferences` for a visual publication with a fixed layout, such as FXL EPUB, PDF or comic books.
* `PlaybackUserPreferences` for an audiobook, text-to-speech or EPUB media overlays preferences.

### Stateless `UserSettings` composable

You can switch over the `PreferencesEditor` type to decide on the best kind of user preferences screen for it.

```kotlin
@Composable
fun <P: Configurable.Preferences, E: PreferencesEditor<P>> UserSettings(
    editor: E,
    commit: () -> Unit
) {
    Column {
        Text("User settings")
        
        Button(
            onClick = {
                editor.clear()
                commit()
            },
        ) {
            Text("Reset")
        }

        Divider()

        when (editor) {
            is EpubPreferencesEditor ->
                ReflowableUserPreferences(
                    publisherStyles = editor.publisherStyles,
                    fontSize = editor.fontSize,
                    fontFamily = editor.fontFamily,
                    commit = commit,
                )
        }
    }
}
```

The `commit` parameter is a closure used to save the edited preferences to the data store.

:question: The individual `EpubPreferences`' `Preference` properties are forwarded to `ReflowableUserPreferences` to be able to reuse it with other reflowable publication types.

### User settings composable for reflowable publications

This stateless composable displays the actual preferences for a reflowable publication. The `Preference` parameters are nullable as they might not be available at all times or for all media types. It delegates the rendering of individual preferences to specific composables.

```kotlin
@Composable
fun ReflowableUserPreferences(
    publisherStyles: SwitchPreference? = null,
    fontSize: RangePreference<Double>? = null,
    fontFamily: EnumPreference<FontFamily?>? = null,
    commit: () -> Unit
) {
    if (publisherStyles != null) {
        SwitchItem("Publisher styles", publisherStyles, commit)
    }

    if (fontSize != null) {
        StepperItem("Font size", fontSize, commit)
    }

    if (font != null) {
        MenuItem("Font", fontFamily, commit) { fontFamily ->
            fontFamily.name
        }
    }
}
```

### Composable for a `SwitchPreference`

A `SwitchPreference` can be represented as a simple switch button.

```kotlin
@Composable
fun SwitchItem(
    title: String,
    preference: SwitchPreference, 
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

This composable takes advantage of the helpers in `SwitchPreference` to set the preference in two different ways:

* `toggle()` will invert the current preference when tapping on the whole list item.
* `set(checked)` sets an explicit value provided by the `Switch`'s `onCheckedChange` callback.

:point_up: Note that the current state for `Switch` is derived from the selected preference first, and the actual setting value as a fallback (`checked = preference.value ?: preference.effectiveValue`). We deemed more important to display the user selected value first, even if it is not applied yet in the Navigator. Your opinion may differ, in which case you can use `checked = preference.effectiveValue`.

### Composable for a `RangePreference<V>`

A `RangePreference<V>` can be represented as a stepper component with decrement and increment buttons.

```kotlin
@Composable
fun  <V: Comparable<V>> StepperItem(
    title: String,
    preference: RangePreference<V>,
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
                    Icon(Icons.Default.Remove, contentDescription = "Less")
                }

                val currentValue = preferences.value ?: preference.effectiveValue
                Text(preference.formatValue(currentValue))

                IconButton(
                    onClick = {
                        preference.increment(setting)
                        commit()
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "More")
                }
            }
        },
    )
}
```

This composable uses the `increment()` and `decrement()` range helpers of `RangePreference`, but you could also set a value manually.

Between the two buttons, we display the current value using the `RangeSetting<V>.formatValue()` helper. This will automatically format the value to a human-readable string, such as a percentage or a value with units (e.g. 30px).

### Composable for an `EnumPreference<V>`

An enum can be displayed with various components, such as:

* a dropdown menu for a large enum
* [segmented buttons](https://m3.material.io/components/segmented-buttons/overview) for a small one

In this example, we chose a dropdown menu built using the `preference.supportedValues`, which returns the allowed enum members.

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

Having a user settings screen is moot if you cannot save and restore the selected preferences for future sessions. Each navigator comes with a JSON serialization helper that you can use or not.

```kotlin
val epubPreferencesSerializer = EpubPreferencesSerializer()

val jsonString = epubPreferencesSerializer.serialize(preferences)
```

When you are ready to restore the user preferences, construct a new `Preferences` object from the JSON string.

```kotlin
val preferences = epubPreferencesSerializer.deserialize(jsonString)
```

In the Test App, `UserPreferencesViewModel` delegates the preferences state hoisting and persistence to a `PreferencesManager`, which acts as a single source of truth.

### Splitting and merging preferences

How you store user preferences has an impact on the available features. You could have, for example:

* A different unique set of preferences for each publication.
* Preferences shared between publications with the same profile or media type (EPUB, PDF, etc.).
* Global preferences shared with all publications (e.g. theme).
* Several user setting profiles/themes that the user can switch to and modify independently.
* Some settings that are not stored as JSON and will need to be reconstructed (e.g. the publication language).

To help you to deal with this, the toolkit provides for each navigator suggested filters that you can use or not.
You can then combine several sets of preferences with the `+` operator.

```kotlin
val bookPrefs = EpubPublicationPreferencesFilter.filter(preferences)
val profilePrefs = EpubSharedPreferencesFilter.filter(preferences)

val combinedPrefs = profilePrefs + bookPrefs
```

:warning: Some preferences are really tied to a particular publication and should never be shared between several publications, such as the language. It's recommended that you store these preferences separately per book and that's what the suggested filters would make you do if you use them.
