# Readium CSS Further Details: Explaining the CSS Voodoo

[Implementers’ doc]

Some parts of Readium CSS might feel like CSS voodoo at first sight. The goal of this document is to clarify how those parts actually work.

## The Internal Framework

Readium CSS ships with a minimum viable framework to typeset unstyled publications (`ReadiumCSS-default.css`). Layout is managed by the “base” styles, used for all publications.

This stylesheet is based on [HTML Suggested Rendering](https://www.w3.org/TR/html/rendering.html#rendering). Consequently, it will work if the markup is semantic.

If you’re familiar with CSS preprocessors (LESS, SASS, Stylus, etc.), you already know how to use it. It indeed relies on variables and functions, which are available in vanilla CSS today.

What’s noteworthy is that you can customize the entire publication by setting CSS variables (a.k.a. custom properties) and either use AS-IS at runtime or compile as a static representation before runtime (PostCSS config coming soon).

The stylesheet will then use those variables defined at the `:root` level. 

Finally, a simplified version of the `font-size` normalize is embedded in this stylesheet (see next section).

### Variables you can set

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

Possible values: `1` | `1.067` | `1.125` (suggested default) | `1.2` | `1.25` | `1.333` | `1.414` | `1.5` | `1.618`

The suggested default will accomodate most configurations, from small to large screens.  Please note it is different from the `--USER__typeScale` suggested default, which was designed to accomodate the publisher’s styles.

* * *

```
--RS__baseFontSize
```

The default `font-size` for body copy. It will serve as a reference for all related computations.

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

## Font Size Normalize and the Type Scale

The assumption is that at least basic semantic elements’ `font-size` can be interpolated to a type scale, with minimal variation (±1–2 pixels difference for each element) in comparison to authors’ styles.

We’re using the `calc()` function to emulate the following: 

```
h1   = base font-size × typeScale ^ 3 (power of 3)
h2   = base font-size × typeScale ^ 2 (power of 2)
h3   = base font-size × typeScale
h4   = base font-size
body = base font-size
```

In which `typeScale` can be customized to match the actual scale used in the publication. There lies the flexibility of this normalize.

Getting the type scale is a one-two punch:

1. `parseInt` of the computed `font-size` for a heading and the base (division = result)*
2. `Math.pow(result, 1/n)`, in which `n` is `3` for `h1` (cubic root) and `2` for `h2` (square root).

\* `h3` stops here.

Then the `font-size` for each heading and body copy element will be recomputed based on this type scale.

Of course this approach is limited, there is little we can do to account for ids, classes, etc.

But it can at least be used for themes, or an opt-in user setting.

## Dynamic leading

Readium CSS automagically finds the ideal `line-height` of the current font and `font-size` in use if the author hasn’t set an explicit value.

We’re using the current algorithm in the `calc()` function:

```
(1em + (2ex - 1ch) - ((1rem - 16px) * 0.1667)) * var(--RS__lineHeightCompensation))
```

Therefore, the `line-height` is:

1. `1em` = the size of the `font-size`;
2. `2ex - 1ch` = 2 x-height - 1 character width (`0`), in order to take the typeface’s proportions into account e.g. if the font has a small x-height, leading will be more solid, and vice versa;
3. `1rem - 16px` = the current user’s `font-size` minus the one at `100%` (base `font-size`);
4. `0.1667` = a scale which has been defined from an optimal range ([it is a magic number](https://css-tricks.com/magic-numbers-in-css/) which has been retro-engineered from a set containing hundreds of samples);
5. `var(--RS__lineHeightCompensation)` is a factor (integer) to compensate a less than ideal `line-height`, especially in non-Latin scripts, where fonts tend to be square-ish.

This isn’t a perfect solution though, and this algorithm may be revisited in the future. Indeed, caveats are:

- leading also depends on line-length, which is not addressed in the algorithm itself, line-length must therefore be constrained by other means – we couldn’t rely on the viewport `width` anyway;
- sometimes, the User Agent default `font-size` is not `16px` (e.g. Kindle experimental browser, user setting a larger `font-size`, etc.);
- `0.1667` feels like a magic number, or at least a weird number as it was retrofitted after testing hundreds of typefaces;
- it’s clearly a natural logarithmic function i.e. it increases rapidly and then slows towards a zero rate of change, which will give mediocre results for very large font sizes (e.g. `400%`);
- it doesn’t work ideally for slab serif fonts with a large x-height, thick stroke and medium character width i.e. square-ish metrics – we don’t have any in the default font-stacks so we had to introduce a compensation factor to address this particular issue.

## Conditional Selectors for user settings

User settings are appended on load and won’t have any effect until a user-setting variable is set.

In order to do that, we must use “conditional selectors.” Problem is there is no `if/else statements` in CSS so how do we achieve this?

```
:root[style*="--USER__variable"]
```

Attribute selectors with pseudo-regex.

As soon as you set a property and its required flag to the `html` (or root) element, the user setting applies.

Whenever needed, we’re leveraging explicit inheritance (`inherit` value) so that the `:root` value can be passed throughout the DOM.

We’re also relying on the `:not()` pseudo-class to exclude some elements. Think of it as the following command prompt: “make all elements in the DOM inherit the value, excepted this one and that one.”

[Performance of those selectors](https://benfrain.com/css-performance-revisited-selectors-bloat-expensive-styles/) should not necessarily be a major concern. Indeed, authors’ stylesheets (weight, complexity, etc.) and expensive properties will have a much larger impact on performance.

The biggest issue with this is that it requires some time to get accustomed to, as it objectively feels like some kind of CSS trick borderline to CSS voodoo. It has proved reliable during testing though, and already deals with user settings issues other Reading Systems might have had troubles addressing so far. In addition, it automagically forces the necessary recalc some user settings absolutely require.

## Themes

In this design model, themes can be a simple set of user settings. They consequently can be treated as objects you can parse to add properties to the `html` (root) element, and stringify to save/cache if the user creates a custom theme. No extra stylesheet needed.