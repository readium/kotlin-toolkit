(function() {
  window.addEventListener('DOMContentLoaded', function(event) {
    document.addEventListener('click', onClick, false);
  });

  function onClick(event) {

    if (!window.getSelection().isCollapsed) {
      // There's an on-going selection, the tap will dismiss it so we don't forward it.
      return;
    }

    var pixelRatio = window.devicePixelRatio

    // Send the tap data over the JS bridge even if it's been handled within the web view, so that
    // it can be preserved and used by the toolkit if needed.
    var shouldPreventDefault = Android.onTap(JSON.stringify({
      "defaultPrevented": event.defaultPrevented,
      "screenX": event.screenX * pixelRatio,
      "screenY": event.screenY * pixelRatio,
      "clientX": event.clientX * pixelRatio,
      "clientY": event.clientY * pixelRatio,
      "targetElement": event.target.outerHTML,
      "interactiveElement": nearestInteractiveElement(event.target),
    }));

    if (shouldPreventDefault) {
      event.stopPropagation();
      event.preventDefault();
    }
  }

  // See. https://github.com/JayPanoz/architecture/tree/touch-handling/misc/touch-handling
  function nearestInteractiveElement(element) {
    var interactiveTags = [
      'a',
      'audio',
      'button',
      'canvas',
      'details',
      'input',
      'label',
      'option',
      'select',
      'submit',
      'textarea',
      'video',
    ]
    if (interactiveTags.indexOf(element.nodeName.toLowerCase()) != -1) {
      return element.outerHTML;
    }

    // Checks whether the element is editable by the user.
    if (element.hasAttribute('contenteditable') && element.getAttribute('contenteditable').toLowerCase() != 'false') {
      return element.outerHTML;
    }

    // Checks parents recursively because the touch might be for example on an <em> inside a <a>.
    if (element.parentElement) {
      return nearestInteractiveElement(element.parentElement);
    }

    return null;
  }

})();