//
//  Copyright 2021 Readium Foundation. All rights reserved.
//  Use of this source code is governed by the BSD-style license
//  available in the top-level LICENSE file of the project.
//

import { TextQuoteAnchor } from "./vendor/hypothesis/anchoring/types";

// Catch JS errors to log them in the app.
window.addEventListener(
  "error",
  function (event) {
    Android.logError(event.message, event.filename, event.lineno);
  },
  false
);

window.addEventListener(
  "load",
  function () {
    const observer = new ResizeObserver(() => {
      requestAnimationFrame(() => {
        onViewportWidthChanged();
        snapCurrentOffset();
      });
    });
    observer.observe(document.body);
  },
  false
);

/**
 * Having an odd number of columns when displaying two columns per screen causes snapping and page
 * turning issues. To fix this, we insert a blank virtual column at the end of the resource.
 */
function appendVirtualColumnIfNeeded() {
  const id = "readium-virtual-page";
  var virtualCol = document.getElementById(id);
  if (isScrollModeEnabled() || getColumnCountPerScreen() != 2) {
    if (virtualCol) {
      virtualCol.remove();
    }
  } else {
    var documentWidth = document.scrollingElement.scrollWidth;
    var colCount = documentWidth / pageWidth;
    var hasOddColCount = (Math.round(colCount * 2) / 2) % 1 > 0.1;
    if (hasOddColCount) {
      if (virtualCol) {
        virtualCol.remove();
      } else {
        virtualCol = document.createElement("div");
        virtualCol.setAttribute("id", id);
        virtualCol.style.breakBefore = "column";
        virtualCol.innerHTML = "&#8203;"; // zero-width space
        document.body.appendChild(virtualCol);
      }
    }
  }
}

export var pageWidth = 1;

function onViewportWidthChanged() {
  // We can't rely on window.innerWidth for the pageWidth on Android, because if the
  // device pixel ratio is not an integer, we get rounding issues offsetting the pages.
  //
  // See https://github.com/readium/readium-css/issues/97
  // and https://github.com/readium/r2-navigator-kotlin/issues/146
  var width = Android.getViewportWidth();
  pageWidth = width / window.devicePixelRatio;
  setProperty(
    "--RS__viewportWidth",
    "calc(" + width + "px / " + window.devicePixelRatio + ")"
  );

  appendVirtualColumnIfNeeded();
}

export function getColumnCountPerScreen() {
  return parseInt(
    window
      .getComputedStyle(document.documentElement)
      .getPropertyValue("column-count")
  );
}

export function isScrollModeEnabled() {
  const style = document.documentElement.style;
  return (
    style.getPropertyValue("--USER__view").trim() == "readium-scroll-on" ||
    // FIXME: Will need to be removed in Readium 3.0, --USER__scroll was incorrect.
    style.getPropertyValue("--USER__scroll").trim() == "readium-scroll-on"
  );
}

export function isRTL() {
  return document.body.dir.toLowerCase() == "rtl";
}

export function isVerticalWritingMode() {
  const writingMode = window
    .getComputedStyle(document.documentElement)
    .getPropertyValue("writing-mode");
  return writingMode.startsWith("vertical");
}

// Scroll to the given TagId in document and snap.
export function scrollToId(id) {
  var element = document.getElementById(id);
  if (!element) {
    return false;
  }

  return scrollToRect(element.getBoundingClientRect());
}

// Position must be in the range [0 - 1], 0-100%.
export function scrollToPosition(position) {
  if (position < 0 || position > 1) {
    throw "scrollToPosition() must be given a position from 0.0 to 1.0";
  }

  let offset;
  if (isScrollModeEnabled()) {
    if (!isVerticalWritingMode()) {
      offset = document.scrollingElement.scrollHeight * position;
      document.scrollingElement.scrollTop = offset;
    } else {
      offset = document.scrollingElement.scrollWidth * position;
      document.scrollingElement.scrollLeft = -offset;
    }
    // window.scrollTo(0, offset);
  } else {
    var documentWidth = document.scrollingElement.scrollWidth;
    var factor = isRTL() ? -1 : 1;
    offset = documentWidth * position * factor;
    document.scrollingElement.scrollLeft = snapOffset(offset);
  }
}

// Scrolls to the first occurrence of the given text snippet.
//
// The expected text argument is a Locator object, as defined here:
// https://readium.org/architecture/models/locators/
export function scrollToLocator(locator) {
  let range = rangeFromLocator(locator);
  if (!range) {
    return false;
  }
  return scrollToRange(range);
}

function scrollToRange(range) {
  return scrollToRect(range.getBoundingClientRect());
}

function scrollToRect(rect) {
  if (isScrollModeEnabled()) {
    document.scrollingElement.scrollTop = rect.top + window.scrollY;
  } else {
    document.scrollingElement.scrollLeft = snapOffset(
      rect.left + window.scrollX
    );
  }

  return true;
}

export function scrollToStart() {
  if (isScrollModeEnabled() && !isVerticalWritingMode()) {
    document.scrollingElement.scrollTop = 0;
  } else {
    document.scrollingElement.scrollLeft = 0;
  }
}

export function scrollToEnd() {
  const scrollingElement = document.scrollingElement;

  if (isScrollModeEnabled()) {
    if (!isVerticalWritingMode()) {
      scrollingElement.scrollTop = document.body.scrollHeight;
    } else {
      scrollingElement.scrollLeft = -document.scrollingElement.scrollWidth;
    }
  } else {
    var factor = isRTL() ? -1 : 1;
    scrollingElement.scrollLeft = snapOffset(
      scrollingElement.scrollWidth * factor
    );
  }
}

