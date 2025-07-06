import { Insets, Size } from "../common/geometry"
import { computeScale, Fit } from "./fit"
import { PageManager } from "./page-manager"
import { ViewportStringBuilder } from "../util/viewport"
import { AreaManager } from "./area-manager"
import { offsetToParentCoordinates } from "../common/geometry"
import { rectToParentCoordinates } from "../common/geometry"
import { GesturesDetector } from "../common/gestures"
import { TapEvent, DecorationActivatedEvent } from "./events"
import { DecorationActivatedEvent as OriginalDecorationActivated } from "../common/decoration"

export class SingleAreaManager {
  private readonly metaViewport: HTMLMetaElement

  private readonly page: PageManager

  private readonly listener: AreaManager.Listener

  private fit: Fit = "contain"

  private insets: Insets = { top: 0, right: 0, bottom: 0, left: 0 }

  private viewport?: Size

  private scale: number = 1

  constructor(
    window: Window,
    iframe: HTMLIFrameElement,
    metaViewport: HTMLMetaElement,
    listener: AreaManager.Listener
  ) {
    this.listener = listener

    const wrapperGesturesListener = {
      onTap: (event: MouseEvent) => {
        const offset = {
          x:
            (event.clientX - visualViewport!.offsetLeft) *
            visualViewport!.scale,
          y:
            (event.clientY - visualViewport!.offsetTop) * visualViewport!.scale,
        }
        listener.onTap({ offset: offset })
      },
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      onLinkActivated: (_: string) => {
        throw Error("No interactive element in the root document.")
      },
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      onDecorationActivated: (_: OriginalDecorationActivated) => {
        throw Error("No decoration in the root document.")
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
        const shiftedOffset = offsetToParentCoordinates(
          event.offset,
          boundingRect
        )
        listener.onTap({ offset: shiftedOffset })
      },
      onLinkActivated: (href: string, outerHtml: string) => {
        listener.onLinkActivated(href, outerHtml)
      },
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      onDecorationActivated: (event: DecorationActivatedEvent) => {
        const boundingRect = iframe.getBoundingClientRect()
        const shiftedOffset = offsetToParentCoordinates(
          event.offset,
          boundingRect
        )
        const shiftedRect = rectToParentCoordinates(event.rect, boundingRect)
        const shiftedEvent = {
          id: event.id,
          group: event.group,
          rect: shiftedRect,
          offset: shiftedOffset,
        }
        listener.onDecorationActivated(shiftedEvent)
      },
    }
    this.page = new PageManager(window, iframe, pageListener)
  }

  setMessagePort(messagePort: MessagePort) {
    this.page.setMessagePort(messagePort)
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

    this.listener.onLayout()
  }
}
