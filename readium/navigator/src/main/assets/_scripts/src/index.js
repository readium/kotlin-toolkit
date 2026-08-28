//
//  Copyright 2021 Readium Foundation. All rights reserved.
//  Use of this source code is governed by the BSD-style license
//  available in the top-level LICENSE file of the project.
//

// Base script used by both reflowable and fixed layout resources.

import "./gestures";
import "./keyboard";
import {
  removeProperty,
  scrollLeft,
  scrollRight,
  scrollToEnd,
  scrollToId,
  scrollToPosition,
  scrollToStart,
  scrollToLocator,
  setProperty,
  setCSSProperties,
} from "./utils";
import { findFirstVisibleLocator } from "./dom";
import { getCurrentSelection } from "./selection";
import { getDecorations, registerTemplates } from "./decorator";

// Public API used by the navigator.
window.readium = {
  // utils
  scrollToId: scrollToId,
  scrollToPosition: scrollToPosition,
  scrollToLocator: scrollToLocator,
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
