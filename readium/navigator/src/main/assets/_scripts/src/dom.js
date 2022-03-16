//
//  Copyright 2022 Readium Foundation. All rights reserved.
//  Use of this source code is governed by the BSD-style license
//  available in the top-level LICENSE file of the project.
//

import { log as logNative, isScrollModeEnabled, pageWidth } from "./utils";

export function findFirstVisibleElement(rootElement) {
  for (var i = 0; i < rootElement.children.length; i++) {
    const child = rootElement.children[i];
    if (child.nodeType !== Node.ELEMENT_NODE) {
      continue;
    }
    const visibleElement = findFirstVisibleElement(child);
    if (visibleElement) {
      return visibleElement;
    }
  }

  if (
    rootElement !== document.body &&
    rootElement !== document.documentElement
  ) {
    const visible = isElementVisible(rootElement, undefined);
    if (visible) {
      return rootElement;
    }
  }
  return undefined;
}

// See computeVisibility_() in r2-navigator-js
function isElementVisible(element, domRect /* nullable */) {
  if (readium.isFixedLayout) {
    return true;
  } else if (!document || !document.documentElement || !document.body) {
    return false;
  }
  if (element === document.body || element === document.documentElement) {
    return true;
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
    // TODO: vertical writing mode
    return rect.top >= 0 && rect.top <= document.documentElement.clientHeight;
  }

  const scrollLeft = rect.left;
  let currentOffset = scrollElement.scrollLeft;
  return rect.left > 0 && rect.left < pageWidth;
}
