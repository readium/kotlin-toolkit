//
//  Copyright 2022 Readium Foundation. All rights reserved.
//  Use of this source code is governed by the BSD-style license
//  available in the top-level LICENSE file of the project.
//

import { log as logNative, isScrollModeEnabled, pageWidth } from "./utils";
import { getCssSelector } from "css-selector-generator";

export function findFirstVisibleLocator() {
  const element = findFirstVisibleBlockElement();
  if (!element) {
    return undefined;
  }

  return {
    href: "#",
    type: "application/xhtml+xml",
    locations: {
      cssSelector: getCssSelector(element),
    },
    text: {
      highlight: element.textContent,
    },
  };
}

function findFirstVisibleBlockElement() {
  return findElement(
    document.body,
    (element) => window.getComputedStyle(element).display != "block"
  );
}

function findElement(rootElement, shouldIgnore) {
  var foundElement = undefined;
  for (var i = rootElement.children.length - 1; i >= 0; i--) {
    const child = rootElement.children[i];
    const element = findElement(child, shouldIgnore);
    if (element) {
      return element;
    }
  }

  if (isElementVisible(rootElement, undefined, shouldIgnore)) {
    return rootElement;
  }
}

// See computeVisibility_() in r2-navigator-js
function isElementVisible(element, domRect /* nullable */, shouldIgnore) {
  if (
    readium.isFixedLayout ||
    element === document.body ||
    element === document.documentElement
  ) {
    return true;
  }
  if (
    !document ||
    !document.documentElement ||
    !document.body ||
    (shouldIgnore && shouldIgnore(element))
  ) {
    return false;
  }

  const elStyle = getComputedStyle(element);
  if (elStyle) {
    const display = elStyle.getPropertyValue("display");
    if (display === "none") {
      return false;
    }
    // Cannot be relied upon, because web browser engine reports invisible when out of view in
    // scrolled columns!
    // const visibility = elStyle.getPropertyValue("visibility");
    // if (visibility === "hidden") {
    //     return false;
    // }
    const opacity = elStyle.getPropertyValue("opacity");
    if (opacity === "0") {
      return false;
    }
  }

  const rect = domRect || element.getBoundingClientRect();

  const scrollElement = document.scrollingElement;
  if (isScrollModeEnabled()) {
    return rect.top >= 0 && rect.top <= document.documentElement.clientHeight;
  } else {
    return rect.left < pageWidth;
  }
}
