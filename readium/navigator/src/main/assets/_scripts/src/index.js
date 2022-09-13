//
//  Copyright 2021 Readium Foundation. All rights reserved.
//  Use of this source code is governed by the BSD-style license
//  available in the top-level LICENSE file of the project.
//

// Base script used by both reflowable and fixed layout resources.

import "./gestures";
import {
  removeProperty,
  scrollLeft,
  scrollRight,
  scrollToEnd,
  scrollToId,
  scrollToPosition,
  scrollToStart,
  scrollToText,
  setProperty,
  setCSSProperties,
} from "./utils";
import {
  createAnnotation,
  createHighlight,
  destroyHighlight,
  getCurrentSelectionInfo,
  getSelectionRect,
  rectangleForHighlightWithID,
  setScrollMode,
} from "./highlight";
import { findFirstVisibleLocator } from "./dom";
import { getCurrentSelection } from "./selection";
import { getDecorations, registerTemplates } from "./decorator";

// Public API used by the navigator.
window.readium = {
  // utils
  scrollToId: scrollToId,
  scrollToPosition: scrollToPosition,
  scrollToText: scrollToText,
  scrollLeft: scrollLeft,
  scrollRight: scrollRight,
  scrollToStart: scrollToStart,
  scrollToEnd: scrollToEnd,
  setCSSProperties: setCSSProperties,
  setProperty: setProperty,
  removeProperty: removeProperty,

  // selection
  getCurrentSelection: getCurrentSelection,

  // decoration
  registerDecorationTemplates: registerTemplates,
  getDecorations: getDecorations,

  // DOM
  findFirstVisibleLocator: findFirstVisibleLocator,
};

// Legacy highlights API.
window.createAnnotation = createAnnotation;
window.createHighlight = createHighlight;
window.destroyHighlight = destroyHighlight;
window.getCurrentSelectionInfo = getCurrentSelectionInfo;
window.getSelectionRect = getSelectionRect;
window.rectangleForHighlightWithID = rectangleForHighlightWithID;
window.setScrollMode = setScrollMode;
