# User Preferences’ insights

[Implementers’ info] [Authors’ info] [WIP]

In case you don’t have solid data related to user preferences, here is some information to help you kickstart your UX.

It is worth noting this should not be considered a guide, this is just a list of interesting observations, notes and remarks. UX research is critical and there’s not a lot available for ebooks so far, so you’ll have to do it yourself to see what works and what doesn’t.

This is really tricky as **there may be a cultural factor to take into account**. In our research, results varied significantly from one market to another. Unfortunately, it is impossible to tell how important this cultural factor is compared to others. In other words, if you’re going global, it might be interesting to do some specific research on this.

## User Settings

When asked directly, the vast majority of users will say they prefer basic settings (reading modes, typeface, and font size) over other options (maximum amount of settings possible, themes, customizable themes). Customizable themes and advanced settings are equally popular, at a distant second place.

Once again, this is a complex UX issue and your mileage may vary. On Android for instance, there is a lot of apps with advanced settings as the default, and millions of people are using them daily. So it’s primarily about your UX proposal, people using it, etc.

Hopefully, Readium CSS was designed to take all those options into account: modules are take or leave, which implies you can build any UX from them.

## Typography

We’ve been running forms on this topic but answers can’t be taken at face value since they might well be biased – given the eprdctn and a11y communities on social networks, and the probable tech-savyness of people who came across those forms in the first place; what is the most interesting is the open-ended questions and how **you can interpret them**.

### People change the publisher’s typeface

People keeping the publisher’s typeface are in the minority (< 15%). Most users at least change the typeface occasionally (see reasons below), although they don’t necessarily do it for each ebook – some ebooks simply use the default typeface so it’s quite complex to measure.

What’s important is that **you’d better have some kind of bulletproof selection of RS typefaces.** “Bulletproof” is the keyword here, since it is difficult to imagine a selection that will satisfy everybody. There won’t be any silver bullet. 

Since more people systematically change the publisher’s typeface than those who never do it, **implementers might end up with a huge share of users systematically changing the typeface and could adjust the UX accordingly** (global setting applying the typeface in all books).

It also implies **you’d better have fonts providing with the largest characters set possible.**

### Why do people change the typeface? 

**Legibility.** We’re including dyslexia typefaces since it is kind of related to this issue.

In some way, **this justifies we must change the typeface for (at least) body copy by any means necessary – and absolutely everything for dys.**

As an aside, respondents didn’t hesitate to criticize the publisher/graphic designer when they feel they made an abysmal choice of typeface(s). **Please pick the typefaces you want to provide carefully.** There can be language subtleties to take into account, and pairs, ligatures, kerning, etc. might have been done well for one latin language but not other latin languages for instance – especially when those other languages are using diacritics.

Other reasons were given, the most interesting ones being: 

- trying new fonts for fun: maybe you can find some way to leverage the fun factor, by implementing a little game to teach typography fundamentals or something like this, it is usually a good idea to leverage the fun factor since it will put the user in a super good mood (UX);
- mood i.e. some pick a typeface that fits the mood of the publication;
- on the one hand, consistency i.e. all ebooks have the same typeface; 
- on the other hand, breaking the monotony i.e. changing the typeface for each book.

### Typefaces that should be included in a reading app

Users can be gathered in diverse categories.

#### The Minimalists

They don’t want an awful lot of fonts, they want the best option possible for serif and sans-serif, which means typefaces of outstanding quality (good legibility, perfect kerning, OpenType Features, etc.).

#### The Defaulters

Arial, Georgia, Verdana, Calibri, Times (New Roman), Segoe UI… 

In other words, the fonts they find and use in MS Word. 

On a related note, The defaults for Kindle (Bookerly) and Play Books (Literata) have become pretty popular. It is worth noting these custom fonts were commissioned, and it could be an alternative if you can afford it.

#### The eInkers

Basically, you’re given a list of typefaces you can find on a Kobo, Pocketbook or Kindle. This might primarily impact some companies doing both eInk Readers and apps but there could well be a possibility to make the user feel “at home” there, by having a few of those typefaces as well.

#### The Slabs

Cæcilia seems to be one of the must-haves, and it’s a slab serif, so you might want to at least provide one in this category or even a lookalike. 

#### The Librists

Those ones are more interested in libre/open source fonts you can find at Google Fonts, Font Squirrel, Github, SIL, etc. More on that later.

Answers include Merriweather, Bitter (slab), Vollkorn, Gentium, Ubuntu, Lato, etc.

#### The A11ys

Unsurprisingly, you’ll need a11y-related fonts: Open Dyslexic is a popular one, but other fonts cited in dyslexia guidelines are mentioned as well (Verdana, Trebuchet, Comic Sans, etc.).

#### The Printers

Some people would like to have typefaces like Garamond, Bembo, Baskerville, Times New Roman, etc. Some respondents explained that they wanted the ebook to be more like a print book.

It is important to note that their screen rendering can be utterly terrible (cf. [what Steve Matteson has to say about Times New Roman on screen](https://www.youtube.com/watch?v=TY-XmJv9u2M)). A lot has changed in designing typefaces for the screen since they were digitized and reworked versions may be available.

#### The Full Metal Typographers

To sum things up, they want all the fonts the platform has to offer, which means all system and locally-installed fonts.

With the advancement of Open Type Features, some issues may arise though: stylesets are unreliable, all fonts don’t manage features the same way e.g. `subs`, `sups`, `ordinal`, or `diagonal-fraction`, etc. This applies anyways, but could become especially complex if the user can access all the fonts available on the platform.

* * *

A few remarks there: 

- if you’re reading between the lines, you’ll see people list typefaces they’re used to – this is kind of obvious in Literata/Bookerly –, so you’d better focus on their category, traits and personality, which implies lookalikes/alternatives could do as well;
- if you want to meet the Printers’ demand, you should check the modern versions of those fonts first, as they have been fine-tuned for screen rendering and can offer a lot more legibility (cf. Linotype’s eText collection);
- you could bet on a flagship typeface like Amazon & Google but that’s a huge investment (custom typeface);
- you’d better be cautious about OTF as soon as possible since the user may find itself with some weird results: letters and numbers as `ordinal`, decorative glyphs replacing the latin alphabet as the typeface designer used stylesets for this, etc.

### Installing extra fonts

It actually is a thing. 

Once again, there may be a huge bias there (tech-savvy users), but we were surprised to find out that not only respondents would be willing to install extra fonts, quite a significant number is already doing it. Think of users installing fonts on their eReaders for instance. 

However, that opens the possibility to manage/install extra fonts. The major source of custom fonts seems to be Google Fonts for respondents but others go the extra mile and install licensed fonts locally, in which case they might want access to all the fonts installed on the system.

You’d better be cautious about Google Fonts as they are often a few versions behind, and the typeface designer could have added support for extra languages, OpenType Features, etc.

This obviously is a feature requiring some extra work, and some platforms may impose limitations, so it should just be considered an interesting piece of information. 

An important note: **some users had to install a font for a11y purposes (e.g. dyslexia) so it’s important you meet this requirement out of the box so that they don’t have to take that extra step.**