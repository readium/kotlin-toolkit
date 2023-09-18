# List of proposals and specs impacting Readium CSS

[Implementers’ doc]

Here is a list of all the specs and proposals which are likely to impact Readium CSS in the future. Some of them are still in their early days but you’ll probably have to review implementations once they are clearly defined and Readium CSS is updated accordingly.

The [W3C CSSWG-Drafts repo](https://github.com/w3c/csswg-drafts/issues) is a good way to keep up. 

Readium CSS’ fundamental approach has been reviewed and should be future proof, but you’ll have to keep the following in mind since EPUB 3.2 is now using CSS snapshots – those specs and proposals are very likely to be listed in future snapshots. 

## Media Queries Level 5

- [Source](https://drafts.csswg.org/mediaqueries-5/)
- Impact: Global (a11y)
- [CSSWG Issues](https://github.com/w3c/csswg-drafts/issues?utf8=✓&q=is%3Aissue%20is%3Aopen%20%5Bmediaqueries%5D%20)

## Logical Properties and Values

- [Source](https://drafts.csswg.org/css-logical/)
- Impact: Global (esp. vertical writing and RTL)
- [CSSWG Issues](https://github.com/w3c/csswg-drafts/issues?utf8=✓&q=%5Bcss-logical-1%5D)

## CSS Extensions

- [Source](https://drafts.csswg.org/css-extensions/)
- Impact: Global (CSS architecture)
- [CSSWG issues](https://github.com/w3c/csswg-drafts/issues?utf8=✓&q=%5Bcss-extensions%5D)
- Note: this specification defines methods for extending several CSS features (custom selectors, custom properties, custom functions, custom combinators, custom @ rules). 

## Calc notation: min() and max()

- [Source](https://drafts.csswg.org/css-values/#calc-notation)
- Impact: Global
- Note: this could help us define floor and ceiling values for a lot of configurations (e.g. have a ceiling for headings `font-size` when a large user `font-size` is set)

## User Agent Properties

- [Source](https://github.com/w3c/csswg-drafts/issues/1693)
- Impact: Default, Themes, User Settings

## CSS Rhythmic Sizing

- [Source](https://www.w3.org/TR/css-rhythm-1/)
- Impact: Pagination, User settings
- [CSSWG Issues](https://github.com/w3c/csswg-drafts/issues?utf8=✓&q=is%3Aissue%20is%3Aopen%20%5Bcss-rhythm%5D)

## CSS Fonts Module Level 4

- [Source](https://www.w3.org/TR/css-fonts-4/)
- Impact: Default (Variable fonts), User settings (`font-min|max-size`)
- [CSSWG Issues](https://github.com/w3c/csswg-drafts/issues?utf8=✓&q=is%3Aissue%20is%3Aopen%20%5Bcss-fonts%5D%20)

## COGA Semantics to Enable Personalization

- [Source](https://w3c.github.io/personalization-semantics/)
- Impact: Implementations, User settings ([related draft](https://w3c.github.io/personalization-semantics/user-settings))
- Related TF: PWG Personalization Task Force

## Multicol editing

- [Source](https://github.com/rachelandrew/multicol-wip)
- Impact: Pagination
- [CSSWG Issues](https://github.com/w3c/csswg-drafts/issues?utf8=✓&q=is%3Aissue%20is%3Aopen%20%5Bcss-multicol%5D)

## W3C i18n

- [Source](https://www.w3.org/standards/webdesign/i18n)
- Impact: CJK & RTL

## CSSOM (a.k.a. Houdini)

- [Source](https://drafts.csswg.org/cssom/)
- Impact: Implementations
- [CSSWG Issues](https://github.com/w3c/csswg-drafts/issues?utf8=✓&q=is%3Aissue%20is%3Aopen%20%5Bcssom%5D)