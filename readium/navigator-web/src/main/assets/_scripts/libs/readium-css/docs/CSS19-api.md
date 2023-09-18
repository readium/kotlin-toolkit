# Readium CSS Variables API

[Implementers’ doc]

This document is meant to list all the customizable medias and flags (to be found in `ReadiumCSS-config.css`), as well as all the CSS variables for Reading System and User styles available in the `dist` stylesheets.

As a reminder, the priority is, in general: 

```
USER > AUTHOR > RS
```

## How to set and remove user preferences

### Setting a user preference

```
var root = document.documentElement; 

root.style.setProperty("--USER__var", "value");
```

You don’t need to remove the variable before setting another value, the new value will simply override the existing one.

### Removing a user preference

```
var root = document.documentElement; 

root.style.removeProperty("--USER__var");
```

## Customizable medias

You will find those customizable medias in `ReadiumCSS-config.css`. The values defined are used in media queries to control use of the auto pagination model.

* * *

```
--responsive-columns
```

Default is `60em`

The `min-width` at which the auto pagination model must be used – will switch from 1 to 2 columns and vice versa.

* * *

```
--min-device-columns
```

Default is `36em`

The minimum device width of the mobile device for which the auto pagination model must be used.

* * *

```
--max-device-columns
```

Default is `47em`

The maximum device width of the mobile device for which the auto pagination model must be used.

* * *

**Warning:** if you customize those medias, all ReadiumCSS `dist` stylesheets must be rebuilt.

## Customizable flags

You will find those customizable flags in `ReadiumCSS-config.css`, and can think of the preset values as boolean inline styles: if they are set on the `:root` element (i.e. `html`) then the flag is enabled. If another value is, or they are removed, then the flag is disabled.

