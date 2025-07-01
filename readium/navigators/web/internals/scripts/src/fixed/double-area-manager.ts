import { Size, Insets } from "../common/types"
import { computeScale, Fit } from "../util/fit"
import { PageManager } from "./page-manager"
import { AreaManager, shiftOffset, shiftRect } from "./area-manager"
import { ViewportStringBuilder } from "../util/viewport"
import { GesturesDetector } from "../common/gestures"
import { DecorationActivatedEvent, TapEvent } from "./events"
import { DecorationActivatedEvent as OriginalDecorationActivated } from "../common/decoration"

export class DoubleAreaManager {
  private readonly metaViewport: HTMLMetaElement

  private readonly leftPage: PageManager

  private readonly rightPage: PageManager

  private readonly listener: AreaManager.Listener

  private fit: Fit = Fit.Contain

  private insets: Insets = { top: 0, right: 0, bottom: 0, left: 0 }

  private viewport?: Size

  private spread?: { left?: string; right?: string }

  constructor(
    window: Window,
    leftIframe: HTMLIFrameElement,
    rightIframe: HTMLIFrameElement,
    metaViewport: HTMLMetaElement,
    listener: AreaManager.Listener
  ) {
    this.listener = listener

    window.addEventListener("message", (event) => {
      if (!event.ports[0]) {
        return
      }

      if (event.source === leftIframe.contentWindow) {
        this.leftPage.setMessagePort(event.ports[0])
      } else if (event.source == rightIframe.contentWindow) {
        this.rightPage.setMessagePort(event.ports[0])
      }
    })

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

    const leftPageListener = {
      onIframeLoaded: () => {
        this.layout()
      },
      onTap: (gestureEvent: TapEvent) => {
        const boundingRect = leftIframe.getBoundingClientRect()
        const shiftedOffset = shiftOffset(gestureEvent.offset, boundingRect)
        listener.onTap({ offset: shiftedOffset })
      },
      onLinkActivated: (href: string, outerHtml: string) => {
        listener.onLinkActivated(href, outerHtml)
      },
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      onDecorationActivated: (gestureEvent: DecorationActivatedEvent) => {
        const boundingRect = leftIframe.getBoundingClientRect()
        const shiftedOffset = shiftOffset(gestureEvent.offset, boundingRect)
        const shiftedRect = shiftRect(gestureEvent.rect, boundingRect)
        const shiftedEvent = {
          id: gestureEvent.id,
          group: gestureEvent.group,
          rect: shiftedRect,
          offset: shiftedOffset,
        }
        listener.onDecorationActivated(shiftedEvent)
      },
    }

    const rightPageListener = {
      onIframeLoaded: () => {
        this.layout()
      },
      onTap: (gestureEvent: TapEvent) => {
        const boundingRect = rightIframe.getBoundingClientRect()
        const shiftedOffset = shiftOffset(gestureEvent.offset, boundingRect)
        listener.onTap({ offset: shiftedOffset })
      },
      onLinkActivated: (href: string, outerHtml: string) => {
        listener.onLinkActivated(href, outerHtml)
      },
      onDecorationActivated: (gestureEvent: DecorationActivatedEvent) => {
        const boundingRect = rightIframe.getBoundingClientRect()
        const shiftedOffset = shiftOffset(gestureEvent.offset, boundingRect)
        const shiftedRect = shiftRect(gestureEvent.rect, boundingRect)
        const shiftedEvent = {
          id: gestureEvent.id,
          group: gestureEvent.group,
          rect: shiftedRect,
          offset: shiftedOffset,
        }
        listener.onDecorationActivated(shiftedEvent)
      },
    }
    this.leftPage = new PageManager(window, leftIframe, leftPageListener)
    this.rightPage = new PageManager(window, rightIframe, rightPageListener)
    this.metaViewport = metaViewport
  }

  loadSpread(spread: { left?: string; right?: string }) {
    this.leftPage.hide()
    this.rightPage.hide()
    this.spread = spread

    if (spread.left) {
      this.leftPage.loadPage(spread.left)
    }

    if (spread.right) {
      this.rightPage.loadPage(spread.right)
    }
  }

  setViewport(size: Size, insets: Insets) {
    if (this.viewport == size && this.insets == insets) {
      return
    }

    this.viewport = size
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

  private layout() {
    if (
      !this.viewport ||
      (!this.leftPage.size && this.spread!.left) ||
      (!this.rightPage.size && this.spread!.right)
    ) {
      return
    }

    const leftMargins = {
      top: this.insets.top,
      right: 0,
      bottom: this.insets.bottom,
      left: this.insets.left,
    }
    this.leftPage.setMargins(leftMargins)
    const rightMargins = {
      top: this.insets.top,
      right: this.insets.right,
      bottom: this.insets.bottom,
      left: 0,
    }
    this.rightPage.setMargins(rightMargins)

    if (!this.spread!.right) {
      this.rightPage.setPlaceholder(this.leftPage.size!)
    } else if (!this.spread!.left) {
      this.leftPage.setPlaceholder(this.rightPage.size!)
    }

    const contentWidth = this.leftPage.size!.width + this.rightPage.size!.width
    const contentHeight = Math.max(
      this.leftPage.size!.height,
      this.rightPage.size!.height
    )
    const contentSize = { width: contentWidth, height: contentHeight }
    const safeDrawingSize = {
      width: this.viewport.width - this.insets.left - this.insets.right,
      height: this.viewport.height - this.insets.top - this.insets.bottom,
    }
    const scale = computeScale(this.fit, contentSize, safeDrawingSize)

    this.metaViewport.content = new ViewportStringBuilder()
      .setInitialScale(scale)
      .setMinimumScale(scale)
      .setWidth(contentWidth)
      .setHeight(contentHeight)
      .build()

    this.leftPage.show()
    this.rightPage.show()

    this.listener.onLayout()
  }
}
