# Readium CSS Fundamental Concepts

[Implementers’ doc] [Authors’ info]

This document serves as an introduction, it explains the fundamental concepts used in designing Readium CSS.

## 1. We’re the User Agent

**In the context of EPUB, we must assume the role of the User Agent.**

Indeed, EPUB is not supported natively so we must build on top of web browsers’ rendering engine.

With such a role comes great responsibility: we must try to behave like browsers are behaving – i.e. be liberal in what we accept from authors –, and find a sensible balance between authors’ stylesheets and users’ overrides.

Authors and users have expectations though, and we had to take those into account. For instance, they are used to having EPUB contents laid out in a paged view, with reading modes (paper, sepia, night) and a minimal set of common settings.

By serving as a User Agent, we must make sure:

1. authors’ EPUB contents are rendered as expected (e.g. their styles are scoped, we don’t override them unless strictly necessary, etc.);
2. we do serve as a user’s agent by providing a Graphical User Interface to writing a user stylesheet (i.e. user settings) and applying those styles as expected.

## 2. Aim at Interoperability

Interoperability is the logical result of the User Agent role we’re claiming.

We’ve done our utmost to follow EPUB and CSS recommendations to the letter, but it vastly depends on practical implementations. As a consequence, Readium CSS ships with its own recommendations, so that you don’t need to check every related specification yourself.

Besides, we’ve checked other major EPUB implementations to provide authors with the best interoperability possible. Indeed, they may design their stylesheet against those implementations so we gave priority to building a reliable environment. 

We wanted to bring EPUB closer to the web though, and decided to leverage HTML5 Suggested Rendering and improve the current situation whenever possible.

## 3. Leverage Modern CSS

CSS has a lot to offer nowadays, and it can help solve complex issues in simpler ways.

Readium CSS has been be designed following 4 core principles:

1. **Modularity:** Readium CSS is not a monolithic stylesheet but a set of modules;
2. **Separation of Reading System’s Concerns:** those modules are task-oriented e.g. paginate, apply default styles, intercept styles for reading modes or user settings, apply a reading mode or user setting, apply a theme, etc.;
3. **Daisy-chainability:** those modules can be loaded and daisy-chained (cascade) depending on conditions;
4. **Customization:** modules can be customized either before or during runtime (CSS variables), which implies themes can be generated within minutes.

## 4. Openness and Transparency

This last principle is important since the relationship between authors and Reading Systems’ developers has not been really great so far.

Therefore, feedback has been collected, samples have been used all along development, and decisions impacting authors have been openly discussed. All of this has been publicly documented during research and development. 

More importantly, we must clearly state what is UA styles and user styles, and why `!important` is used. This process has a significant impact when it comes to transparency since it affects the cascade itself (see user settings recommendations). Should an author file an issue scoped to your implementation, please take the time to explain why you decided to differ and use `!important`, especially as such a decision can likely be backed by users’ feedback in many cases.