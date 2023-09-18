# User Settings, Reading Modes and Themes

[Implementers’ doc] [Authors’ info]

The idea is:

1. appending/removing flags and CSS variables for user settings (inline style on `html`);
2. managing user settings entirely via CSS so that we don’t need to manipulate the DOM too much.

There are alternatives approaches you can adopt if this one doesn’t fit.

**Note:** Possible values are strict i.e. implementers can’t use any other value; recommended values are loose i.e. they are left to implementers’ judgment.

## Mechanism for user settings

The `ReadiumCSS-after.css` stylesheet, which contains user settings, can be appended before runtime; its declarations won’t be applied until user variables are set. 

User settings require the following process: 

1. add the flag and its value to `html` when applicable (font override, font size and/or advanced setting);
2. add the setting-specific variable and its value to `html`;
3. styles are updated live.

The selectors used in user settings are indeed “conditional”, styles are applied if the variable (or a specific value for reading modes) is set as an inline style in `html` (`:root`).

### Setting and removing a variable

#### Setting

```
var root = document.documentElement; 

root.style.setProperty("name of var", "value");
```

You don’t need to remove the variable before setting another value, the new value will simply override the existing one.

#### Removing

You can either remove the style explicitly with `removeProperty()`:

```
var root = document.documentElement; 

root.style.removeProperty("name of var");
```

Or implicitly by using an empty string as a value with `setProperty()`:

```
var root = document.documentElement; 

root.style.setProperty("name of var", "");
```

