import { Margins, Size } from "../common/geometry"
import { DecorationActivatedEvent, TapEvent } from "./events"
import { IframeMessage, LinkActivatedMessage } from "./iframe-message"

/** Manages a fixed layout resource embedded in an iframe. */
export class PageManager {
  private readonly iframe: HTMLIFrameElement

  private readonly listener: PageManager.Listener

  private margins: Margins = { top: 0, right: 0, bottom: 0, left: 0 }

  size?: Size

  constructor(
    window: Window,
    iframe: HTMLIFrameElement,
    listener: PageManager.Listener
  ) {
    if (!iframe.contentWindow) {
      throw Error("Iframe argument must have been attached to DOM.")
    }

    this.listener = listener
    this.iframe = iframe
  }

  setMessagePort(messagePort: MessagePort) {
    messagePort.onmessage = (message) => {
      this.onMessageFromIframe(message)
    }
  }

  show() {
    this.iframe.style.display = "unset"
  }

  hide() {
    this.iframe.style.display = "none"
  }

  /** Sets page margins. */
  setMargins(margins: Margins) {
    if (this.margins == margins) {
      return
    }

    this.iframe.style.marginTop = this.margins.top + "px"
    this.iframe.style.marginLeft = this.margins.left + "px"
    this.iframe.style.marginBottom = this.margins.bottom + "px"
    this.iframe.style.marginRight = this.margins.right + "px"
  }

  /** Loads page content. */
  loadPage(url: string) {
    this.iframe.src = url
  }

  /** Sets the size of this page without content. */
  setPlaceholder(size: Size) {
    this.iframe.style.visibility = "hidden"
    this.iframe.style.width = size.width + "px"
    this.iframe.style.height = size.height + "px"
    this.size = size
  }

  private onMessageFromIframe(event: MessageEvent) {
    const message = event.data as IframeMessage
    switch (message.kind) {
      case "contentSize":
        return this.onContentSizeAvailable(message.size)
      case "tap":
        return this.listener.onTap(message.event)
      case "linkActivated":
        return this.onLinkActivated(message)
      case "decorationActivated":
        return this.listener.onDecorationActivated(message.event)
    }
  }

  private onLinkActivated(message: LinkActivatedMessage) {
    try {
      const url = new URL(message.href, this.iframe.src)
      this.listener.onLinkActivated(url.toString(), message.outerHtml)
    } catch {
      // Do nothing if url is not valid.
    }
  }

  private onContentSizeAvailable(size?: Size) {
    if (!size) {
      //FIXME: handle edge case
      return
    }
    this.iframe.style.width = size.width + "px"
    this.iframe.style.height = size.height + "px"
    this.size = size

    this.listener.onIframeLoaded()
  }
}

export namespace PageManager {
  export interface Listener {
    onIframeLoaded(): void
    onTap(event: TapEvent): void
    onLinkActivated(href: string, outerHtml: string): void
    onDecorationActivated(event: DecorationActivatedEvent): void
  }
}
