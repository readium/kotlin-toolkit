# Baseline for a11y-related user settings

[Implementers’ doc] [Authors’ info]

Our current baseline for user settings includes:

- night/sepia modes, and custom `background-color` and `color`;
- `text-align` and `hyphens`;
- `font-size` and `line-height`;
- a11y-related typefaces;
- `letter-` and `word-spacing`;
- removing italics, subscrips and superscripts, and drop caps.

We should probably consider additional options:

- forcing an outline for focused elements;
- unfloating elements;
- removing images.

Please note some or all of those settings may be added in Readium CSS at some point.

**A few notes:**

The a11y-related typefaces are not necessarily typefaces which were specifically designed for a11y. A typeface a reader is used to, like Verdana, Trebuchet, etc., can be a11y-related as well. From research, the more varied typefaces available, the better. There is no silver bullet there, some people don’t like Open Dyslexic for instance, and prefer sans-serif typefaces they are used to.

Removing italics, subs and sups, drop caps and floated elements should probably be automatic if the typeface or theme selected was explicitly designed for dyslexia. Additionnaly, the `--USER__a11yNormalize: readium-a11y-on` flag is available and bring this feature to other typefaces.

## OS settings

Some Operating Systems provide users with global settings. We must take those settings into account, which is the purpose of the “OS a11y” module. This module still needs improvements. 

We’ll have to make decisions about reading modes there: if the user sets the monochrome option, how are we supposed to deal with sepia and colors? If the user sets an inverted high-contrast mode, what happens in night mode? There can indeed be multiple users for the same computer, using the same account, and we should cover this  case out of the box.

## Current a11y Features on the market

There are features you might want to add to your Reading System so that it can cover the needs of all readers. 

Readium CSS can help but can’t manage all of those features. Indeed, CSS might fall short of your expectations, especially on reliability. In other words, CSS is not necessarily the best way to manage features which could be considered in its scope at first sight.

As a rule, CSS can be used for styling (colors, highlights, etc.) but can’t be used to target contents (text nodes) directly. As a consequence, you’ll need to add hooks in the DOM (extra markup, classes, attributes, etc.) for some features.

### Dyslexia

Luc Maumet (EDRLab) has conducted an analysis on the features available in dedicated software. Here are the features Readium CSS can manage and those it can’t.

#### Features in Readium CSS Scope

- Create and personalize a profile (customizable theme which can be saved and retrieved)
- Add space between words
- Add space between letters
- Change the typeface
- Offer a dyslexia typeface
- Change the font-size
- Improve line-height

#### Features outscoping Readium CSS

- Alternate colors for words
- Alternate colors for lines
- Highlight a specific letter
- Highlight uppercase letters
- Highlight punctuation
- Highlight syllabes
- Highlight consonants and vowels with different colors
- Text to Speech
- Highlight a specific sound in the text
- Export contents with the customized layout

## Vestibular disorders

There’s an a11y “issue” which is quite unknown, but may impact a lot of people, either temporarily or permanently: vestibular disorders.

To put it simply, some interactions can make the user dizzy, uneasy, and in some cases ruin her/his entire day, because the user feels like they can’t keep balance when walking or even staying up, or feel vertigo. [See this article](https://source.opennews.org/articles/motion-sick/) for further details and illustrations.

**At the very minimum, allow the user to disable animations which may create effects (e.g. page-transition animation).** But everything else will vary from implementation to implementation.

On iOS/Mac, there’s a system setting which has been implemented and [browsers increasingly support the prefers-reduced-motion media query](https://webkit.org/blog/7551/responsive-design-for-motion/) to design accordingly.

There’s little we can do about interactive EPUB contents themselves, but we can at least promote what Mozilla got wonderfully right from the start to authors, i.e. [an overlay asking users to set their preference](http://devtoolschallenger.com).

## Internationalization

Very little information on a11y is currently available in the [W3C Text Layout and Typography Requirements](https://w3c.github.io/typography/).

All we know at the moment is that, in the Arabic and Farsi scripts, disabling ligatures might help for dyslexia.