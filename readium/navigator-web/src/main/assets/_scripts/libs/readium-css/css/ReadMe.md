# Quickstart

Readium CSS is a set of reference stylesheets for EPUB Reading Systems. It provides styles for reflowable text: 

- a CSS normalize for EPUB contents;
- paged and scrolled views;
- default styles;
- reading modes (night, sepia, etc.);
- themes;
- user settings;
- media overlays and user highlights.

**Note:** Readium CSS stylesheets were not designed and should not be used for fixed-layout EPUB, nor other file formats like FB2, PRC, Mobi, TEI, etc.

## Important info

**TL;DR: use the stylesheets in the `css/dist` folder if you don’t need to customize Readium CSS.** All the flags and variables can then be taken at face value in the docs.

- The `src` files, which are modules, can’t be used AS-IS. They indeed have to be processed by PostCSS to create `dist` stylesheets.
- By default, those modules are daisy-chained and compiled into 3 `dist` stylesheets: 
    1. `ReadiumCSS-before.css`;
    2. `ReadiumCSS-default.css` (for unstyled ebooks);
    3. `ReadiumCSS-after.css`.
- We’re currently managing RTL and CJK (including horizontal and vertical writing) scripts using specific `dist` stylesheets:
    1. Right to Left stylesheets are distributed in an `rtl` folder;
    2. CJK horizontal stylesheets are distributed in an `cjk-horizontal` folder;
    3. CJK vertical stylesheets are distributed in an `cjk-vertical` folder.
- The build process is currently subpar as it has been designed for our default and must be changed entirely if you want to process and use modules directly – which would make sense with HTTP2 for instance.

## How To

Inject ReadiumCSS stylesheets into the `<head>` of (X)HTML documents. 

1. `ReadiumCSS-before.css`
2. Authors’ stylesheet(s) or `ReadiumCSS-default.css` for unstyled publications
3. `ReadiumCSS-after.css`

Check the [Stylesheets order doc](../docs/CSS06-stylesheets_order.md) for further details.

For RTL, you would then have to load the stylesheets in the `rtl` folder. Same for CJK. Check the [“Internationalization” doc](../docs/CSS16-internationalization.md) for guidance.

By default, we inject all stylesheets on load and rely on custom properties (a.k.a. CSS variables) set on `html` to apply user settings.

## Customize

ReadiumCSS ships with a `ReadiumCSS-config.css` file you can use to customize it a little bit. It allows implementers to:

1. define in which conditions the auto pagination model is used;
2. choose selectors for the user settings’ flags.

