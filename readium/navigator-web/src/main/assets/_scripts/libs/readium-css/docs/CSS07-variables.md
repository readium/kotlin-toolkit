# How to use CSS custom properties (a.k.a. variables)

[Implementers’ doc]

Note: CSS variables **are not and won’t be supported in IE11.** You can either create a static stylesheet for all UA browsers or decide to leverage them in most UA browsers + use a JS fallback for IE11 if you want to support this browser – you can test for CSS variables support in JS.

## What are CSS variables?

CSS variables allow an author to assign arbitrary values to a property with an author-chosen name. This custom property must start with `--`. The `var()` function then allows the author to use those values elsewhere in the document.

```
:root {
  --primaryColor: red;
}

a {
  color: var(--primaryColor);
}

button {
  color: white;
  background-color: var(--primaryColor);
}

```

If you’re familiar with CSS pre-processors (LESS, SASS, Stylus, etc.), you’re probably using variables already. The major difference is that CSS variables are supported in the browser and, consequently, you can access and edit them in real-time using JavaScript.

* * *

Global references:

- [CSS Custom Properties spec](https://drafts.csswg.org/css-variables/)
- [Can I Use](http://caniuse.com/#feat=css-variables)
- [CSS Variables: Why should you care](https://developers.google.com/web/updates/2016/02/css-variables-why-should-you-care) — developers.google.com
- [CSS Custom Properties in Microsoft Edge](https://blogs.windows.com/msedgedev/2017/03/24/css-custom-properties/#fjMpiOekgjsGVp4S.97)
- [PostCSS plugin to transform CSS variables into a static representation](https://www.npmjs.com/package/postcss-css-variables)
- [JS Perf — CSS Variables vs. Inline Styles](https://jsperf.com/css-variables-vs-inline-styles)
- [CSS Variables are a game changer](http://slides.com/gregoradams/css-variables-are-a-game-changer#/)

## Cascade and inheritance

Custom properties follow standard cascading rules. In other words, you can define the same property at different levels of specificity. 

```
:root {
  --fontSize: 16px;
}

aside {
  --fontSize: 12px;
}

body, aside {
  font-size: var(--fontSize);
  /* Will be 16px for body and 12px for aside */
}

```

You can also use them in media queries.

```
:root {
  --defaultFontSize: 16px;
}

@media screen and (max-width: 560px) {
  :root {
    --defaultFontSize: 14px;
  }
}

html {
  font-size: var(--defaultFontSize);
  /* Will be 14px if the viewport max-width < 560px and 16px otherwise */
}

```

To sum things up, variables bring a lot of flexibility and customization to CSS. It can also simplify your stylesheets and make them much more maintainable. 

## Fallback

In case you want to use them, you’ll have to hardcode values for IE11 (and Edge 14).

```
:root {
  --primaryColor: red;
}

a {
  color: red; /* Fallback */
  color: var(--primaryColor);
}

button {
  color: white;
  background-color: red; /* Fallback */
  background-color: var(--primaryColor);
}
```

That could be managed via JavaScript though, so that you don’t need to maintain fallbacks in pure CSS.

It is important to note the `var()` function provides a fallback mechanism itself, in case your custom property is not set. 

The function looks like this: 

```
var(<custom-property-name> [, <declaration-value> ]? )
```

```
:root {
  /* No --primaryColor in there */
}

a {
  color: var(--primaryColor, red); /* In which red is the fallback */
}

button {
  color: white;
  background-color: var(--primaryColor, red); /* In which red is the fallback */
}
```

That could be useful for theming for instance, by setting default values as fallbacks then declaring custom properties using JavaScript.

Although browsers have been optimized to deal with CSS variables, please note performance might suffer in some browsers if you have a lot of variables with a fallback.

## Using CSS variables in JavaScript

### Feature detect

[As seen in this article by Michael Scharnagl](https://justmarkup.com/log/2016/02/theme-switcher-using-css-custom-properties/), if you want to test for support, you can do the following:

```
if (window.CSS && window.CSS.supports && window.CSS.supports('--a', 0)) {
  // CSS custom properties supported.
} else {
  // CSS custom properties not supported
}
```

Since CSS feature queries are not supported by IE11, the previous snippet will return false.

It will return true for browsers which support feature queries since `@supports` only checks if the syntax is correct (and don’t bother checking if the custom property exists).

### Retrieve values

You can retrieve values by using `getPropertyValue("--name_of_the_variable")` (it is a computed style).

```
var myColor = window.getComputedStyle(document.documentElement).getPropertyValue("--textColor");
```

### Set values

You can set values by using `setProperty("--name_of_the_variable", "value")`.

```
document.documentElement.style.setProperty("--textColor", "#000");
```

If you’re appending a style element in the `head` of the document, you can also change the value in there, especially if you have a lot of elements to manage. For instance, if a `--maxImageHeight` variable is defined in the CSS for `img`, you could do the following:

```
var newStyles = document.createElement('style');
newStyles.id = "overrides";

var docHeight = window.getComputedStyle(document.body).getPropertyValue("height") + "px";
var newMaxImageHeight = document.createTextNode("img {--maxImageHeight: " + docHeight + ";}");

newStyles.appendChild(newMaxImageHeight);
document.head.appendChild(newStyles);
```

This will override the value in the original CSS since the `img` selector is more specific. It is important to notice you could hardcode styles here for browsers which don’t support variables (adding `max-height: docHeight` to the textNode you’re appending in the stylesheet).

## Interesting hacks

CSS variables offer a lot of flexibility. 

### Avoid JS concatenation

[As seen in this article by Zack Bloom](https://eager.io/blog/communicating-between-javascript-and-css-with-css-variables/), if you want to avoid managing concatenation (value + unit) in JavaScript, you could use the `calc()` function in CSS.

```
.element {
  height: calc(var(--element-height) * 1px);
}
```

### Testing active breakpoints

As seen in [this article by Eric Ponto](https://www.ericponto.com/blog/2014/09/17/share-css-variables-with-javascript/), you could create a `--breakpoint` variable to “emulate” something like container/element queries.

```
body {
  --breakpoint: medium;
}

@media screen and (max-width: 30em) {
  body {
    --breakpoint: small;
  }
}
@media screen and (min-width: 50em) {
  body {
    --breakpoint: large;
  }
}
```

Then create a function to return the breakpoint and append styles accordingly.

You could probably leverage such a hack to implement some kind of `column-width` media query in paged view, but it would require CSS authors to provide specific stylesheets with this non-standard media as a `link` attribute – otherwise, performance will be really really bad as you would have to hijack the entire stylesheet to retrieve media queries first.

### Setting another variable as a value

[As seen in the developers.google.com article](https://developers.google.com/web/updates/2016/02/css-variables-why-should-you-care), you can set the value of the variable to refer to another variable at runtime by using the `var()` function in your call to `setProperty()`.

```
document.documentElement.style.setProperty("--primary-color", "var(--secondary-color)");
```

### Abusing CSS variables to make them read and acted on by JavaScript

[As seen in the spec itself (example 3)](https://drafts.csswg.org/css-variables/#syntax), you could create values which sole purpose is to be read and acted on by JavaScript.

While the following variable would obviously be useless as a variable (invalid value):

```
--foo: if(x > 5) this.width = 10;
```

You could use it in JavaScript since it’s still a valid custom property.

### User themes

You could even go the extra mile and provide an “import theme” feature or implement custom styles inputs (color pickers, font family, etc.) in the UI so that users can create their own theme on the fly, then manage it more easily (storing the user theme as a style element with an id in `localStorage` for instance).

[See this CSS-Tricks’ article for use cases and demos](https://css-tricks.com/css-custom-properties-theming/).