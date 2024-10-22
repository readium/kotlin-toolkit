import { Margins, Size } from "../common/types"
import { TapEvent } from "../common/events"

/** Manages a fixed layout resource embedded in an iframe. */
export class PageManager {
  private readonly iframe: HTMLIFrameElement

  private readonly listener: PageManager.Listener

  private margins: Margins = { top: 0, right: 0, bottom: 0, left: 0 }

  private readonly channel = new MessageChannel()

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
    this.iframe.addEventListener(
      "load",
      () => {
        this.onIframeLoaded()
      },
      { once: true }
    )
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

  private onIframeLoaded() {
    const viewport = this.iframe.contentWindow!.document.querySelector(
      "meta[name=viewport]"
    )
    if (!viewport || viewport! instanceof HTMLMetaElement) {
      //FIXME: handle edge case
      return
    }

    const pageSize = this.parsePageSize(viewport as HTMLMetaElement)
    if (!pageSize) {
      //FIXME: handle edge case
      return
    }
    this.iframe.style.width = pageSize.width + "px"
    this.iframe.style.height = pageSize.height + "px"
    this.size = pageSize

    this.channel.port1.onmessage = (message) => {
      this.onMessageFromIframe(message)
    }
    this.iframe.contentWindow!.postMessage("Init", "*", [this.channel.port2])

    this.listener.onIframeLoaded()
  }

  private onMessageFromIframe(message: MessageEvent) {
    if (message.data.x && message.data.y) {
      this.listener.onTap({ x: message.data.x, y: message.data.y })
    } else if (message.data.href) {
      this.listener.onLinkActivated(message.data.href)
    }
  }

  /** Parses the page size from the viewport meta tag of the loaded resource. */
  private parsePageSize(viewportMeta: HTMLMetaElement): Size | undefined {
    const regex = /(\w+) *= *([^\s,]+)/g
    const properties = new Map<any, any>()
    let match
    while ((match = regex.exec(viewportMeta.content))) {
      if (match != null) {
        properties.set(match[1], match[2])
      }
    }
    const width = parseFloat(properties.get("width"))
    const height = parseFloat(properties.get("height"))

    if (width && height) {
      return { width, height }
    } else {
      return undefined
    }
  }
}

export namespace PageManager {
  export interface Listener {
    onIframeLoaded(): void
    onTap(event: TapEvent): void
    onLinkActivated(href: string): void
  }
}
