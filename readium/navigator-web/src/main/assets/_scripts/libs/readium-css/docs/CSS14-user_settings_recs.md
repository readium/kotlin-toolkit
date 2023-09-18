# Recommendations for User Settings Management

[Implementers’ doc] [Authors’ doc]

User settings are a complex issue as CSS authoring is liberal by nature.

In theory, this issue should be easily solvable using UA and user stylesheets as [they turn precedence upside down for `!important`](https://www.w3.org/TR/css3-cascade/#cascading). In practice, we don’t have any universal mechanism to set a stylesheet as an UA or user stylesheet: all the stylesheets we manipulate are created equal in scope; they are considered authors’ stylesheets by the rendering engine.

The following list is the order of priority we should emulate: 

1. important user agent declarations;
2. important user declarations;
3. important author declarations;
4. normal author declarations;
5. normal user declarations;
6. normal user agent declarations.

## Managing conflicts

We’re referring to the [W3C’s priority of constituencies](https://www.w3.org/TR/html-design-principles/#priority-of-constituencies). In other words: 

```
Users’ styles > Authors’ styles > Implementers’ styles
```

Which is why conceptualizing this issue as intents may help.

Typography is not just a set of settings though, it’s a complex system in which some settings impact others. For instance, font-size impacts line-height and line-length, which impacts the number of columns.

It is up to implementers to pick their preferred approach (minimal set, presets, minimal set/presets with advanced settings, maximal set) since it is primarily an UX issue.

## !important

### User’s intent

Does not apply as we basically manage important declarations for them.

### Author’s intent

In theory, using `!important` is a clear intent the author is willing to enforce some specific style.

### What do do

In practice, `!important` may be abused, it may be a quick fix to solve a cascading issue – and the author didn’t bother taking user settings into account –, it may be used to enforce some styles in Reading Systems overridding a lot of styles by default, etc.

This case is a complex one. We’re designing Readium CSS with the minimum amount of overrides possible but authors’ `!important` may conflict with some user settings. Please refer to the following cases.

## Number of columns

### User’s intent

The user intends to set a personnal preference. They may be used to reading newspapers, where the column length is quite small, but that can create extra issues on the implementers’ side.

### Author’s intent

Does not apply to authors.

### What to do

It depends on the implementers’ approach (presets with typographic rules as references Vs. the user can override on a setting-by-setting basis). 

## Page margins

### User’s intent

The user intends to adjust line-length.

### Author’s intent

We have to override margins for pagination so the most important style is `max-width`, if set. 

### What to do

The author intends to limit line-length, but it is unclear they are doing it because some Reading Systems don’t or because they want to enforce one specific (max) line-length.

## Reading modes

### User’s intent

The user intends to improve visual comfort.

### Author’s intent

Does not apply to authors but it could if we create a public “API” for authors to express intents related to reading modes. 

Since authors have filed very few use cases at the moment, this is highly conditional.

### What to do

Implementers should take two main variables into account: contrast and luminosity, which means they might want to invert some images or get rid of backgrounds in night mode. 

Inverting images that should be inverted in night mode can’t be done well without an author’s intent though.

Please note Readium CSS provides two extra options to manage images in night mode: 

1. darken;
2. invert.

Those are two opt-in mechanisms for users, ideally set on a book-by-book basis since they may work well with some publications but badly with others.

## Font family (typeface)

### User’s intent

The user intends to at least override the typeface used for body copy.

For a11y-related typefaces (Open Dyslexic, bold style of sans-serif family), the user probably intends to replace headings, scripts and italics as well.

### Author’s intent

Declaring a `font-family` for `body`, `p` and `p.class` is so common that it should not be considered an intent *per se.* While `p.class` is debatable, it is so widespread in practice (some authoring tools output `font-family` for every paragraph style) that it can’t be considered an intent in pure CSS. Implementers indeed have to validate it against the document’s `font-family` using JavaScript to make sure it shouldn’t be overridden.

A different `font-family` can be set for headings (`h1`, `h2`, `h3`, etc.), `blockquote`, `i`, `span`, etc.

### What to do

A different `font-family` for specific elements should be considered a proper intent: a font may indeed be used to make the structure clearer, achieve visual effects (e.g. manuscript letter) or make sure different languages are displayed correctly.

For a11y-related settings (e.g. Open Dyslexic), you should override those elements too, as not doing it will impact the reading experience negatively. Please make sure to provide a font-stack that covers a large amount of different languages though.

In any case, math and SVG contents should not be overridden.

## Font size

### User’s intent

The user intends to adjust the font-size, either because it is too small or too large by default.

### Author’s intent

This may be considered an intent if a `font-family` is set for body copy.

### What do do

The author could be trying to compensate for a smaller or larger x-height. 

You might want to normalize `font-size` (*à la iBooks*) if the user changes the typeface.

## Line height (leading)

### User’s intent

The user intends to adjust line height, either because it is too solid or too loose by default.

### Author’s intent

The author may be trying to enforce vertical rhythm but you really can’t tell if you don’t analyze the entire stylesheet.

### What do do

Common elements for the body copy should be overridden (paragraphs, lists…).

## Text align (justification)

### User’s intent

The user intends to set a personnal preference. Although it is a bad practice in typography, some may prefer justified text without hyphenation.

### Author’s intent

If set for body copy, this is a publisher’s or author’s preference.

If explicitly set to `right` and `center` for paragraphs, to which `left` is added for headings, it is a clear intent.

### What do do

Clear intents should not be overridden. This requires JavaScript though, and it could be easier to manage exceptions than all the elements which must be overridden (less DOM manipulation).

Elements such as headings, tables, pre, etc. should not be overridden: declaring `text-align: justify` for those elements would indeed degrade legibility.

## Hyphenation

### User’s intent

The user intends to enable/disable justification, possibly depending on the default/author’s styles.

### Author’s intent

Since hyphenation works in combination with justification in proper typography, this should be considered a preference or the enforcement of a typographic rule. 

### What to do

Typography-wise, it is OK to hyphenate body copy with `text-align: left`, it is critical to hyphenate body copy with `text-align: justify`.

We are taking care of elements which should not be hyphenated in the patch stylesheet so that you don’t have to.

## Paragraphs’ formatting

### User’s intent

The user intends to change the paragraph styling i.e. `text-indent` and vertical margins. Problem is this can either be a preference or a specific need (e.g. dyslexia).

### Author’s intent

This is a publisher’s or author’s preference.

There indeed is two accepted options for styling paragraphs: 

1. vertical margins without indent;
2. indent without vertical margins.

### What to do

Ideally, paragraphs’ formatting should be handled as a whole. There is nothing preventing implementers to provide users with both settings though.

## Characters’ spacing

### User’s intent

The user intents to customize `word-spacing` and/or `letter-spacing`, which can help for dyslexia for instance.

### Author’s intent

It is very rare authors’ will use those CSS properties for body copy. They may use it for headings though (small capitals, large font-size, etc.).

### What to do

In any case, implementers should force those settings for body copy.

## Ligatures (Arabic & Persian scripts)

### User’s intent

The user intends to disable ligatures. This setting applies to the Arabic and Persian scripts, and is believed to help dyslexic readers as `word-spacing` in Latin.

### Author’s intent

In those scripts, ligatures will be enabled by default. It’s neither an intent or preference, it is just the way it works. 

### What to do

Implementers must enforce the user preference, as it is primarily related to a11y.