# EPUB Navigators feature parity

This document tracks the feature differences between the legacy EPUB navigator (`EpubNavigatorFragment` in `readium-navigator`) and the new Compose-based web navigators (`ReflowableWebRendition` and `FixedWebRendition` in `readium-navigator-web-reflowable` and `readium-navigator-web-fixedlayout`).

The new navigators are still experimental: all their public APIs are annotated with `@ExperimentalReadiumApi`.

Legend:

* ✅ Supported
* 🔄 Changed: covered through a different API design, no direct equivalent planned
* ⚠️ Partially supported, see notes
* ❌ Not supported yet
* ➖ Not applicable

## Overview

| Feature area | Legacy (Fragments) | New (Compose) | Notes |
|---|:---:|:---:|---|
| Reflowable EPUB | ✅ | ✅ | |
| Fixed-layout EPUB | ✅ | ✅ | |
| Documentation | ✅ | ✅ | |

## Navigation

| Feature | Legacy | New | Notes |
|---|:---:|:---:|---|
| Restoring the initial location | ✅ | ✅ | |
| Go to locator / link | ✅ | ✅ | New API: `goTo(GoLocation)`, `goTo(Location)`, `goTo(Url)` |
| Move forward / backward | ✅ | ✅ | `OverflowController.moveForward()` / `moveBackward()`, plus `moveLeft()` / `moveRight()` helpers |
| `canMoveForward` / `canMoveBackward` | ➖ | ⚠️ | Present but not observable, so Compose UIs may not recompose reliably |
| Edge-tap page turns | ✅ | ✅ | `DirectionalNavigationAdapter` vs `defaultInputListener()` |
| Keyboard page turns | ✅ | ❌ | No key event handling at all in the new navigators |
| Animated programmatic page turns | ✅ | ❌ | |
| Continuous scrolling across resources | ❌ | ⚠️ | Reflowable only, the fixed-layout navigator is always paginated |

## Preferences and settings

| Feature | Legacy | New | Notes |
|---|:---:|:---:|---|
| Configurable preferences | ✅ | ✅ | `PreferencesController` in the new API |
| Preferences editor for building a user settings UI | ✅ | 🔄 | Editing preferences is now the host app's responsibility |
| JSON serialization of preferences | ✅ | ✅ |

### Fixed-layout preferences

| Preference | Legacy | New | Notes |
|---|:---:|:---:|---|
| `spread` / `spreads` | ✅ | ✅ | New layout resolver honors `page` hints (left / right / center) in both LTR and RTL |
| `fit` | ❌ | ✅ | `CONTAIN`, `WIDTH`, `HEIGHT` |
| `readingProgression` | ✅ | ✅ | |

### Reflowable preferences

Present in both: `backgroundColor`, `columnCount`, `fontFamily`, `fontSize`, `fontWeight`, `hyphens`, `imageFilter`, `language`, `letterSpacing`, `ligatures`, `lineHeight`, `paragraphIndent`, `paragraphSpacing`, `readingProgression`, `scroll`, `textAlign`, `textColor`, `textNormalization`, `verticalText`, `wordSpacing`.

| Preference | Legacy | New | Notes |
|---|:---:|:---:|---|
| `theme` (light / dark / sepia) | ✅ | 🔄 | Replaced by color preference bundles (`LightTheme`, `SepiaTheme`, `DarkTheme` companion constants) instead of a single enum |
| `publisherStyles` | ✅ | 🔄 | Replaced by the narrower `overridePublisherColors` |
| `pageMargins` | ✅ | 🔄 | Replaced by `minMargins` plus line-length preferences |
| `linkColor` / `visitedColor` | ❌ | ✅ | New in the Compose navigator |
| `minimalLineLength` / `optimalLineLength` / `maximalLineLength` | ❌ | ✅ | New adaptive column layout based on line length and system font scale |

### Readium CSS and typography

