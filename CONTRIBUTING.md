# Contributing to the Readium Kotlin Toolkit

First and foremost, thanks for your interest! 🙏 We need contributors like you to help bring this project to fruition.

We welcome many kind of contributions such as improving the documentation, submitting bug reports and feature requests, or writing code.

## Writing code

### Coding standard

We use [`ktlint`](https://github.com/pinterest/ktlint) to ensure code formatting and avoid bikeshedding.

Before submitting a PR, save yourself some trouble by automatically formatting the code with `make format` from the project's root directory.

### Modifying the EPUB Navigator's JavaScript layer

The EPUB navigator injects a set of JavaScript files into a publication's resources, exposing a JavaScript API to the `WebView` under the `readium` global namespace. The JavaScript source code is located under [`readium/navigator/src/main/assets/_scripts`](readium/navigator/src/main/assets/_scripts).

`index-reflowable.js` is the root of the bundle injected in a reflowable EPUB's resources, while `index-fixed.js` is used for a fixed-layout EPUB's resources.

If you make any changes to the JavaScript files, you must regenerate the bundles embedded in the application. First, make sure you have [`corepack` installed](https://pnpm.io/installation#using-corepack). Then, run `make scripts` from the project's root directory.

