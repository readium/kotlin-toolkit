# CSS Performance Hacks

[Implementers’ doc]

This document discusses the CSS specs implementers can use in case they have performance issues: 

1. `will-change`;
2. CSS containment.

At first sight, using those 2 specs can give tremendous results but you sometimes have to use them in clever ways: it’s not just about adding a line of CSS in your stylesheet.

## Will-change

### The fundamental rule of will-change

> Set will-change to the properties you’ll actually change, on the elements that are actually changing. And remove it when they stop.

### Abstract

The `will-change` property allows an author to inform the UA ahead of time of what kinds of changes they are likely to make to an element. The UA can then try to optimize how they handle the element ahead of time, performing potentially-expensive work preparing for an animation before the animation actually begins.

**In other words, `will-change` is a UA hint,** it doesn’t have any styling effect on the elements for which you’re using it. It’s worth noting it can have appearance effects though, if a new stacking context is created.

It is, in many ways, an official property for the infamous “translate-z hack.” Although it was meant for animation, it can also deal with more simple changes.

### Usage

```
will-change: <animateable-feature> = scroll-position || contents || <custom-ident>
```

#### Scroll-position

Indicates that the author expects to animate or change the scroll position of the element in the near future.

A browser might take this value as a signal to expand the range of content around the scroll window that is rendered, so that longer/faster scrolls can be done smoothly.

#### Contents

Indicates that the author expects to animate or change something about the element’s contents in the near future.

A browser might take this value as a signal to cache less aggressively on the element, or avoid caching at all and just continually re-render the element from scratch.

#### custom-ident

Indicates that the author expects to animate or change the property with the given name on the element in the near future.

A browser might take a value of transform as a signal that it should go ahead and promote the element to its own layer immediately, before the element starts to be transformed, to avoid any delay involved in rerendering the old and new layers.

### Drawbacks

By using `will-change`, you’re basically telling the browser the element is a few moments away from changing, the browser consequently dedicates memory and resources to this element. **If you use it for too many elements, it will thus degrade performance.**

This property should be considered a last resort, it was not meant for premature optimization. **You should use it only if you have to deal with performance issues.**

### Do’s and Dont’s

#### Do’s

- Use `will-change` sparingly in stylesheets
- Give `will-change` sufficient time to work
- Use `<custom-ident>` to target super specific changes (`left`, `opacity`, etc.)
- Use it in JavaScript if needed (add and remove hint)
- Remove `will-change` after the changes are done

#### Dont’s

- Don’t declare changes to too many properties
- Don’t apply it to too many elements
- Don’t waste resources on elements that have stopped changing

### Examples

#### UI elements

Specifying `will-change` for a small number of persistent UI elements in a page which should react snappily to the user is appropriate:

```
body > .sidebar {
  will-change: transform;
  /* Will use 'transform' to slide it out when the user requests. */
}
```

Because this is limited to a small number of elements, the fact that the optimization is rarely actually used doesn’t hurt very much.

#### User click

If an element is going to change when a user clicks on it, setting `will-change` on hover will usually give at least 200 milliseconds for the optimizations to be set up, as human reaction time is relatively slow.

```
.element { 
  transition: opacity 0.2s; 
  opacity: 1; 
}

.element:hover { 
  will-change: opacity; 
}

.element:active { 
  opacity: 0.3; 
}
```

#### Adding and removing the hint with JavaScript

This is probably the way to go, should you try using `will-change` to optimize performance.

Here we have the hint added on `:hover`, then removed when the animation has ended.

```
var el = document.getElementById('element');

// Set will-change when the element is hovered
el.addEventListener('mouseenter', hintBrowser);
el.addEventListener('animationEnd', removeHint);

function hintBrowser() {
  // The optimizable properties that are going to change
  // in the animation's keyframes block
  this.style.willChange = 'transform, opacity';
}

function removeHint() {
  this.style.willChange = 'auto';
}
```

### References