Those custom selectors can only be customized before runtime. You could for example use a class or a custom attribute instead of an inline style. Check the [“User Preferences”](../docs/CSS12-user_prefs.md#flags) and [“Quickstart”](../docs/CSS02-quickstart.md) docs for further details.

**Note:** The preset is not a default implementers should use. Indeed, the initialization of those flags depends on your user settings’ management e.g. apply user settings to all EPUBs, only the ones that have already been customized, etc.

* * * 

```
:--paged-view
```

Preset: `--USER__view: readium-paged-on`

Scope: `html`

Override class: Chrome (should be applied by any means necessary)

* * * 

```
:--scroll-view
```

Preset: `--USER__view: readium-scroll-on`

Scope: `html`

Override class: Chrome (should be applied by any means necessary)

* * * 

```
:--font-override
```

Preset: `--USER__fontOverride: readium-font-on`

Scope: `html`

Override class: None. This flag is required to change the `font-family` user setting.

* * * 

```
:--advanced-settings
```

Preset: `--USER__advancedSettings: readium-advanced-on`

Scope: `html`

Override class: None. This flag is required to apply the `font-family`, the `font-size` and/or advanced user settings.

* * * 

```
:--sepia-mode
```

Preset: `--USER__appearance: readium-sepia-on`

Scope: `html`

Override class: Chrome (should be applied by any means necessary)

This flag applies the sepia reading mode.

* * * 

```
:--night-mode
```

Preset: `--USER__appearance: readium-night-on`

Scope: `html`

Override class: Chrome (should be applied by any means necessary)

This flag applies the night reading mode.

* * * 

```
:--darken-filter
```

Preset: `--USER__darkenImages: readium-darken-on`

Scope: `html`

Override class: Chrome advanced (optional but should be applied by any means necessary if provided to users)

This will only apply in night mode to darken images and impact `img`.

* * * 

```
:--invert-filter
```

Preset: `--USER__invertImages: readium-invert-on`

Scope: `html`

Override class: Chrome advanced (optional but should be applied by any means necessary if provided to users)

This will only apply in night mode to invert images and impact `img`.

* * * 

```
:--a11y-normalize
```

Preset: `--USER__a11yNormalize: readium-a11y-on`

Scope: `html`

Required flag: `:--fontOverride`

Override class: User settings advanced (optional but should be applied by any means necessary if provided to users)

It impacts font style, weight and variant, text decoration, super and subscripts.

* * *

**Warning:** if you customize those flags, all ReadiumCSS `dist` stylesheets must be rebuilt.

## Reading System Styles

Custom properties for the Reading System are prefixed with `--RS__`.

### Pagination

* * *

```
--RS__colWidth
```

The optimal column’s width. It serves as a floor in our design.

* * *

```
--RS__colCount
```

The optimal number of columns (depending on the columns’ width).

* * *

```
--RS__colGap
```

The gap between columns. It must be set in pixels so that it won’t resize with font size. 

You must account for this gap when scrolling.

* * *

```    
--RS__pageGutter
```

The horizontal page margins. It must be set in pixels so that it won’t resize with font size.

* * *

```
--RS__maxLineLength
```

The optimal line-length. It must be set in `rem` in order to take `:root`’s `font-size` as a reference, whichever the `body`’s `font-size` might be.

### Safeguards

* * *

```
--RS__maxMediaWidth
```

The `max-width` for media elements i.e. `img`, `svg`, `audio` and `video`.

* * *

```
--RS__maxMediaHeight
```

The `max-height` for media elements i.e. `img`, `svg`, `audio` and `video`.

* * *

```
--RS__boxSizingMedia
```

The box model (`box-sizing`) you want to use for media elements.

* * *

```
--RS__boxSizingTable
```

The box model (`box-sizing`) you want to use for tables.

### Default font-stacks

* * *

```
--RS__oldStyleTf
```

An old style serif font-stack relying on pre-installed fonts.

Default is `"Iowan Old Style", "Sitka Text", Palatino, "Book Antiqua", serif`.

* * *

```
--RS__modernTf
```

A modern serif font-stack relying on pre-installed fonts.

Default is `Athelas, Constantia, Georgia, serif`.

* * *

```
--RS__sansTf
```

A neutral sans-serif font-stack relying on pre-installed fonts.

Default is `-apple-system, system-ui, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif`.

* * *

```
--RS__humanistTf 
```

A humanist sans-serif font-stack relying on pre-installed fonts.

Default is `Seravek, Calibri, Roboto, Arial, sans-serif`.

* * *

```
--RS__monospaceTf 
```

A monospace font-stack relying on pre-installed fonts.

Default is `"Andale Mono", Consolas, monospace`.

### Default font-stacks for Japanese publications

* * *

```
--RS__serif-ja
```

A Mincho font-stack whose fonts with proportional latin characters are prioritized for horizontal writing.

Default is `"ＭＳ Ｐ明朝", "MS PMincho", "Hiragino Mincho Pro", "ヒラギノ明朝 Pro W3", "游明朝", "YuMincho", "ＭＳ 明朝", "MS Mincho", "Hiragino Mincho ProN", serif`.

* * *

```
--RS__sans-serif-ja
```

A Gothic font-stack whose fonts with proportional latin characters are prioritized for horizontal writing.

Default is `"ＭＳ Ｐゴシック", "MS PGothic", "Hiragino Kaku Gothic Pro W3", "ヒラギノ角ゴ Pro W3", "Hiragino Sans GB", "ヒラギノ角ゴシック W3", "游ゴシック", "YuGothic", "ＭＳ ゴシック", "MS Gothic", "Hiragino Sans", sans-serif`.

* * *

```
--RS__serif-ja-v
```

A Mincho font-stack whose fonts with fixed-width latin characters are prioritized for vertical writing.

Default is `"ＭＳ 明朝", "MS Mincho", "Hiragino Mincho Pro", "ヒラギノ明朝 Pro W3", "游明朝", "YuMincho", "ＭＳ Ｐ明朝", "MS PMincho", "Hiragino Mincho ProN", serif`.

* * *

```
--RS__sans-serif-ja-v
```

A Gothic font-stack whose fonts with fixed-width latin characters are prioritized for vertical writing.

Default is `"ＭＳ ゴシック", "MS Gothic", "Hiragino Kaku Gothic Pro W3", "ヒラギノ角ゴ Pro W3", "Hiragino Sans GB", "ヒラギノ角ゴシック W3", "游ゴシック", "YuGothic", "ＭＳ Ｐゴシック", "MS PGothic", "Hiragino Sans", sans-serif`.

### Base styles for all ebooks

* * *

```
--RS__baseFontFamily
```

The default typeface for body copy in case the ebook doesn’t have one declared.

Please note some languages have a specific font-stack (japanese, chinese, hindi, etc.)

* * *

```
--RS__baseLineHeight
```

The default line-height for body copy in case the ebook doesn’t have one declared.

### Default colors for all ebooks

* * *

```
--RS__textColor
```

The default `color` for body copy’s text.

* * *

```
--RS__backgroundColor
```

The default `background-color` for pages.

* * *

```
--RS__selectionBackgroundColor
```

The `background-color` for selected text.

It is worth noting it can be customized for each reading mode.

* * *

```
--RS__selectionTextColor
```

The `color` for selected text.

It is worth noting it can be customized for each reading mode.

### Default styles for unstyled publications

#### Typefaces 

* * *

```
--RS__compFontFamily
```

The typeface for headings. The value can be another variable e.g. `var(-RS__humanistTf)`.

* * *

```
--RS__codeFontFamily
```

The typeface for code snippets. The value can be another variable e.g. `var(-RS__monospaceTf)`.

#### Typography

* * *

```
--RS__typeScale
```

The scale to be used for computing all elements’ `font-size`. Since those font sizes are computed dynamically, you can set a smaller type scale when the user sets one of the largest font sizes.

Possible values: `1` | `1.067` | `1.125` | `1.2` (suggested default) | `1.25` | `1.333` | `1.414` | `1.5` | `1.618`

* * *

```
--RS__baseFontSize
```

The default `font-size` for body copy. It will serve as a reference font all related computations.

* * *

```
--RS__baseLineHeight
```

The default `line-height` for all elements.

#### Vertical rhythm

* * *

```
--RS__flowSpacing
```

The default vertical margins for HTML5 flow content e.g. `pre`, `figure`, `blockquote`, etc.

* * *

```
--RS__paraSpacing
```

The default vertical margins for paragraphs.

* * *

```
--RS__paraIndent
```

The default `text-indent` for paragraphs.

#### Hyperlinks

* * *

``` 
--RS__linkColor
```

The default `color` for hyperlinks.

* * *

```
--RS__visitedColor
```

The default `color` for visited hyperlinks.

#### Accentuation colors

* * *

```
--RS__primaryColor
```

An optional primary accentuation `color` you could use for headings or any other element of your choice.

* * *

```
--RS__secondaryColor
```

An optional secondary accentuation `color` you could use for any element of your choice.

## Reading Modes

Custom properties for reading modes are prefixed with `--RS__`.

* * *

```
--RS__backgroundColor
```

The `background-color` which must be applied to the entire screen in the reading mode.

* * *

```
--RS__textColor
```

The `color` for text in the reading mode.

* * *

```
--RS__linkColor
```

The `color` for the links in the reading mode.

* * *

```  
--RS__visitedColor
```

The `color` for visited links in the reading mode.

* * *

```
--RS__selectionBackgroundColor
```

The `background-color` for selected text in the reading mode.

* * *

```
--RS__selectionTextColor
```

The `color` for selected text in the reading mode.

## User Settings

Custom properties for user settings are prefixed with `--USER__`.

* * *

```
--USER__colCount
```

The number of columns (`column-count`) the user wants displayed (one-page view or two-page spread).

Scope: `html`

Possible values: `1` | `2` | `auto` (default)

Required flag: none

Override class: Chrome advanced (optional but should be applied by any means necessary if provided to users)

To reset, change the value to `auto`.

* * *

```
--USER__pageMargins
```

A factor applied to horizontal margins (`padding-left` and `padding-right`) the user wants to set.

Scope: `html`

It impacts `body`.

Recommended values: a range from `0.5` to `2`.  Increments are left to implementers’ judgment.

Required flag: none

Override class: Chrome advanced (optional but should be applied by any means necessary if provided to users)

To reset, change the value to `1`.

* * *

```
--USER__backgroundColor
```

The `background-color` for the whole screen.

Scope: `html` 

It impacts all elements in the DOM.

Possible values: Color HEX (e.g. `#FFFFFF`), `rgb(a)`, `hsl`.

Required flag: none

Override class: Chrome advanced (optional but should be applied by any means necessary if provided to users)

To reset, remove the CSS variable.

* * *

```
--USER__textColor
```

The `color` for textual contents.

Scope: `html`

It impacts all elements but headings and `pre` in the DOM.

Possible values: Color HEX (e.g. `#FFFFFF`), `rgb(a)`, `hsl`.

Required flag: none

Override class: Chrome advanced (optional but should be applied by any means necessary if provided to users)

To reset, remove the CSS variable.

* * *

```
--USER__textAlign
```

The alignment (`text-align`) the user prefers.

Scope: `html`

It impacts `body`, `li`, and `p` which are not children of `blockquote` and `figcaption`.

Possible values: `left` (LTR) or `right` (RTL) | `justify`

Required flag: `:--advancedSettings`

Override class: User settings advanced (optional but should be applied by any means necessary if provided to users)

Note: the value `start` can be used to let all rendering engines, excepted Trident (IE11) and EdgeHTML (Edge), automatically deal with `left` and `right` based on the direction (`dir` attribute) set for the document and its nested elements.

* * *

```
--USER__bodyHyphens
```

Enabling and disabling hyphenation.

Scope: `html`

It impacts `body`, `p`, `li`, `div` and `dd`.

Possible Values: `auto` | `none`

Required flag: `:--advancedSettings`

Override class: User settings advanced (optional but should be applied by any means necessary if provided to users)

**Warning:** for the time being, automatic hyphenation won’t work if you are using the Blink rendering engine (either via Chrome or a Webview) on ChromeOS, Linux and Windows. It indeed is not implemented yet and we recommend not trying to polyfill it using JavaScript as it will create a11y issues, especially with screen readers.

* * *

```
--USER__fontFamily
```

The typeface (`font-family`) the user wants to read with.

Scope: `html`

It impacts `body`, `p`, `li`, `div`, `dt`, `dd` and phrasing elements which don’t have a `lang` or `xml:lang` attribute.

Required flag: `:--fontOverride`

Override class: User settings (should be applied by any means necessary)

To reset, remove the required flag.

* * *

```
--USER__fontSize
```

Increasing and decreasing the root `font-size`. It will serve as a reference for the cascade.

Scope: `html`

Possible values: `var(--RS__oldStyleTf)` | `var(--RS__modernTf)` | `var(--RS__sansTf)` | `var(--RS__humanistTf)` | `<string>`

For Japanese, possible values become: `var(--RS__serif-ja)` (horizontal writing) | `var(--RS__sans-serif-ja)` (horizontal writing) | `var(--RS__serif-ja-v)` (vertical writing) | `var(--RS__sans-serif-ja-v)` (vertical writing) | `<string>`

Required flag: `:--fontOverride`

Override class: User settings (should be applied by any means necessary)

To reset, remove the required flag.

* * *

```
--USER__typeScale
```

The type scale the user wants to use for the publication.

Scope: `html`

It requires the `ReadiumCSS-fs_normalize.css` stylesheet.

It impacts headings, `p`, `li`, `div`, `pre`, `dd`, `small`, `sub`, and `sup`.

Recommended values: a range from `75%` to `250%`. Increments are left to implementers’ judgment.

Required flag: `:--advancedSettings`

Override class: User settings advanced (optional but should be applied by any means necessary if provided to users)

* * *

```
--USER__lineHeight
```

Increasing and decreasing leading (`line-height`).

Scope: `html`

It impacts `body`, `p`, `li` and `div`

Recommended values: a range from `1` to `2`. Increments are left to implementers’ judgment.

Required flag: `:--advancedSettings`

Override class: User settings advanced (optional but should be applied by any means necessary if provided to users)

* * *

```
--USER__paraSpacing
```

The vertical margins (`margin-top` and `margin-bottom`) for paragraphs.

Scope: `p`

Recommended values: a range from `0` to `2rem`. Increments are left to implementers’ judgment.

Required flag: `:--advancedSettings`

Override class: User settings advanced (optional but should be applied by any means necessary if provided to users)

* * *

```
--USER__paraIndent
```

The `text-indent` for paragraphs.

Scope: `p`

Recommended values: a range from `0` to `3rem`. Increments are left to implementers’ judgment.

Required flag: `:--advancedSettings`

Override class: User settings advanced (optional but should be applied by any means necessary if provided to users)

* * *

```
--USER__wordSpacing
```

Increasing space between words (`word-spacing`, related to a11y).

Scope: `h1`, `h2`, `h3`, `h4`, `h5`, `h6`, `p`, `li`, `div`

Recommended values: a range from `0` to `1rem`. Increments are left to implementers’ judgment.

Required flag: `:--advancedSettings`

Override class: User settings advanced (optional but should be applied by any means necessary if provided to users)

* * *

```
--USER__letterSpacing
```

Increasing space between letters (`letter-spacing`, related to a11y).

Scope: `h1`, `h2`, `h3`, `h4`, `h5`, `h6`, `p`, `li`, `div`

Recommended values: a range from `0` to `0.5rem`. Increments are left to implementers’ judgment.

Required flag: `:--advancedSettings`

Override class: User settings advanced (optional but should be applied by any means necessary if provided to users)

* * *

```
--USER__ligatures
```

Enabling and disabling ligatures in Arabic (related to a11y).

Scope: `html`

It impacts all text.

Possible values: `none` | `common-ligatures`

Required flag: `:--advancedSettings`

Override class: User settings advanced (optional but should be applied by any means necessary if provided to users)