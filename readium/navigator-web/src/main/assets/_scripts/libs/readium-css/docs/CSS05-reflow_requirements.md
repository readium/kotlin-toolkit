# Reading Systems Requirements for reflowable text

[Implementers’ doc] [Authors’ doc]

Source: [EPUB Content Documents 3.2](https://w3c.github.io/publ-epub-revision/epub32/spec/epub-contentdocs.html#sec-css)

There’s a few criteria Reading Systems must meet in order to be conformant: 

- it must support the official definition of CSS as described in the [W3C CSS Snapshot](https://www.w3.org/TR/CSS/);
- it must support all `-epub-` prefixed properties ([list](https://w3c.github.io/publ-epub-revision/epub32/spec/epub-contentdocs.html#sec-css-prefixed)).

In addition:

- it should support all applicable modules in CSS Snapshot which have reached at least Candidate Recommendation status and are widely implemented;
- its user agent stylesheet should support [HTML Suggested Rendering](https://www.w3.org/TR/html/rendering.html#rendering);
- it should respect authors CSS and user styles as defined in [Reading System overrides](https://www.w3.org/TR/html/rendering.html#rendering).

## CSS Snapshot

Since we’re relying on web browsers’ rendering engine and web views, this should not be an issue. Our main concern should be future-proofing Readium CSS to stay up-to-date with current/modern CSS authoring (cf. [issue #12](https://github.com/readium/readium-css/issues/12))

## Prefixed properties

Blink and Webkit rendering engines may support some but it is unclear how we can be 100% conformant there, as it would require parsing the CSS to replace `-epub-properties` with the standard ones if the author has not used them.

As a matter of fact, 

> Authors are advised to use CSS-native solutions for the removed properties where and when they are available.

and,

> Authors currently using these prefixed properties are advised to move to unprefixed versions as soon as support allows.

Proposal: let the rendering engine/web view deal with this. We don’t have Houdini yet and [parsing CSS is a heavy process](https://philipwalton.com/articles/the-dark-side-of-polyfilling-css/) altering performance pretty badly. 

## HTML Suggested Rendering

We made sure we can rely on UA Stylesheets and have created a patch for paged views.

The patch deals with: 

- fragmentation (widows, orphans, page breaks);
- hyphenation;
- open type features;
- borders;
- horizontal spacing;
- little normalization between UA stylesheets.

It doesn’t change the nature of HTML Suggested Rendering though, it only builds on it for EPUB rendering (adjustments).

## Reading System Overrides

What’s important: 

- users should be able to override authors’ styles as desired;
- reading systems should not override authors’ styles unless strictly necessary, in a way that preserves the Cascade.

We meet both requirements by default.

As regards the cascade, the following list is the priority order we should emulate: 

1. important user agent declarations;
2. important user declarations;
3. important author declarations;
4. normal author declarations;
5. normal user declarations;
6. normal user agent declarations.

Most user declarations have to use `!important` to make sure they are applied, which is conformant. However, we try to manage “normal author declarations” by carefully targeting selectors for each user setting.

Few user agent declarations have to use `!important` as well, in order to make sure the EPUB file displays as expected (pagination, page margins, etc.). In other words, those overrides are strictly necessary.

Please make sure to check the [“CSS User Setting Recommendations”](../docs/CSS14-user_settings_recs.md) and [“Stylesheets Order”](../docs/CSS06-stylesheets_order.md) to manage overrides as best as possible. 

## Notes

A conformant author’s stylesheet must not: 

- use the `direction` property;
- use the `unicode-bidi` property.