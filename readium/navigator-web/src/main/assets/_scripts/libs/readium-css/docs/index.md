## Introduction

Readium CSS is a set of reference stylesheets for EPUB Reading Systems, starting with Readium 2.

Readium CSS provides styles for reflowable text: 

- paged and scrolled views;
- a “patch” for HTML5 Suggested Rendering specific to publications (e.g. extra styles for hyphenation, breaks, etc.);
- default styles for unstyled ebooks;
- reading modes (day, night, and sepia);
- themes;
- user settings;
- media overlays and user highlights;
- a set of baselines and recommendations for accessibility, overrides, and internationalization.

**Readium CSS is currently in alpha** (version `1.0.0-beta.1`).

## Contents

{% include toc.html %}

## Download

[You can alternatively download this documentation as an EPUB file](https://github.com/readium/readium-css/raw/master/docs/ReadiumCSS_docs.epub).

## Utilities

We’ve been using house-made web apps during ReadiumCSS that we made available in the project’s repo:

- [Typeface Tester](utils/Typeface-tester.html), to help pick fonts for reading apps or alternatives for EPUB files;
- [Dynamic Leading Tester](utils/DynamicLeading-tester.html), to find the ideal line-height for a specified font.