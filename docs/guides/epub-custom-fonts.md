# Adding custom fonts to the EPUB navigator

The `EpubNavigatorFragment` supports a limited number of font families by default. To change the available typefaces and add new ones, you need to provide font family declarations when initializing the `EpubNavigatorFragment` factory:

```kotlin
EpubNavigatorFragment.createFactory(
    ...,
    config = EpubNavigatorFragment.Configuration(
        fontFamilies = listOf(
            FontFamily.LITERATA.from(FontFamilySource.GoogleFonts),
            FontFamily.ACCESSIBLE_DFA.from(FontFamilySource.ReadiumCss),
            FontFamily.OPEN_DYSLEXIC.from(FontFamilySource.Assets("fonts/OpenDyslexic-Regular.otf")),
        )
    )
)
```

To use the default list of typefaces, modify or pass the `EpubNavigatorFragment.Configuration.DEFAULT_FONT_FAMILIES` list.

## Typefaces available offline

If you want to expose only the font families that are available offline, use this configuration:

```kotlin
EpubNavigatorFragment.Configuration(
    fontFamilies = listOf(
        FontFamily.SERIF.from(FontFamilySource.System),
        FontFamily.SANS_SERIF.from(FontFamilySource.System),
        FontFamily.CURSIVE.from(FontFamilySource.System),
        FontFamily.MONOSPACE.from(FontFamilySource.System),
        FontFamily.ACCESSIBLE_DFA.from(FontFamilySource.ReadiumCss),
        FontFamily.IA_WRITER_DUOSPACE.from(FontFamilySource.ReadiumCss),
    )
)
```

By default, Android ships with a very limited set of font families. Only these generic ones are available offline: `sans-serif`, `serif`, `cursive` and `monospace`.

Additionally, [Readium CSS](https://github.com/readium/readium-css) embeds two accessible typefaces:

* [AccessibleDfA](https://github.com/Orange-OpenSource/font-accessible-dfa), by Orange;
* [IA Writer Duospace](https://github.com/iaolo/iA-Fonts/tree/master/iA%20Writer%20Duospace), by iA.

## Typefaces downloaded from Google Fonts

You can also use font families hosted by [Google Fonts](https://fonts.google.com/) using the `GoogleFonts` font family source. :warning: The device will need an Internet connection to use them.

```kotlin
FontFamily.LITERATA.from(FontFamilySource.GoogleFonts)
```

Readium declares [a set of open source font families](https://github.com/readium/readium-css/blob/develop/docs/CSS10-libre_fonts.md) which were vetted for reading purposes. Most of them are hosted by Google Fonts.

But you can also declare a custom font:

1. Check out if it's available on Google Fonts and copy its name, for example "[Great Vibes](https://fonts.google.com/betterspecimen/Great+Vibes)".
2. In your app, declare the new `FontFamily` using its exact name. Providing an `alternate` font family as a fallback is a good idea in case the device is offline.
    ```kotlin
    val greatVibes = FontFamily(name = "Great Vibes", alternate = FontFamily.CURSIVE)
    ```
3. Finally, use the font family with the `GoogleFonts` source in the `Configuration` object.
    ```kotlin
    greatVibes.from(FontFamilySource.GoogleFonts)
    ```

## Embed typefaces in your app assets

A better way to support new font families is to embed them directly in your app to have them available offline.

1. Add the font files in the [`assets/` directory](https://developer.android.com/guide/topics/resources/providing-resources#OriginalFiles).
2. In your app, declare the new `FontFamily` using a name of your choice. Providing an `alternate` font family as a fallback is a good idea in case the font file is unreachable.
    ```kotlin
    val openDyslexic = FontFamily(name = "OpenDyslexic", alternate = FontFamily.ACCESSIBLE_DFA)
    ```
3. Finally, use the font family with the `Assets` source in the `Configuration` object, giving the file path relative to the `assets/` directory.
    ```kotlin
    openDyslexic.from(FontFamilySource.Assets("fonts/OpenDyslexic-Regular.otf"))
    ```

## Android Downloadable Fonts

Android [Downloadable Fonts](https://developer.android.com/guide/topics/ui/look-and-feel/downloadable-fonts) are not yet supported. Please open a new issue if you want to contribute this feature to Readium.

