import { DecorationActivatedEvent, DecorationManager } from "./decoration"

export interface GesturesListener {
  onTap(event: MouseEvent): void
  onLinkActivated(href: string, outerHtml: string): void
  onDecorationActivated(event: DecorationActivatedEvent): void
}

export class GesturesDetector {
  private readonly listener: GesturesListener

  private readonly decorationManager?: DecorationManager

  private readonly window: Window

  constructor(
    window: Window,
    listener: GesturesListener,
    decorationManager?: DecorationManager
  ) {
    this.window = window
    this.listener = listener
    this.decorationManager = decorationManager

    document.addEventListener(
      "click",
      (event) => {
        this.onClick(event)
      },
      false
    )
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

    let nearestElement: Element | null
    if (event.target instanceof HTMLElement) {
      nearestElement = this.nearestInteractiveElement(event.target)
    } else {
      nearestElement = null
    }

    if (nearestElement) {
      if (nearestElement instanceof HTMLAnchorElement) {
        this.listener.onLinkActivated(
          nearestElement.href,
          nearestElement.outerHTML
        )

        event.stopPropagation()
        event.preventDefault()
      } else {
        return
      }
    }

    let decorationActivatedEvent: DecorationActivatedEvent | null
    if (this.decorationManager) {
      decorationActivatedEvent =
        this.decorationManager.handleDecorationClickEvent(event)
    } else {
      decorationActivatedEvent = null
    }

    if (decorationActivatedEvent) {
      this.listener.onDecorationActivated(decorationActivatedEvent)
    } else {
      this.listener.onTap(event)
    }

    // event.stopPropagation()
    // event.preventDefault()
  }

  // See. https://github.com/JayPanoz/architecture/tree/touch-handling/misc/touch-handling
  private nearestInteractiveElement(element: Element): Element | null {
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
      return element
    }

    // Checks whether the element is editable by the user.
    if (
      element.hasAttribute("contenteditable") &&
      element.getAttribute("contenteditable")!.toLowerCase() != "false"
    ) {
      return element
    }

    // Checks parents recursively because the touch might be for example on an <em> inside a <a>.
    if (element.parentElement) {
      return this.nearestInteractiveElement(element.parentElement)
    }

    return null
  }
}
