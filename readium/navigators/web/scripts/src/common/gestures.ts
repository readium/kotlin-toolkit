export interface GesturesListener {
  onTap(event: MouseEvent): void
}

export class GesturesManager {
  private readonly listener: GesturesListener

  private readonly window: Window

  constructor(window: Window, listener: GesturesListener) {
    this.window = window
    this.listener = listener
    window.addEventListener("DOMContentLoaded", () => {
      document.addEventListener(
        "click",
        (event) => {
          this.onClick(event)
        },
        false
      )
    })
  }

  private onClick(event: MouseEvent) {
    if (event.defaultPrevented) {
      return
    }

    const selection = this.window.getSelection()
    if (selection && selection.type == "Range") {
      // There's an on-going selection, the tap will dismiss it so we don't forward it.
      // selection.type might be None (collapsed) or Caret with a collapsed range
      // when there is not true selection.
      return
    }

    if (
      event.target instanceof Element &&
      this.nearestInteractiveElement(event.target)
    ) {
      return
    }

    this.listener.onTap(event)

    event.stopPropagation()
    event.preventDefault()
  }

  // See. https://github.com/JayPanoz/architecture/tree/touch-handling/misc/touch-handling
  private nearestInteractiveElement(element: Element): string | null {
    if (element == null) {
      return null
    }
    const interactiveTags = [
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
    ]
    if (interactiveTags.indexOf(element.nodeName.toLowerCase()) != -1) {
      return element.outerHTML
    }

    // Checks whether the element is editable by the user.
    if (
      element.hasAttribute("contenteditable") &&
      element.getAttribute("contenteditable")!.toLowerCase() != "false"
    ) {
      return element.outerHTML
    }

    // Checks parents recursively because the touch might be for example on an <em> inside a <a>.
    if (element.parentElement) {
      return this.nearestInteractiveElement(element.parentElement)
    }

    return null
  }
}
