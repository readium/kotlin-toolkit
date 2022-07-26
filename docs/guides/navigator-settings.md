# Configuring the Navigator

:warning: The Navigator Setting API is still experimental and currently only available with `EpubNavigatorFragment`.

## Overview

A few Readium components – such as the Navigator – support dynamic configuration through the `Configurable` interface. It provides an easy way to build a user settings interface and save user preferences as a JSON object.

The application cannot explicitly set the Navigator `Settings`. Instead, you can submit a set of `Preferences` to the Navigator (`Configurable`) which will in turn recompute its settings and refresh the presentation. Then, the application can refresh its user settings interface with the new settings emitted by the Navigator.

<img src="assets/settings-flow.svg">

```kotlin
// 1. Get the current Navigator settings.
val settings = navigator.settings.value

// 2. Create a new set of preferences.
val preferences = Preferences {
    set(settings.overflow, Overflow.PAGINATED)
    increment(settings.fontSize)
    toggle(settings.publisherStyles)
}

// 3. Apply the preferences, the Navigator will in turn update its settings. 
navigator.applyPreferences(preferences)
```

### Settings

The `Settings` (*plural*) object is unique for each Navigator implementation and holds the currently available `Setting` (*single*) properties. Each `Setting` object represents a single configurable property of the Navigator, such as the font size or the theme. It holds the current value of the setting, as well as additional metadata and constraints depending on the setting type.

Here are some of the available setting types:

* `ToggleSetting` - a simple boolean setting, e.g. whether or not the publisher styles are enabled.
* `RangeSetting<V : Comparable<V>>` - a setting for comparable values constrained in a range, e.g. the page margins as a `RangeSetting<Int>` could range from 0px to 200px.
* `PercentSetting` - a specialization of `RangeSetting<Double>` which represents a percentage from, by default, 0.0 to 1.0.
* `EnumSetting<V>` - a setting whose value is a member of the enum `V`, e.g. the theme (`light`, `dark`, `sepia`) or the font family.

### Preferences

The `Preferences` object holds the `Setting` values which should be preferred by the Navigator when computing its `Settings`. Preferences can be produced from different sources:

* Static app defaults.
* User preferences restored from JSON.
* User settings interface.

#### Inactive settings

A setting can be inactive if its activation conditions are not met in a set of preferences. For example, with the EPUB navigator the word spacing setting requires the publisher styles to be disabled to take effect.

The Navigator will ignore inactive settings when refreshing its presentation.

You can check if a setting is active with:

```kotlin
preferences.isActive(settings.wordSpacing)
```

To force activate a setting, use `MutablePreferences.activate()` which will automatically reset the other preferences to the required values.

```kotlin
val updatedPreferences = preferences.copy {
    activate(settings.wordSpacing)
}
```

:point_up: For convenience, settings are force activated by default when set in a `MutablePreferences`. This helps the user to see the impact of a setting when changing it in the user interface. If you wish to set a preference without modifying the other ones, set the `activate` parameter to `false`.

```kotlin
preferences.copy {
    set(settings.overflow, Overflow.PAGINATED, activate = false)
    increment(settings.fontSize, activate = false)
    toggle(settings.publisherStyles, activate = false)
}
```

## Build a user settings interface

## Save and restore the user preferences
