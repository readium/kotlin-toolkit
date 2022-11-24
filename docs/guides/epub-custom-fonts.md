# The FontFamily type

The `FontFamily` type represents a font family that can be picked up by users when they are reading reflowable EPUBs. Some are predefined in the navigator (though typefaces are not necessarily available for them) and you can create new ones using a name of your choice. Providing an `alternate` font family as a fallback is a good idea in case the font file is unreachable.

```kotlin
    val openDyslexic = FontFamily(name = "OpenDyslexic", alternate = FontFamily.ACCESSIBLE_DFA)
```

# Adding custom typefaces to the EPUB navigator

By default, Android doesn't guarantee the availability of any specific typeface on devices. Only generic font families can reliably be used: `sans-serif`, `serif`, `cursive` and `monospace`.

Additionally, the `EpubNavigatorFragment` comes with three font families that you don't have to provide typefaces for:

* [AccessibleDfA](https://github.com/Orange-OpenSource/font-accessible-dfa), by Orange;
* [IA Writer Duospace](https://github.com/iaolo/iA-Fonts/tree/master/iA%20Writer%20Duospace), by iA;
* [OpenDyslexic](https://opendyslexic.org/).

To add new typefaces, you need to provide font family declarations when initializing the `EpubNavigatorFragment` factory. Make sure that one of the patterns that you provide in `servedAssets` is matching the asset path you pass to `æddSource`.

```kotlin
EpubNavigatorFragment(
    ...,
    configuration = EpubNavigatorFactory.Configuration(
        servedAssets = listOf(
           "fonts/.*"
        )
    ).apply {
       addFontFamilyDeclaration(FontFamily.LITERATA) {
          addFontFace {
             addSource("fonts/Literata-VariableFont_opsz,wght.ttf")
             setFontStyle(FontStyle.NORMAL)
          }
          addFontFace {
             addSource("fonts/Literata-Italic-VariableFont_opsz,wght.ttf")
             setFontStyle(FontStyle.ITALIC)
          }
       }
    }
)
```

# Customizing the list of fonts available through the EPUB preferences editor

If you have added custom typefaces to the EPUB navigator, you probably want to customize the list of fonts available through the preferences editor as well.

```kotlin
EpubNavigatorFactory(
    ...,
       preferencesEditorConfiguration = EpubPreferencesEditor.Configuration(
          fontFamilies = listOf(FontFamily.LITERATA, FontFamily.OPEN_DYSLEXIC, FontFamily.ACCESSIBLE_DFA)
       )
    )
)
```

## Android Downloadable Fonts

Android [Downloadable Fonts](https://developer.android.com/guide/topics/ui/look-and-feel/downloadable-fonts) are not yet supported. Please open a new issue if you want to contribute this feature to Readium.