// Returns false if the page is already at the left-most scroll offset.
export function scrollLeft(animated) {
  var documentWidth = document.scrollingElement.scrollWidth;
  var offset = currentScrollBase() - pageWidth;
  var minOffset = isRTL() ? -(documentWidth - pageWidth) : 0;
  return scrollToOffset(Math.max(offset, minOffset), animated);
}

// Returns false if the page is already at the right-most scroll offset.
export function scrollRight(animated) {
  var documentWidth = document.scrollingElement.scrollWidth;
  var offset = currentScrollBase() + pageWidth;
  var maxOffset = isRTL() ? 0 : documentWidth - pageWidth;
  return scrollToOffset(Math.min(offset, maxOffset), animated);
}

// Scrolls to the given left offset, optionally with animation.
// Returns false if the page scroll position is already close enough to the given offset.
function scrollToOffset(offset, animated) {
  //        Android.log("scrollToOffset " + offset);
  if (isScrollModeEnabled()) {
    throw "Called scrollToOffset() with scroll mode enabled. This can only be used in paginated mode.";
  }

  var currentOffset = currentScrollBase();
  var targetOffset = snapOffset(offset);
  var diff = Math.abs(currentOffset - offset) / pageWidth;
  var moved = diff > 0.01;

  if (animated && moved) {
    animateScrollTo(targetOffset, 300);
  } else {
    cancelScrollAnimation();
    document.scrollingElement.scrollLeft = targetOffset;
  }

  return moved;
}

var _animFrameId = null;
var _animTargetX = null;

// Returns the intended scroll destination if an animation is running, otherwise
// the live scroll position. Used so rapid page turns stack from the previous
// target rather than from a mid-animation position.
function currentScrollBase() {
  return _animTargetX !== null ? _animTargetX : window.scrollX;
}

function cancelScrollAnimation() {
  if (_animFrameId !== null) {
    cancelAnimationFrame(_animFrameId);
    _animFrameId = null;
  }
  _animTargetX = null;
}

// Animates a horizontal scroll to endX over the given duration (ms).
function animateScrollTo(endX, duration) {
  cancelScrollAnimation();
  var startX = document.scrollingElement.scrollLeft;
  _animTargetX = endX;
  var startTime = performance.now();

  function step(now) {
    var elapsed = now - startTime;
    var progress = Math.min(elapsed / duration, 1);
    // Ease-in-out quad
    var eased =
      progress < 0.5
        ? 2 * progress * progress
        : -1 + (4 - 2 * progress) * progress;
    document.scrollingElement.scrollLeft = Math.round(
      startX + (endX - startX) * eased
    );
    if (progress < 1) {
      _animFrameId = requestAnimationFrame(step);
    } else {
      _animFrameId = null;
      _animTargetX = null;
    }
  }

  _animFrameId = requestAnimationFrame(step);
}

// Snap the offset to the screen width (page width).
function snapOffset(offset) {
  var value = offset + (isRTL() ? -1 : 1);
  return value - (value % pageWidth);
}

// Snaps the current offset to the page width.
export function snapCurrentOffset() {
  //        Android.log("snapCurrentOffset");
  if (isScrollModeEnabled()) {
    return;
  }
  var currentOffset = window.scrollX;
  // Adds half a page to make sure we don't snap to the previous page.
  var factor = isRTL() ? -1 : 1;
  var delta = factor * (pageWidth / 2);
  document.scrollingElement.scrollLeft = snapOffset(currentOffset + delta);
}

export function rangeFromLocator(locator) {
  try {
    let locations = locator.locations;
    let text = locator.text;
    if (text && text.highlight) {
      var root;
      if (locations && locations.cssSelector) {
        root = document.querySelector(locations.cssSelector);
      }
      if (!root) {
        root = document.body;
      }

      let anchor = new TextQuoteAnchor(root, text.highlight, {
        prefix: text.before,
        suffix: text.after,
      });
      return anchor.toRange();
    }

    if (locations) {
      var element = null;

      if (!element && locations.cssSelector) {
        element = document.querySelector(locations.cssSelector);
      }

      if (!element && locations.fragments) {
        for (const htmlId of locations.fragments) {
          element = document.getElementById(htmlId);
          if (element) {
            break;
          }
        }
      }

      if (element) {
        let range = document.createRange();
        range.setStartBefore(element);
        range.setEndAfter(element);
        return range;
      }
    }
  } catch (e) {
    logError(e);
  }

  return null;
}

/// User Settings.

export function setCSSProperties(properties) {
  for (const name in properties) {
    setProperty(name, properties[name]);
  }
}

// For setting user setting.
export function setProperty(key, value) {
  if (value === null || value === "") {
    removeProperty(key);
  } else {
    var root = document.documentElement;
    // The `!important` annotation is added with `setProperty()` because if it's part of the
    // `value`, it will be ignored by the Web View.
    root.style.setProperty(key, value, "important");
  }
}

// For removing user setting.
export function removeProperty(key) {
  var root = document.documentElement;

  root.style.removeProperty(key);
}

/// Toolkit

export function log() {
  var message = Array.prototype.slice.call(arguments).join(" ");
  Android.log(message);
}

export function logError(message) {
  Android.logError(message, "", 0);
}
