window.addEventListener("DOMContentLoaded", function () {
  bindDragGesture(document);
});

// See. https://github.com/JayPanoz/architecture/tree/touch-handling/misc/touch-handling
function nearestInteractiveElement(element) {
  if (element == null) {
    return null;
  }
  var interactiveTags = [
    "a",
    "audio",
    "button",
    "canvas",
    "details",
    "input",
    "label",
    "option",
    "select",
    "submit",
    "textarea",
    "video",
  ];
  if (interactiveTags.indexOf(element.nodeName.toLowerCase()) != -1) {
    return element.outerHTML;
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
    if (state) {
      state = undefined;
      isStartingDrag = false;
      return;
    }

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
        shouldPreventDefault = Gestures.onDragStart(JSON.stringify(state));
      }
    } else {
      shouldPreventDefault = Gestures.onDragMove(JSON.stringify(state));
    }

    if (shouldPreventDefault) {
      event.stopPropagation();
      event.preventDefault();
    }
  }

  function onEnd(event) {
    if (!state) return;

    const shouldPreventDefault = Gestures.onDragEnd(JSON.stringify(state));
    if (shouldPreventDefault) {
      event.stopPropagation();
      event.preventDefault();
    }
    state = undefined;
  }
}
