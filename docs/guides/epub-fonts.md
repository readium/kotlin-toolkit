# Font families in the EPUB navigator

Readium allows users to customize the font family used to render a reflowable EPUB, by changing the [EPUB navigator preferences](navigator-preferences.md).

:warning: You cannot change the default font family of a fixed-layout EPUB (with zoomable pages), as it is similar to a PDF or a comic book.

## Available font families

Android does not ship with any font families except the generic ones: `sans-serif`, `serif`, `cursive` and `monospace`. As a result, the default selection of typefaces with the Readium Kotlin toolkit is quite limited.

To improve readability, Readium embeds three additional font families designed for accessibility:

* [OpenDyslexic](https://opendyslexic.org/)
* [AccessibleDfA](https://github.com/Orange-OpenSource/font-accessible-dfa), by Orange
* [iA Writer Duospace](https://github.com/iaolo/iA-Fonts/tree/master/iA%20Writer%20Duospace), by iA

You can use all these font families out of the box with the EPUB navigator:

```kotlin
// Set the current font family.
epubNavigator.submitPreferences(EpubPreferences(
    fontFamily = FontFamily.OPEN_DYSLEXIC
))
```

```kotlin
// To customize the list of available font families with the preferences editor:
epubPreferencesEditor.fontFamily
    .withSupportedValues(
        FontFamily.SANS_SERIF,
        FontFamily.SERIF,
        FontFamily.CURSIVE,
        FontFamily.MONOSPACE,
        FontFamily.OPEN_DYSLEXIC,
        FontFamily.ACCESSIBLE_DFA,
        FontFamily.IA_WRITER_DUOSPACE
    )
```

## How to add custom font families?

To offer more choices to your users, you must embed and declare custom font families. Use the following steps:

1. Get the font files in the desired format, such as .ttf and .otf. [Google Fonts](https://fonts.google.com/) is a good source of free fonts.
2. Copy the files to a subdirectory of your [app `assets`](https://developer.android.com/guide/topics/resources/providing-resources), such as `src/main/assets/fonts`.
3. Declare new extensions for your custom font families to make them first-class citizens. This is optional but convenient.
    ```kotlin
    val FontFamily.Companion.ATKINSON_HYPERLEGIBLE get() = FontFamily("Atkinson Hyperlegible")
    val FontFamily.Companion.LITERATA: FontFamily get() = FontFamily("Literata")
    ```
4. Configure the EPUB navigator with:
    * `servedAssets` - An asset path pattern to serve your font files in the navigator.
    * A declaration of the font faces for all the additional font families.
    ```kotlin
    EpubNavigatorFactory(...).createFragmentFactory(
        ...,
        configuration = EpubNavigatorFactory.Configuration {
            // Add the assets folder which contains the font files to authorize the navigator to access it.
            servedAssets += "fonts/.*"

            // Declare a font family with a file per style.
            addFontFamilyDeclaration(FontFamily.ATKINSON_HYPERLEGIBLE) {
                addFontFace {
                    addSource("fonts/AtkinsonHyperlegible-Regular.ttf", preload = true)
                    setFontStyle(FontStyle.NORMAL)
                    setFontWeight(FontWeight.NORMAL)
                }
                addFontFace {
                    addSource("fonts/AtkinsonHyperlegible-Bold.ttf")
                    setFontStyle(FontStyle.NORMAL)
                    setFontWeight(FontWeight.BOLD)
                }
                addFontFace {
                    addSource("fonts/AtkinsonHyperlegible-Italic.ttf")
                    setFontStyle(FontStyle.ITALIC)
                    setFontWeight(FontWeight.NORMAL)
                }
                addFontFace {
                    addSource("fonts/AtkinsonHyperlegible-BoldItalic.ttf")
                    setFontStyle(FontStyle.ITALIC)
                    setFontWeight(FontWeight.BOLD)
                }
            }

            // Declare a variable font family.
            // See https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Fonts/Variable_Fonts_Guide
            addFontFamilyDeclaration(FontFamily.LITERATA) {
                addFontFace {
                    addSource("fonts/Literata-VariableFont_opsz,wght.ttf")
                    setFontStyle(FontStyle.NORMAL)
                    setFontWeightRange(200..900)
                }
                addFontFace {
                    addSource("fonts/Literata-Italic-VariableFont_opsz,wght.ttf")
                    setFontStyle(FontStyle.ITALIC)
                    setFontWeightRange(200..900)
                }
            }
        }
    )
    ```

You are now ready to use your custom font families.

```kotlin
// Set the current font family.
epubNavigator.submitPreferences(EpubPreferences(
    fontFamily = FontFamily.LITERATA
))
```

```kotlin
// To customize the list of available font families with the preferences editor:
epubPreferencesEditor.fontFamily
    .withSupportedValues(
        FontFamily.ATKINSON_HYPERLEGIBLE,
        FontFamily.LITERATA,
        FontFamily.OPEN_DYSLEXIC,
    )
```

## Android Downloadable Fonts

Android [Downloadable Fonts](https://developer.android.com/guide/topics/ui/look-and-feel/downloadable-fonts) are not yet supported. Please open a new issue if you want to contribute this feature to Readium.