In order to provide this customization, we use custom media and custom selectors, which will hopefully become [standards implemented at some point](https://drafts.csswg.org/css-extensions/), but require PostCSS at the moment. Consequently, you’ll have to rebuild all `dist` stylesheets if you’re changing this file.

### Auto pagination model

The auto pagination model switches from 1 to 2 columns, and vice versa, when the conditions defined in `ReadiumCSS-config.css` are met. Further details about this model can be found in [“Injection and pagination” doc](../docs/CSS03-injection_and_pagination.md).

On desktop, `--responsive-columns` is the `min-width` at which the model must be used. Default is `60em`, a relative unit since it is responsive by default and will switch depending on the window’s dimensions and the font size.

Should you want it never or always applied, you can either define a `min-width` large or small enough, or remove the media queries entirely in `ReadiumCSS-pagination.css` and `ReadiumCSS-colNumber_pref.css`.

On mobile, `--min-device-columns` and `--max-device-columns` is the range of (minimum and maximum) device widths in which the model must be used. We are forcing the orientation in `landscape`.

We recommend not trying to apply it in portrait orientation because 2 columns will provide users with quite a terrible reading experience in this configuration.

Those custom medias may be extended at some point, depending on implementers’ feedbacks and issues.

### Flags for user settings

By default, we are using flags in the form of CSS variables to manage reading modes and user settings. But you might want to customize those flags in order to use custom attributes (`data-*`) or good old CSS classes.

A complete list of flags can be found in the [User preferences doc](../docs/CSS12-user_prefs.md).

As an example, if you want to use a CSS class for night mode, it could look like: 

```
@custom-selector :--night-mode .night-mode;
```

And if you want to use custom attributes for advanced settings, it could look like:

```
@custom-selector :--advanced-settings [data-settings="advanced"];
```

Both would then have to be appended to `html` at runtime.

Once again, you must rebuild `dist` stylesheets.

### Add and remove modules

We have to add and remove modules depending on the language/script of the publication so this need is covered out of the box.

In the `css/src` folder, you’ll find all the needed stylesheets you will process to `css/dist`. Those stylesheets contain a list of imports e.g. for `ReadiumCSS-before.css`:

```
@import "../ReadiumCSS-config.css";
@import "modules/ReadiumCSS-base.css";
@import "modules/ReadiumCSS-day_mode.css";
@import "modules/ReadiumCSS-fonts.css";
@import "modules/ReadiumCSS-html5patch.css";
@import "modules/ReadiumCSS-safeguards.css";
```

As a consequence, modules you want to use have to be listed in those `-before` and `-after` files. Then rebuild them using PostCSS.

Additional user settings MUST be added to the `user-settings-submodules` folder, and make the required flag clear, if applicable.

Please remember to take the cascade into account, as issues might arise just because the order has been changed or modules moved from `-before` to `-after` – in which case we won’t be able to reproduce and debug an issue if we don’t know this important detail. See [Stylesheets order doc](../docs/CSS06-stylesheets_order.md) for further details.

### Reading System variables

If you want to change the name of `--RS__` prefixed variables, you will have to change it in every module.

### User settings variables

If you want to change the name of `--USER__` prefixed variables, you will have to change it in every module.

## Build dist stylesheets

First, please make sure you have node.js installed. If you don’t, go [download it on the official page](https://nodejs.org/en/).

Then navigate to the `readium-css` folder in your terminal and type:

```
npm install
```

This will install all dependencies needed to build `dist` stylesheets.

Once you have customized `src` files, in the terminal type: 

```
npm run-script build
```

This will rebuild all `dist` stylesheets in their specific folder.

### Options

Other scripts are available: 

- `build:ltr` for default stylesheets (Left to Right);
- `build:rtl` for Right to Left stylesheets;
- `build:cjk` for CJK scripts;
- `build:vertical` for CJK and the Mongolian scripts in vertical writing.

### Additional info

Further details about the build and test processes can be found in [the npm doc](../docs/CSS22-npm.md).

## Manage User Settings

Currently, user settings are managed by setting CSS variables at the `:root` level (`html` element).

### Flags

At the top of each user settings submodule, you will find the required flag for the preference.

This flag is needed for the setting to work.

### User variables

All settings can be set using `--USER__` prefixed variables. Set those properties for `html` and the cascade will automatically do its job.

To set a variable:

```
var root = document.documentElement || document.getElementById("iframe-wrapper").contentWindow.document.documentElement; 

root.style.setProperty("--USER__var", "value");
```

To remove a variable:

```
var root = document.documentElement || document.getElementById("iframe-wrapper").contentWindow.document.documentElement; 

root.style.removeProperty("--USER__var");
```

Please note you must implement a fallback strategy if you want to support Internet Explorer 11 and early versions of Microsoft Edge.

### Examples

#### Changing hyphenation and justification

```
root.style.setProperty("--USER__advancedSettings", "readium-advanced-on");
root.style.setProperty("--USER__textAlign", "justify");
root.style.setProperty("--USER__bodyHyphens", "auto");
```

Of course this example is simplistic. You could for instance create an helper to set multiple properties at once.

#### Changing the type scale 

You might want to change the type scale in order to adjust the `font-size` of all elements to the size of the screen and the global `font-size` set by the user. It can indeed help solve overflow issues for long words in headings, ridiculously large sizes, etc.

```
root.style.setProperty("--USER__advancedSettings", "readium-advanced-on");
root.style.setProperty("--USER__typeScale", "1.067");
```

## Create Themes

In this model, themes are a set of user settings you can store and retrieve. Add the properties to `html` and you get a theme.

Check the [User Preferences doc](../docs/CSS12-user_prefs.md) for a list of available user variables.