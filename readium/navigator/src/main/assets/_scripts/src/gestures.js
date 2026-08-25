/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

import { handleDecorationClickEvent } from "./decorator";
import { nearestInteractiveElement } from "./dom";
import { toNativeRect } from "./rect";
import { logError } from "./utils";
import { getCssSelector } from "css-selector-generator";

window.addEventListener("DOMContentLoaded", function () {
  document.addEventListener("click", onClick, false);
  bindDragGesture(document);
});

function onClick(event) {
  if (!window.getSelection().isCollapsed) {
    // There's an on-going selection, the tap will dismiss it so we don't forward it.
    return;
  }

  var pixelRatio = window.devicePixelRatio;
  let clickEvent = {
    defaultPrevented: event.defaultPrevented,
    x: event.clientX * pixelRatio,
    y: event.clientY * pixelRatio,
    targetElement: extractTargetElement(event.target),
    interactiveElement: nearestInteractiveElement(event.target),
  };

  if (handleDecorationClickEvent(event, clickEvent)) {
    return;
  }

  // Send the tap data over the JS bridge even if it's been handled within the web view, so that
  // it can be preserved and used by the toolkit if needed.
  var shouldPreventDefault = Android.onTap(JSON.stringify(clickEvent));

  if (shouldPreventDefault) {
    event.stopPropagation();
    event.preventDefault();
  }
}

function bindDragGesture(element) {
  // passive: false is necessary to be able to prevent the default behavior.
  element.addEventListener("touchstart", onStart, { passive: false });
  element.addEventListener("touchend", onEnd, { passive: false });
  element.addEventListener("touchmove", onMove, { passive: false });

  var state = undefined;
  var isStartingDrag = false;
  const pixelRatio = window.devicePixelRatio;

  function onStart(event) {
    isStartingDrag = true;

    const startX = event.touches[0].clientX * pixelRatio;
    const startY = event.touches[0].clientY * pixelRatio;
    state = {
      defaultPrevented: event.defaultPrevented,
      startX: startX,
      startY: startY,
      currentX: startX,
      currentY: startY,
      offsetX: 0,
      offsetY: 0,
      interactiveElement: nearestInteractiveElement(event.target),
    };
  }

  function onMove(event) {
    if (!state) return;

    state.currentX = event.touches[0].clientX * pixelRatio;
    state.currentY = event.touches[0].clientY * pixelRatio;
    state.offsetX = state.currentX - state.startX;
    state.offsetY = state.currentY - state.startY;

    var shouldPreventDefault = false;
    // Wait for a movement of at least 6 pixels before reporting a drag.
    if (isStartingDrag) {
      if (Math.abs(state.offsetX) >= 6 || Math.abs(state.offsetY) >= 6) {
        isStartingDrag = false;
        shouldPreventDefault = Android.onDragStart(JSON.stringify(state));
      }
    } else {
      shouldPreventDefault = Android.onDragMove(JSON.stringify(state));
    }

    if (shouldPreventDefault) {
      event.stopPropagation();
      event.preventDefault();
    }
  }

  function onEnd(event) {
    if (!state) return;

    const shouldPreventDefault = Android.onDragEnd(JSON.stringify(state));
    if (shouldPreventDefault) {
      event.stopPropagation();
      event.preventDefault();
    }
    state = undefined;
  }
}

/**
 * Extracts metadata about the target element for gesture handling.
 *
 * Returns an object with the element's bounding rectangle, tag name, source
 * URL, a CSS selector, an accessibility label, and a caption. This information
 * is used on the native side to build the appropriate `Content.Element`.
 *
 * Returns `null` when the tapped element is not (or is not contained within) an
 * image element, so ordinary taps only pay for a cheap parent walk.
 */
function extractTargetElement(element) {
  if (!element || !element.getBoundingClientRect) {
    return null;
  }

  let imageElement = findNearestImageElement(element);
  if (!imageElement) {
    return null;
  }

  try {
    return buildTargetElement(imageElement);
  } catch (error) {
    logError(`Failed to extract the tapped target element: ${error}`);
    return null;
  }
}

function buildTargetElement(imageElement) {
  let rawSrc =
    imageElement.getAttribute("src") ||
    imageElement.getAttribute("href") ||
    null;

  // Resolve the raw src/href attribute to an absolute URL using the document's
  // base URI. `getAttribute` returns the literal attribute value (possibly
  // relative), while we need the absolute form so the native side can
  // relativize it against the publication base URL to recover the correct
  // manifest href.
  let src = rawSrc ? new URL(rawSrc, document.baseURI).href : null;

  // `html` is only needed for inline SVGs that have no resolvable `src`.
  let html = src ? null : imageElement.outerHTML;

  return {
    tag: imageElement.tagName.toLowerCase(),
    html: html,
    src: src,
    rect: toNativeRect(imageElement.getBoundingClientRect()),
    accessibilityLabel: imageElement.getAttribute("aria-label")?.trim() || null,
    caption: extractCaption(imageElement),
    cssSelector: getCssSelector(imageElement),
  };
}

/**
 * Returns a human-readable caption for an image element by checking, in
 * order: the `alt` attribute, the `title` attribute, the text content of the
 * first SVG `<title>` child, the text content of the first SVG `<desc>`
 * child, and the text content of a `<figcaption>` inside a parent `<figure>`.
 * Returns `null` when none of these are present.
 *
 * When `alt` is present — even as an empty string (decorative image) — no
 * other source is consulted, so that an explicit `alt=""` suppresses fallback
 * captions rather than incorrectly propagating them.
 */
function extractCaption(imageElement) {
  if (imageElement.hasAttribute("alt")) {
    const alt = imageElement.getAttribute("alt").trim();
    return alt || null;
  }

  const title = imageElement.getAttribute("title")?.trim();
  if (title) return title;

  const svgTitle = imageElement
    .querySelector(":scope > title")
    ?.textContent.trim();
  if (svgTitle) return svgTitle;

  const svgDesc = imageElement
    .querySelector(":scope > desc")
    ?.textContent.trim();
  if (svgDesc) return svgDesc;

  const figure = imageElement.closest("figure");
  if (figure) {
    const figcaption = figure.querySelector("figcaption")?.textContent.trim();
    if (figcaption) return figcaption;
  }

  return null;
}

/**
 * Walks up the DOM tree from the given element to find the nearest image
 * element (img, svg).
 */
function findNearestImageElement(element) {
  const imageTags = ["img", "svg"];
  let current = element;
  while (current && current !== document.documentElement) {
    if (imageTags.includes(current.tagName.toLowerCase())) {
      return current;
    }
    current = current.parentElement;
  }
  return null;
}
