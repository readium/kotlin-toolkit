//
//  Copyright 2023 Readium Foundation. All rights reserved.
//  Use of this source code is governed by the BSD-style license
//  available in the top-level LICENSE file of the project.
//

import { nearestInteractiveElement } from "./dom";

window.addEventListener("keydown", (event) => {
  if (shouldIgnoreEvent(event)) {
    return;
  }

  preventDefault(event);
  sendPressKeyMessage(event, "down");
});

window.addEventListener("keyup", (event) => {
  if (shouldIgnoreEvent(event)) {
    return;
  }

  preventDefault(event);
  sendPressKeyMessage(event, "up");
});

function shouldIgnoreEvent(event) {
  return (
    event.defaultPrevented ||
    nearestInteractiveElement(document.activeElement) != null
  );
}

// We prevent the default behavior for keyboard events, otherwise the web view
// might scroll.
function preventDefault(event) {
  event.stopPropagation();
  event.preventDefault();
}

function sendPressKeyMessage(event, type) {
  if (event.repeat) return;

  let keyEvent = {
    type: type,
    code: event.code,
    characters: String.fromCharCode(event.keyCode),
    alt: event.altKey,
    control: event.ctrlKey,
    shift: event.shiftKey,
    meta: event.metaKey,
  };

  Android.onKey(JSON.stringify(keyEvent));
}