| Feature | Legacy | New | Notes |
|---|:---:|:---:|---|
| Readium CSS injection with RTL / CJK variants | ✅ | ✅ | |
| Custom font family declarations | ✅ | ✅ | Same builder DSL (`fontFamilyDeclarations`) |
| Bundled accessibility fonts (OpenDyslexic, AccessibleDfA, iA Writer Duospace) | ✅ | ✅ | |

## Decorations

| Feature | Legacy | New | Notes |
|---|:---:|:---:|---|
| Apply decoration groups | ✅ | ✅ | Legacy `applyDecorations(list, group)`; new observable `decorations` map on `DecorationController` |
| Highlight and underline styles | ✅ | ✅ | |
| Custom styles and HTML templates | ✅ | ✅ | `HtmlDecorationTemplate` vs `WebDecorationTemplate`, same layout / width options |
| Activable decorations (tap events) | ✅ | ✅ | See `DecorationListener.onDecorationActivated` |
| Decorations on fixed layout | ❌ | ✅ | |

## Selection

| Feature | Legacy | New | Notes |
|---|:---:|:---:|---|
| `currentSelection()` / `clearSelection()` | ✅ | ✅ | |
| Selection on fixed layout | ❌ | ✅ | Supports single and double spreads |
| Custom selection action mode callback | ✅ | ✅ | `textSelectionActionModeCallback` parameter |
| Selection disabled on protected publications | ✅ | ⚠️ | Always disabled in the new navigators, no opt-out yet |
| CSS selector in selection locations | ✅ | ❌ | Selections currently carry only text quotes |

## Input and gestures

| Feature | Legacy | New | Notes |
|---|:---:|:---:|---|
| Tap events (reflowable) | ✅ | ✅ | |
| Tap events (fixed layout) | ❌ | ✅ | The legacy navigator did not report gestures for fixed-layout publications |
| Tapped element info (e.g. image viewer) | ✅ | ❌ | Legacy `TapEvent.targetElement`; new `TapEvent` only carries an offset |
| Drag events | ⚠️ | ❌ | Reflowable only in the legacy navigator; no drag events in the new ones |
| Key events | ✅ | ❌ | |

## Hyperlinks and footnotes

| Feature | Legacy | New | Notes |
|---|:---:|:---:|---|
| Intercept internal link activation | ✅ | ✅ | `shouldFollowInternalLink` vs `onReadingOrderLinkActivated` |
| External link callback | ✅ | ✅ | |
| Footnote content extraction (`FootnoteContext`) | ✅ | ✅ | |
| Non-linear resource links | ⚠️ | ⚠️ | New API has a dedicated `onNonLinearLinkActivated` callback but no built-in way to render non-linear resources |

## Locators, progression and positions

| Feature | Legacy | New | Notes |
|---|:---:|:---:|---|
| Current location tracking | ✅ | ✅ | `currentLocator: StateFlow` vs observable `location` on the controller |
| Position and total progression | ✅ | ✅ | FXL positions derived from the reading-order index in the new navigator |
| `firstVisibleElementLocator()` | ✅ | ❌ | Needed for starting TTS on the current page |
| Visible range reporting | ❌ | ✅ | New `ReflowableWebViewport` exposes visible progression and position ranges, reflowable only |

## Integration and platform

| Feature | Legacy | New | Notes |
|---|:---:|:---:|---|
| Resource load error reporting | ✅ | ❌ | Errors silently dropped (`TODO: pass errors to the app` in both rendition states) |
| Custom served assets (`servedAssets`) | ✅ | ✅ | |
| Custom JavaScript interfaces | ✅ | ❌ | Legacy `registerJavascriptInterface` has no equivalent |
| `evaluateJavascript()` | ✅ | ❌ | |
| Window insets and display cutout handling | ✅ | ✅ | `windowInsets` parameter on the composables |
| `disablePageTurnsWhileScrolling` | ✅ | ✅ | `handleTapsWhileScrolling` in `defaultInputListener` |
