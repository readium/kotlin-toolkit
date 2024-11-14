import { Insets, Size } from "../common/types"
import { computeScale, Fit } from "../util/fit"
import { PageManager } from "./page-manager"
import { ViewportStringBuilder } from "../util/viewport"
import { AreaManager } from "./area-manager"
import { GesturesDetector } from "../common/gestures"
import { TapEvent } from "../common/events"

export class SingleAreaManager {
  private readonly metaViewport: HTMLMetaElement

  private readonly page: PageManager

  private fit: Fit = Fit.Contain

  private insets: Insets = { top: 0, right: 0, bottom: 0, left: 0 }

  private viewport?: Size

  private scale: number = 1

  constructor(
    window: Window,
    iframe: HTMLIFrameElement,
    metaViewport: HTMLMetaElement,
    listener: AreaManager.Listener
  ) {
    window.addEventListener("message", (event) => {
      if (event.source === iframe.contentWindow && event.ports[0]) {
        this.page.setMessagePort(event.ports[0])
      }
    })

    const wrapperGesturesListener = {
      onTap: (event: MouseEvent) => {
        const tapEvent = {
          x:
            (event.clientX - visualViewport!.offsetLeft) *
            visualViewport!.scale,
          y:
            (event.clientY - visualViewport!.offsetTop) * visualViewport!.scale,
        }
        listener.onTap(tapEvent)
      },
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      onLinkActivated: (_: string) => {
        throw Error("No interactive element in the root document.")
      },
    }
    new GesturesDetector(window, wrapperGesturesListener)

    this.metaViewport = metaViewport
    const pageListener = {
      onIframeLoaded: () => {
        this.onIframeLoaded()
      },
      onTap: (event: TapEvent) => {
        const boundingRect = iframe.getBoundingClientRect()
        const tapEvent = {
          x:
            (event.x + boundingRect.left - visualViewport!.offsetLeft) *
            visualViewport!.scale,
          y:
            (event.y + boundingRect.top - visualViewport!.offsetTop) *
            visualViewport!.scale,
        }
        listener.onTap(tapEvent)
      },
      onLinkActivated: (href: string, outerHtml: string) => {
        listener.onLinkActivated(href, outerHtml)
      },
    }
    this.page = new PageManager(window, iframe, pageListener)
  }

  setViewport(viewport: Size, insets: Insets) {
    if (this.viewport == viewport && this.insets == insets) {
      return
    }

    this.viewport = viewport
    this.insets = insets
    this.layout()
  }

  setFit(fit: Fit) {
    if (this.fit == fit) {
      return
    }

    this.fit = fit
    this.layout()
  }

  loadResource(url: string) {
    this.page.hide()
    this.page.loadPage(url)
  }

  private onIframeLoaded() {
    if (!this.page.size) {
      // FIXME: raise error
    } else {
      this.layout()
    }
  }

  private layout() {
    if (!this.page.size || !this.viewport) {
      return
    }

    const margins = {
      top: this.insets.top,
      right: this.insets.right,
      bottom: this.insets.bottom,
      left: this.insets.left,
    }
    this.page.setMargins(margins)

    const safeDrawingSize = {
      width: this.viewport.width - this.insets.left - this.insets.right,
      height: this.viewport.height - this.insets.top - this.insets.bottom,
    }

    const scale = computeScale(this.fit, this.page.size, safeDrawingSize)

    this.metaViewport.content = new ViewportStringBuilder()
      .setInitialScale(scale)
      .setMinimumScale(scale)
      .setWidth(this.page.size.width)
      .setHeight(this.page.size.height)
      .build()

    this.scale = scale

    this.page.show()
  }
}
