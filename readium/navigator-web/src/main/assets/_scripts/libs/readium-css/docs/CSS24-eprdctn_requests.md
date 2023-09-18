# E-production feedback and requests

[Implementers’ info]

One important part of the Readium CSS project is collecting feedback and queries from the e-production community (CSS authors). This is a summary.

## CSS Authors’ typical profile

The typical CSS author has been doing e-production for 3 years or more (76%), is primarily concerned about interoperability (80%) and feels like he/she gets at least CSS fundamentals (88%). Most of his/her queries are related to layouts you can do in print but you can’t in ebooks yet.

A few notes: 

- interoperability implies Kindle, back to Mobi7 on the north-american market (Mobi7 is ± HTML 3.2);
- specific versions of EPUB files for more advanced RS like iBooks or Readium exist, and this issue is partly related to distribution: authors don’t know the app the reader will use and don’t want risking rendering issues;
- sometimes, specific versions mean authors (and/or software) will fall back to EPUB 2 files, in order to get around support uncertainty; 
- one key is probably progressive enhancement techniques, but that’s more of an education/evangelization issue and not in implementations’ scope *per se*.

## Workflow and authoring

### Workflow is still print-centric

From the feedback we could get, DTP software is still prevalent (more than 2/3 of answers), but automation is a thing (XML and even Node.js). 

This means that should we provide new design options, change won’t happen overnight. Authors would have to add them to their workflow first (manual editing or automation upgrade), then authoring software would have to support them. 

What’s interesting is that most authors would appreciate having such design options (more than 75%, on two polls), but if there isn’t any side-effects in other apps (cf. interoperability). Of course that would require production moving to EPUB 3 ([EPUB 2 still makes up roughly 70% of all incoming content @ Kobo for instance](http://epubsecrets.com/from-inside-the-epub-ingestion-factory.php)).

So once again, we have an education/evangelization issue to deal with there.

### Authoring practices

CSS authoring varies greatly.

Almost 2/3 of authors use a custom stylesheet that they adjust for every book, very few actually rely on the styles output by their DTP tools. As a consequence, there’s no consensus when it comes to the CSS selectors authors use. It can be: 

- `p`
- `.class`
- `p.class`
- `div.class p.class`
- `[epub|type="semantic-inflection"] p`
- greatly-specified selectors with or without combinators (from actual samples).

Reasons include: 

- ease of use;
- complexity and variety of the publication’s contents;
- semantics (especially with `epub:type`);
- production being outsourced with no specification – i.e. this is how the contractor do things.

If we want to solve the user settings issue well, we’ll probably have to find clever and inventive ways, selectors specificity being a dead-end in practice. 

There’s [a proposal for user agent properties](https://github.com/w3c/csswg-drafts/issues/1693) and [a draft for customization](https://w3c.github.io/personalization-semantics/), they have some potential for upcoming files if EPUB gets a “do not touch my CSS” flag at some point – what’s already distributed is probably lost. With user agent properties, [authors could design user-centric stylesheets](https://gist.github.com/JayPanoz/c5bbf0bd7e53997d3a91d9c1be44a82f) so, in theory, RS could not override styles at all and just set values for those properties.

In the meantime, we have to “emulate” the cascade and resolve to `!important`. So that’s trying to make an unperfect mechanism into something more elegant, which requires a lot of fine-tuning.

## Worries and issues

### Worries

What bothers authors the most: 

1. the app overriding styles without an explicit demand from the user (a.k.a. changing a setting);
2. the complete lack of control over page layout (margins, background, etc.);
3. user overrides which are all or nothing.

Interestingly, reading modes’ adjustments and user settings exposure are not priorities – which doesn’t mean they aren’t concerns. There are two assumptions we could make: 

- providing readers with images filters in night mode is probably the best first step we can make since a significant part of authors might not use extra markup to deal with it on an image-per-image basis (especially if no other RS supports it);
- user settings exposure is probably one of the best practical ways to deal with non-binary user settings and overrides (cf. user agent properties proposal) so, once again, education/evangelization kicks in. 

### Issues

Top issues are, in order of priority: 

1. images’ sizing e.g. author’s sizing is not respected, image is cut-off;
2. broken media queries (which makes responsive design impossible);
3. lack of support for modern CSS;
4. Math.

This shouldn’t come as a surprise; those are long-standing issues in need of practical solutions (media queries don’t work in columned-content) and fixes (flexbox, grid, and modern layout specs have a lot of bugs in columns’ implementations). Our best bet there is collaborating with the [CSS-multicol spec editors](https://github.com/rachelandrew/multicol-wip) since we’re probably one of the major use cases for this spec.

## Popular requests

Unsurprisingly, a lot of requests are about layouts possible in print but not (yet) in e-books:

- full bleed (container, image, background-color);
- support for paged-media (running header/footer, float figures top/bottom, etc.);
- vertical centering and alignment;
- floated shapes (wrapping the image and not its bounding box);
- option to put contents like page numbers, icons, etc. in the margins;
- using a baseline grid;
- forcing the chapter title as the running header;
- proper support of page-breaks;
- media queries for reading modes (sepia, night, etc.);
- stylable popup footnotes.

A few notes: 

- in theory, putting contents in the margins and full-bleed are (almost) possible in Readium CSS: it depends on its implementation. Consequently, don’t be surprised if some authors make some experiments and raise issues;
- some requests are already possible in some rendering engines: vertical-centering and alignment, floated shapes, and using a baseline grid is already possible on some platforms (depends on the web view/browser used) so it is more of an authors’ issue;
- anything related to paged-media has had low traction at the browser vendors level so far, you’ll probably have to polyfill those features if you want to meet those expectations.

## Interoperability issues reported

We care deeply about interoperability. Although Readium CSS has been designed from scratch, a lot of research has been put into other Reading Systems in order to improve interop (and not introduce breaking changes/side-effects). To our knowledge, we’re even the first project openly tackling EPUB implementations’ interoperability. Indeed, one of our main goals is to provide authors with a solid bedrock so Readium CSS should not become an issue for them. 

A few interop issues have been reported: 

- page margins: some apps/devices have huge page margins, others don’t have any, it’s really hard for authors to know what they should be doing here (`margin: 0` or `margin: value`);
- support of existing specifications is lacking (the issue being that some existing specs are really hard to implement well in practice);
- images optimization (this could be related to `picture` and `srcset`);
- font obfuscation isn’t implemented everywhere, which is a huge issue for fixed-layout.