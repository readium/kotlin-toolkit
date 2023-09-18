# Install, test and build Readium CSS

[Implementers’ doc]

## Install and init references for regression tests

First, navigatate to the project’s folder in your terminal, 

```
cd path/to/readium-css
```

then type: 

```
npm install
```

This will install all dev dependencies needed and make npm scripts available to ease all processes you’ll need to run later.

Then, once the install is finished, type:

```
npm run test:ref
```

This will create reference screenshots for the CSS regression tests.

## Build

We are using PostCSS, a tool for transforming CSS with JavaScript. It comes with a vast amount of task-oriented plugins and allows authors to use modern specs which are not implemented yet.

- [PostCSS official website](http://postcss.org)
- [PostCSS tutorial](https://webdesign.tutsplus.com/tutorials/using-postcss-for-cross-browser-compatibility--cms-24567)
- [PostCSS plugins list](https://www.postcss.parts)

### PostCSS Dependencies

ReadiumCSS is relying on a PostCSS config to build `dist` stylesheets. If you `npm install` the repository, all those dependencies will be installed as well.

Here is the current list of dependencies: 

- stylelint ([link](https://stylelint.io/user-guide/postcss-plugin/));
- postcss-cli ([link](https://github.com/postcss/postcss-cli));
- postcss-import ([link](https://github.com/postcss/postcss-import));
- postcss-sorting ([link](https://github.com/hudochenkov/postcss-sorting));
- postcss-custom-media ([link](https://github.com/postcss/postcss-custom-media));
- postcss-custom-selectors ([link](https://github.com/postcss/postcss-custom-selectors));
- postcss-discard-comments ([link](https://github.com/ben-eb/postcss-discard-comments));
- postcss-css-variables ([link](https://github.com/MadLittleMods/postcss-css-variables)) [disabled];
- postcss-alter-property-value ([link](https://github.com/kunukn/postcss-alter-property-value)) [disabled].

### Build dist stylesheets

If you customize `ReadiumCSS-config.css`, you will have to rebuild stylesheets.

**Note:** the current build process is subpar – to say the least. Please feel free to improve it (gulp, grunt, etc.).

#### Available scripts

By default, the following scripts are available: 

- `build`, will build all stylesheets;
- `build:ltr`, will build default stylesheets (Left to Right scripts);
- `build:rtl`, will build stylesheets for Right to Left scripts;
- `build:cjk`, will build stylesheets for Chinese, Japanese, and Korean in horizontal writing mode; 
- `build:vertical`, will build stylesheets for Chinese, Japanese, Korean, and Mongolian in vertical writing mode.

Those scripts will overwrite the files in the `css/dist` folder, the stylesheets you’ll use in your app.

#### Usage

First navigate to the `readium-css` folder if you didn’t already, then…

```
npm run build
```

#### Building dist stylesheets for browsers which don’t support CSS variables

If you need to build stylesheets for IE11 or an early version of Edge (e.g. 14), then you can use most of ReadiumCSS, excepted user settings. You’ll consequently have to customize the `src`’s `ReadiumCSS-before.css`, `ReadiumCSS-default.css` and `ReadiumCSS-after.css` and remove the user settings submodules.

Then you must customize the selectors in `ReadiumCSS-config.js` and replace them with either CSS classes or custom attributes so that reading modes and flags can work as expected.

Finally you will have to enable the `postcss-css-variables` and `postcss-alter-property-value` in the `postcss.config.js` file to be found at the `src` folder’s root.

The following must be added to `plugins`: 

```
require("postcss-css-variables")({
  "preserve": true
}),
require("postcss-alter-property-value")({
  declarations: {
    "*": {
      task: "remove"
    , whenValueEquals: "undefined"
    }
  }
})
```

This will:

1. interpolate CSS variables into a static representation, while preserving variables for other browsers (`"preserve": true`);
2. remove static representations which can’t be interpolated and are `undefined` (`remove` task).

We recommend managing user settings via JavaScript in this case, especially as you can test support for CSS variables, as described in the [CSS Variables primer](../docs/CSS07-variables.md).

### Useful PostCSS plugins

Here is a list of additionnal PostCSS plugins which might prove useful to implementers.

- Unprefix EPUB properties: [EPUB interceptor](https://github.com/JayPanoz/postcss-epub-interceptor)
- Adding vendor prefixes: [Autoprefixer](https://github.com/postcss/autoprefixer)

## Test

Once you have build `dist` stylesheets, you can run regression tests using [Backstop.js](https://github.com/garris/BackstopJS).

It helps you check if you didn’t accidentally create a breaking change when customizing stylesheets, and make sure pagination, reading modes, and user settings work as expected.

### Config

You will find the configuration file, `backstop.json` at the root of the project. By default, it runs those tests for a smartphone (portrait) and a tablet (landscape) viewports using Chrome, but you can customize it to fit your needs.

For instance, if you don’t need to support mobile, you could modify `viewports`: 

```
"viewports": [
  {
    "label": "desktop small",
    "width": 800,
    "height": 600
  },
  {
    "label": "desktop large",
    "width": 1600,
    "height": 900
  }
]
```

And if you want to run tests using Webkit instead of Blink because you’re developing iOS apps:

```
"engine": "phantomjs"
```

### Test files

If you customize flags in `ReadiumCSS-config.css`, you must modify HTML files in the `tests` folder; user settings are indeed set as inline styles on the `html` element and are using the default flags.

### Available scripts

By default, the following scripts are available: 

- `test`, will run tests;
- `test:ref`, will create reference screenshots;
- `test:approve`, will update reference screenshots from the current test.

### Usage

First navigate to the `readium-css` folder if you didn’t already, then…

```
npm run test
```

The regression tests will run against the newly-created `dist` stylesheets, which is why you must build them beforehand.

Once all scenarios are tested for the viewports you created, which can take up to a minute, a report will automatically open in your browser.

If a unit test is marked as “failed”, it doesn’t necessarily mean the user setting failed, it just means you made a significant change which impacts rendering. Take a closer look at the diff, and if you’re happy with the result, head to the terminal and type:

```
npm run test:approve
```

This will make the current test screenshots the new reference for the next test.

**Note:** on some occasions, an error might happen during tests and the process won’t stop. Try `ctrl + c` to stop the current process and run the test again.