Setting a property with an empty string as the value will indeed invoke `removeProperty()`, as defined in the [CSS Object Model Spec](https://drafts.csswg.org/cssom/#dom-cssstyledeclaration-setproperty).

## Flags

Some variables behave like flags. You could also use custom `data-*` attributes or CSS classes to manage the following ones. See the [“Quickstart” doc](../docs/CSS02-quickstart.md) for customization.

By default, those flags are not set. Then their initialization depends on your user settings’ management e.g. apply user settings to all EPUBs, only the ones that have already been customized, etc.

### User view

Allows to switch between paged and scroll view.

```
--USER__view
```

Supported values: `readium-paged-on` | `readium-scroll-on`

Override class: Chrome (should be applied by any means necessary)

If the flag is not set, ReadiumCSS will fall back to the paged view.

### Font Family override

Acts as an explicit switch to override the publisher’s `font-family`.

```
--USER__fontOverride
```

Supported value: `readium-font-on`

Override class: None. This flag is required to change the `font-family` user setting.

To switch back to the publisher’s font, you can either set an empty string as a value or remove the property.

### Advanced Settings

Acts as an explicit switch to override the publisher’s styles.

If you provide users with a “Publisher’s styles” toggle, it must be enabled and disabled accordingly.

```
--USER__advancedSettings
``` 

Supported value: `readium-advanced-on`

Override class: None. This flag is required to apply the `font-family`, the `font-size` and/or advanced user settings.

To switch back to the publisher’s styles, you can either set an empty string as a value or remove the property. This will disable all advanced settings requiring the flag.

### Reading Modes

We currently have two reading modes for night and sepia.

```
--USER__appearance
```

Supported values: `readium-day-on` | `readium-sepia-on` | `readium-night-on`

Override class: Chrome (should be applied by any means necessary)

If the flag is not set, ReadiumCSS will fall back to the day mode.

### Filters

Please note night mode provides two extra specific variables: 

```
--USER__darkenFilter
```

Supported value: `readium-darken-on`

Override class: Chrome advanced (optional but should be applied by any means necessary if provided to users)

To disable the filter, you can either set an empty string as a value or remove the property.

```
--USER__invertFilter
```

Supported value: `readium-invert-on`

Override class: Chrome advanced (optional but should be applied by any means necessary if provided to users)

To disable the filter, you can either set an empty string as a value or remove the property.

### Accessibility Normalization

Users may want to normalize text (no bold, no italics, etc.) for accessibility reasons, using a non a11y-specific typeface.

```
--USER__a11yNormalize
```

Supported value: `readium-a11y-on`

Required flag: `--USER__fontOverride: readium-font-on`

Override class: User settings advanced (optional but should be applied by any means necessary if provided to users)

To disable the normalization, you can either set an empty string as a value or remove the property.

## List of variables 

### Layout 

The user can set the number of columns and page margins.

#### Number of columns

```
--USER__colCount
```

Possible values: `1` | `2` | `auto` (default)

Required flag: none

Override class: Chrome advanced (optional but should be applied by any means necessary if provided to users)

To reset, change the value to `auto`.

By default, this setting behaves as an `auto` value, it will switch to 1 or 2 columns depending on the minimum `width` available and `font-size`.

It is up to implementers to decide whether they want this setting to be available and override any configuration or only some (e.g. setting only available in landscape and/or larger screens).

#### Page margins

```
--USER__pageMargins
```

Recommended values: a range from `0.5` to `2`.  Increments are left to implementers’ judgment.

Required flag: none

Override class: Chrome advanced (optional but should be applied by any means necessary if provided to users)

To reset, change the value to `1`.

The user margins are a factor of the reference we set.

This will probably be fine-tuned in the next version (beta).

### Themes (background and text colors)

The user can set a `background-color` and `color`. 

The following two variables must be used together.

```
--USER__backgroundColor
--USER__textColor
```

Possible values: Color HEX (e.g. `#FFFFFF`), `rgb(a)`, `hsl`.

Required flag: none

Override class: Chrome advanced (optional but should be applied by any means necessary if provided to users)

To reset, remove both variables.

### Hyphenation and justification

The user can set `text-align` and `hyphens` for body copy contents.

#### Text align

```
--USER__textAlign
```

Possible values: `left` (LTR) or `right` (RTL) | `justify`

Required flag: `--USER__advancedSettings: readium-advanced-on`

Override class: User settings advanced (optional but should be applied by any means necessary if provided to users)

Note: the value `start` can be used to let all rendering engines, excepted Trident (IE11) and EdgeHTML (Edge), automatically deal with `left` and `right` based on the direction (`dir` attribute) set for the document and its nested elements.

#### Hyphens

```
--USER__bodyHyphens
```

Possible Values: `auto` | `none`

Required flag: `--USER__advancedSettings: readium-advanced-on`

Override class: User settings advanced (optional but should be applied by any means necessary if provided to users)

**Warning:** for the time being, automatic hyphenation won’t work if you are using the Blink rendering engine (either via Chrome or a Webview, including Electron’s) on ChromeOS, Linux and Windows. It indeed is not implemented yet and we recommend not trying to polyfill it using JavaScript as it will create a11y issues, especially with screen readers. 

**As a consequence, we strongly advise implementers against providing users with an hyphenation-specific setting if targeting this rendering engine.**

### Typography

The user can set `font-family`, `font-size` and `line-height` for body copy contents.

#### Font family

```
--USER__fontFamily
```

Possible values: `var(--RS__oldStyleTf)` | `var(--RS__modernTf)` | `var(--RS__sansTf)` | `var(--RS__humanistTf)` | `<string>`

For Japanese, possible values become: `var(--RS__serif-ja)` (horizontal writing) | `var(--RS__sans-serif-ja)` (horizontal writing) | `var(--RS__serif-ja-v)` (vertical writing) | `var(--RS__sans-serif-ja-v)` (vertical writing) | `<string>`

Required flag: `--USER__fontOverride: readium-font-on`

Override class: User settings (should be applied by any means necessary)

To reset, remove the required flag.

#### Font size

We have to normalize `font-size` for body copy elements so that it can work in pure CSS. In order to do so, we are using a normalize. The `--USER__advancedSettings: readium-advanced-on` inline style must be set for `html` in order for the font-size setting to work.

Although it might be an issue to authors at first sight, this approach is backed by actual data.

```
--USER__fontSize
```

Recommended values: a range from `75%` to `250%`. Increments are left to implementers’ judgment.

Required flag: `--USER__advancedSettings: readium-advanced-on`

Override class: User settings (should be applied by any means necessary)

#### Type scale

If the `--USER__advancedSettings: readium-advanced-on` style is set for `html`, you can customize the `font-size` of all elements using a factor. This may come in handy on mobile devices, if the user sets a large font-size.

```
--USER__typeScale
```

Possible values: `1` | `1.067` | `1.125` | `1.2` (suggested default) | `1.25` | `1.333` | `1.414` | `1.5` | `1.618`

Required flag: `--USER__advancedSettings: readium-advanced-on`

Override class: User settings advanced (optional but should be applied by any means necessary if provided to users)

You can use different type scale values depending on the `font-size`. For instance, if the user sets a large one, you might want to decrease the type scale so that headings are not super large.

#### Line height

```
--USER__lineHeight
```

Recommended values: a range from `1` to `2`. Increments are left to implementers’ judgment.

Required flag: `--USER__advancedSettings: readium-advanced-on`

Override class: User settings advanced (optional but should be applied by any means necessary if provided to users)

### Paragraphs’ formatting

The user can set `margin-top`, `margin-bottom` and `text-indent` for paragraphs. 

#### Paragraphs’ spacing

```
--USER__paraSpacing
```

Recommended values: a range from `0` to `2rem`. Increments are left to implementers’ judgment.

Required flag: `--USER__advancedSettings: readium-advanced-on`

Override class: User settings advanced (optional but should be applied by any means necessary if provided to users)

#### Paragraphs’ indent

```
--USER__paraIndent
```

Recommended values: a range from `0` to `3rem`. Increments are left to implementers’ judgment.

Required flag: `--USER__advancedSettings: readium-advanced-on`

Override class: User settings advanced (optional but should be applied by any means necessary if provided to users)

### Characters’ spacing

The user can set `word-spacing` and `letter-spacing` for headings and body copy contents.

#### Word spacing

```
--USER__wordSpacing
```

Recommended values: a range from `0` to `1rem`. Increments are left to implementers’ judgment.

Required flag: `--USER__advancedSettings: readium-advanced-on`

Override class: User settings advanced (optional but should be applied by any means necessary if provided to users)

#### Letter spacing

```
--USER__letterSpacing
```

Recommended values: a range from `0` to `0.5rem`. Increments are left to implementers’ judgment.

Required flag: `--USER__advancedSettings: readium-advanced-on`

Override class: User settings advanced (optional but should be applied by any means necessary if provided to users)

#### Arabic Ligatures

```
--USER__ligatures
```

Possible values: `none` | `common-ligatures`

Required flag: `--USER__advancedSettings: readium-advanced-on`

Override class: User settings advanced (optional but should be applied by any means necessary if provided to users)

## Themes

In this model, themes are just a set of user variables with specific values.

It becomes even easier to override them for the user as the values are already user settings.

In other words, think of preset and custom themes as a set of variables, which makes it easier to create, manipulate and cache them.

## Alternative options

There is not a lot of alternatives when it comes to managing user settings. Options include:

- appending/removing stylesheets (either `link` or `style` element) dynamically;
- traversing the DOM and appending inline styles using JavaScript;
- using classes or custom attributes for a limited subset of settings (e.g. reading modes);
- leveraging native features web views have to offer.

Please bear in mind Readium CSS provides a baseline, it resonably manages all those issues using CSS only. But if you want to offer users the most advanced experience there can be, you’ll end up with a mix of all those options.

For instance, you can’t really manage `text-align` perfectly if you don’t traverse the DOM to find elements for which the user setting should not apply, CSS has no way to retrieve those elements.

It is worth mentioning that at least some rendering engines are optimized to manage **global** CSS variables (i.e. the ones declared in `:root`) and reserve a special cache for faster lookup and updates. Changes should consequently be handled as inline styles in the `html` element if you want the best performance possible.

## User settings can be language-specific

It is important to note that the list of user settings you may provide users with can change depending on the primary language of the publication.

Indeed, it doesn’t make sense to have some user settings in some languages, and they would do more harm than good e.g. hyphens in CJK. Ideally, those settings should therefore be removed from the UI, or at least disabled, if needed.

### All languages other than Latin

Implementers will need to load different list of fonts based on the languages listed in [Default Fonts](../docs/CSS09-default_fonts.md).

The most complex issue is finding fonts for those languages, especially as mobile systems often ship with the minimum amount of fonts possible to support Indic, Arabic, Hebrew, CJK, etc. And when the platform provides an extended selection, users often have to download them beforehand.

The following is provided as guidance only:

1. the app should at least offer the publisher’s font and the default (`var(--RS__baseFontFamily)`) for the language – which should work automatically if the correct language is set for each document;
2. if implementers want to extend the list:
   1. use pre-installed fonts if the system offers some;
   2. use downloadable fonts if the system offers some;
   3. carefully pick fonts supporting the language and the idiosyncrasies of its typography;
   4. fall back to [Google Noto Fonts](https://www.google.com/get/noto/).
3. users probably have fonts already installed, re-use those fonts if possible (advanced setting in which they can access or declare those fonts).

### Right to left scripts

User settings to disable are:

- `--USER__bodyHyphens`;
- `--USER__wordSpacing`;
- `--USER__letterSpacing`.

User settings to add are:

- `--USER__ligatures`.

### CJK

For Chinese, Japanese, and Korean, implementers must manage both horizontal and vertical writing modes, since the pagination model differs.

#### Horizontal writing mode

User settings to disable are: 

- `--USER__textAlign`;
- `--USER__bodyHyphens`;
- `--USER__paraIndent`;
- `--USER__wordSpacing`;
- `--USER__letterSpacing`.

#### Vertical writing mode

This also impacts the Mongolian script.

User settings to disable are:

- `--USER__colCount`;
- `--USER__textAlign`;
- `--USER__bodyHyphens`;
- `--USER__paraIndent`;
- `--USER__wordSpacing`;
- `--USER__letterSpacing`.