- [CSS Will Change Module Level 1](https://www.w3.org/TR/css-will-change-1/)
- [Can I Use](http://caniuse.com/#feat=will-change)
- [Mozilla Dev Network](https://developer.mozilla.org/en-US/docs/Web/CSS/will-change)
- [Everything you need to know about the CSS will-change property](https://dev.opera.com/articles/css-will-change-property/)
- [CSS ‘will-change’ Property: A Performance Case Study](https://www.maxlaumeister.com/blog/css-will-change-property-a-performance-case-study/)

## CSS Containment 

**Note: This is only implemented in Blink (Chrome + Opera). It is under consideration for Microsoft Edge.**

### Abstract

The `contain` property indicates that **the element and its contents are, as much as possible, independent of the rest of the page.**

This allows the browser to recalculate layout, style, paint, size, or any combination of them for a limited area of the DOM and not the entire page. To put it simply, it lets developers limit the scope of the browser's styles, layout and paint work.

It was primarily meant for webpages which contain a lot of widgets that are all independent as it can be used to prevent one widget's CSS rules from changing other things on the page.

### Usage

```
contain: strict || content || size || layout || style || paint
```

#### Strict

This is a shorthand for all values, it is equivalent to `contain: size layout style paint`.

#### Content

This is a shorthand for all values but `size`, it is equivalent to `contain: layout style paint`.

#### Size

It indicates that the element can be laid out without the need to examine its descendants. 

#### Layout

It indicates that nothing outside can affect the element’s internal layout, and vice versa. 

#### Style

It indicates that, for properties which can have effects on more than just an element and its descendants, those effects don’t escape the containing element.

#### Paint

It indicates that the descendants of the element don’t display outside its bounds, so if an element is off-screen or otherwise not visible, its descendants are also guaranteed to be not visible.

### Possible optimizations

In theory, huge performance gains are possible, but it requires a solid knowledge of how browsers work and what causes reflow, relayout, repaint, etc. Judging by examples, I can see how it could be useful but we’ll probably have to find inventive and clever ways to leverage it since our scope is very often the root document.

If you need a refresher: 

- [How browsers work](https://www.html5rocks.com/en/tutorials/internals/howbrowserswork/)
- [What force layout/reflow](https://gist.github.com/paulirish/5d52fb081b3570c81e3a)

#### Layout

- When laying out the page, the contents of separate containing elements can be laid out in parallel, as they’re guaranteed not to affect each other.
- When laying out the page, if the containing element is off-screen or obscured and the layout of the visible parts of the screen do not depend on the size of the containing element (for example, if the containing element is near the end of a block container, and you’re viewing the beginning of the block container), the layout of the containing elements' contents can be delayed or done at a lower priority.
- Layout is normally document-scoped so if you change an element's `left` property, every single element in the DOM might need to be checked. Enabling containment here can potentially reduce the number of elements to just a handful, rather than the whole document, saving the browser a ton of unnecessary work and significantly improving performance.

#### Style

- Whenever a property is changed on a descendant of the containing element, calculating what part of the DOM tree is “dirtied” and might need to have its style recalculated can stop at the containing element.
- Containment here is purely about limiting the parts of the tree that are under consideration when styles are mutated, not when they are declared.

#### Paint

- If the containing element is off-screen or obscured, the UA can directly skip trying to paint its contents, as they’re guaranteed to be off-screen/obscured as well.
- Unless the clipped content is made accessible via a separate mechanism such as the overflow, resize, or text-overflow properties, the UA can reserve “canvas” space for the element exactly the element’s size.
- Because they are guaranteed to be stacking contexts, scrolling elements can be painted into a single GPU layer.
- It acts as a containing block for absolutely positioned and fixed position elements: any children are positioned based on the element, not the document.
- It becomes a stacking context: things like `z-index` will have an effect on the element, and children will be stacked according to the new context.
- It becomes a new formatting context: a block level element will be treated as a new, independent layout environment. Layout outside of the element won’t typically affect the containing element's children.

#### Size

- It ensures you don’t rely on child elements for sizing, but by itself it doesn’t offer much performance benefit.

### References

- [CSS Containment Module Level 1](https://www.w3.org/TR/css-contain-1/)
- [Can I Use](http://caniuse.com/#feat=css-containment)
- [Mozilla Dev Network](https://developer.mozilla.org/en-US/docs/Web/CSS/contain)
- [CSS Containment in Chrome 52](https://developers.google.com/web/updates/2016/06/css-containment)