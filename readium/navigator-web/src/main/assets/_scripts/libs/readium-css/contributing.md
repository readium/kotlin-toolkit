# Contributing to Readium CSS

First and foremost, thanks for your interest! 👍

The following is a set of guidelines for contributing to the Readium CSS project, hosted in the [Readium](https://github.com/readium) Organization on Github. These are just guidelines, not rules, so feel free to propose changes to this document in a pull request.

## Table of contents

1. [Design Principles](#design-principles)
2. [Testing](#testing)
3. [Coding Standards](#coding-standards)
4. [How Can I Contribute?](#how-can-i-contribute)
    + [Reporting Bugs](#reporting-bugs)
    + [Suggesting Enhancements](#suggesting-enhancements)
    + [Your First Code Contribution](#your-first-code-contribution)
    + [Pull Requests](#pull-requests)
5. [Roadmap](#roadmap)

## Design Principles

Readium CSS has been design following 4 principles:

1. **Modularity:** Readium CSS is not a monolithic stylesheet but a set of modules;
2. **Separation of Reading System’s Concerns:** those modules are task-oriented e.g. paginate, apply default styles, intercept styles for user settings, apply a reading mode or user setting, etc.;
3. **Daisy-chainability:** those modules can be loaded and daisy-chained (cascade), depending on conditions;
4. **Customization:** modules can be customized either before or during runtime (CSS variables).

Please keep those principles in mind before making a pull request. If you have any doubt, please feel free to contact [maintainers](mailto:readium-css@edrlab.org).

## Testing

Since Readium CSS and Readium 2 are still in their early days, there is no other way than DYI-testing right now.

You can either:

1. try implementing it in an existing Reading System;
2. use the [webpub manifest prototype](https://github.com/HadrienGardeur/webpub-manifest/tree/gh-pages/examples/paged-viewer).

In any case, you have to manually inject stylesheets and apply settings via the console, or design and code scripts if you want a GUI (user settings menu).

You can use the [R2-streamer-js](https://github.com/edrlab/r2-streamer-js) to manage EPUB files if needed.

## Coding Standards

The following guidelines are aimed at existing or newly-created stylesheets for Readium CSS.

### General Rules

- Indent by 2 spaces (or a tab which is equal to 2 spaces).
- Use multiline: one property and value per line.
- Add a space after a property’s colon (e.g. `display: block`).
- End all lines with a semi-colon.
- For multiple, comma-separated selectors, try grouping them in a logical fashion (e.g. putting headings together).
- Attribute selectors, like `input[type="text"]` should always wrap the attribute's value in double quotes.
- Don’t use classes, style elements.
- Only use `!important` when a style must be applied at any cost necessary.
- Try using prefixed-properties which are necessary, including `-epub-properties` (check the [EPUB 3.2 spec](https://w3c.github.io/publ-epub-revision/epub32/spec/epub-contentdocs.html#sec-css-prefixed) for a complete list).

### Stylesheet Header

Every stylesheet must contain a header using the following template: 

```
/* Readium CSS 
   Name of the stylesheet

   Description i.e. which task the stylesheet is trying to accomplish

   Repo: https://github.com/readium/readium-css */
```

### Custom Properties (variables)

We’re using custom properties, also known as CSS variables, to make Readium CSS customizable. See the `CSS-api.md` file in docs for a complete list.

We’re currently using the two following prefix:

- `--RS__` for Reading System variables;
- `--USER__` for user settings.

The variable itself must use camelCase (e.g. `--RS__textColor`, and `--USER__fontSize`).

Of course all values don’t have to be variables, variables should be used if: 

- it allows implementers to customize Reading System styles or themes;
- it is likely to be updated on runtime (user setting).

### Cascade and Inheritance

Readium CSS is leveraging the cascade and inheritance, which implies authors’ stylesheets are part of the cascade.

The order of priority we must emulate is the following: 

1. transition declarations;
2. important user agent declarations;
3. important user declarations;
4. important author declarations;
5. animation declarations;
6. normal author declarations;
7. normal user declarations;
8. normal user agent declarations.

In other words, we’re following the priority of constituencies whenever possible: 

```
User > Author > User Agent
```

This applies to the custom properties as well:

```
--USER__var > --RS__var
```

If possible, user settings must be inherited from the `--USER__variable` set on `:root`. This obviously can’t be done for all user settings as it depends on the scope (the elements targeted).

User settings must also abide by [these recommendations](https://readium.org/readium-css/docs/CSS14-user_settings_recs.html).

## How Can I Contribute?

### Reporting Bugs

First and foremost, check if your issue has already been reported (don’t forget to check closed issues too). If it has, add a comment to the existing issue instead of opening a new one.

Before opening any issue… 

- **Ensure your EPUB is valid.** Always validate your EPUB against [EPUBCheck](https://github.com/w3c/epubcheck). If there are errors or warnings, please fix those before submitting an issue.
- **Create an isolated and reproducible test case.** Be sure the problem exists in Readium CSS (try your HTML in the browser). Try providing a [reduced test case](https://css-tricks.com/reduced-test-cases/).
- **Share as much information as possible.** See below.

#### Submitting a Good Bug Report

Explain the problem and include additional details to help maintainers reproduce the problem:

- Use a clear and descriptive title for the issue to identify the problem.
- Tell which platforms or browsers are impacted.
- Describe the exact steps to reproduce the problem in as many details as possible.
- Provide specific examples to demonstrate the steps. Include links to files or GitHub projects, or copy/pasteable snippets, which you use in those examples. If you're providing snippets in the issue, use Markdown code blocks.
- Describe the behavior you observed after following the steps and point out what exactly is the problem with that behavior.
- Explain which behavior you expected to see instead and why.
- Include screenshots and/or animated GIFs which show you following the described steps and clearly demonstrate the problem.
- If you’ve fixed the issue by yourself, tell us how you did it (snippet in a Markdown code block).

#### Template for Submitting Bug Reports

```
I'm submitting a…

- [x] bug report
- [ ] feature request
- [ ] other

**Short description of the issue/suggestion:**

**Steps to reproduce the issue/enhancement:**

1. [First Step]
2. [Second Step]
3. [Other Steps...]

**What is the expected behaviour?**

**What is the current behaviour?**

**Do you have screenshots, GIFs, demos or samples which demonstrate the problem or enhancement?** 

![image description](url)

**What is the motivation / use case for changing the behaviour?**

**Do you know which CSS modules (stylesheets) are impacted?**

**Please tell us about your environment:**

- Platform: [Android | iOS | Linux | MacOS | Windows | other]
- Browser / Rendering Engine: [all | Chrome XX | Firefox XX | IE XX | Safari XX | Mobile Chrome XX | Chromium on Electron XX | Android Web View | iOS XX Safari | iOS XX UIWebView | iOS XX WKWebView | other]

**Other information** (e.g. related issues, suggestions how to fix, links for us to have context)
```

### Suggesting Enhancements

First and foremost, check if those enhancements have already been suggested (check issues) or addressed (check pull requests).

#### Submitting a Good Enhancement Suggestion

- Use a clear and descriptive title for the issue to identify the suggestion.
- Provide a step-by-step description of the suggested enhancement in as many details as possible.
- Provide specific examples to demonstrate the steps. Include copy/pasteable snippets which you use in those examples, as Markdown code blocks.
- Include screenshots and/or animated GIFs which help you demonstrate the steps or point out the part of Readium CSS which the suggestion is related to.
- Explain why this enhancement would be useful to most users, the problems it solves as regards design, UX, etc.

#### Template for Suggesting an Enhancement

```
I'm submitting a…

- [ ] bug report
- [x] feature request
- [ ] other

**Short description of the issue/suggestion:**

**Steps to reproduce the issue/enhancement:**

1. [First Step]
2. [Second Step]
3. [Other Steps...]

**What is the expected behaviour?**

**What is the current behaviour?**

**Do you have screenshots, GIFs, demos or samples which demonstrate the problem or enhancement?** 

![image description](url)

**What is the motivation / use case for changing the behaviour?**

**Do you know which CSS modules (stylesheets) are impacted?**

**Please tell us about your environment:**

- Platform: [Android | iOS | Linux | MacOS | Windows | other]
- Browser / Rendering Engine: [all | Chrome XX | Firefox XX | IE XX | Safari XX | Mobile Chrome XX | Chromium on Electron XX | Android Web View | iOS XX Safari | iOS XX UIWebView | iOS XX WKWebView | other]

**Other information** (e.g. related issues, suggestions how to fix, links for us to have context)
```

### Your First Code Contribution

Unsure where to begin contributing to the Readium CSS Project? You can start by looking through the `feedback-required` and/or `low-hanging fruit` issues.

Then fork the repo, create a branch named after the issue you’re trying to solve, and implement your modifications.

If needed, do not hesitate to ask questions. We’re here to help.

We’ll finally review your pull request to check if everything is OK.

### Pull Requests

- Test your code (see [platform support](https://github.com/readium/readium-css/wiki/Platform-Support) in the Wiki).
- Pull requests should **always** be created against the develop branch – never the master.
- Please update from the develop branch before proposing a pull request so that we don’t have to deal with conflicts.
- Use a clear and descriptive title.
- List all fixes and enhancements the pull request provides.
- Include screenshots and/or animated GIF whenever possible – no need to do that for correcting typos in docs for instance.
- Document new code.