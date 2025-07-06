import { DecorationActivatedEvent as OriginalDecorationActivated } from "../common/decoration"
import { GesturesListener } from "../common/gestures"
import { AreaManager } from "../fixed/area-manager"
import { DecorationActivatedEvent, TapEvent } from "../fixed/events"

export interface GesturesBridge {
  onTap(event: string): void
  onLinkActivated(href: string, outerHtml: string): void
  onDecorationActivated(
    id: string,
    group: string,
    rect: string,
    offset: string
  ): void
}

export interface DocumentStateBridge {
  onDocumentLoadedAndSized: () => void
  onDocumentResized: () => void
}

export class ReflowableListenerAdapter implements GesturesListener {
  readonly gesturesBridge: GesturesBridge

  constructor(gesturesBridge: GesturesBridge) {
    this.gesturesBridge = gesturesBridge
  }

  onTap(event: MouseEvent) {
    const tapEvent = {
      x: (event.clientX - visualViewport!.offsetLeft) * visualViewport!.scale,
      y: (event.clientY - visualViewport!.offsetTop) * visualViewport!.scale,
    }
    const stringEvent = JSON.stringify(tapEvent)
    this.gesturesBridge.onTap(stringEvent)
  }
  onLinkActivated(href: string, outerHtml: string) {
    this.gesturesBridge.onLinkActivated(href, outerHtml)
  }

  onDecorationActivated(event: OriginalDecorationActivated): void {
    const offset = {
      x:
        (event.event.clientX - visualViewport!.offsetLeft) *
        visualViewport!.scale,
      y:
        (event.event.clientY - visualViewport!.offsetTop) *
        visualViewport!.scale,
    }
    const stringOffset = JSON.stringify(offset)
    const stringRect = JSON.stringify(event.rect)
    this.gesturesBridge.onDecorationActivated(
      event.id,
      event.group,
      stringRect,
      stringOffset
    )
  }
}

export class FixedListenerAdapter implements AreaManager.Listener {
  readonly gesturesApi: GesturesBridge
  readonly documentApi: DocumentStateBridge
  readonly window: Window
  resizeObserverAdded: boolean
  documentLoadedFired: boolean

  constructor(
    window: Window,
    gesturesApi: GesturesBridge,
    documentApi: DocumentStateBridge
  ) {
    this.window = window
    this.gesturesApi = gesturesApi
    this.documentApi = documentApi
    this.resizeObserverAdded = false
    this.documentLoadedFired = false
  }

  onTap(event: TapEvent): void {
    this.gesturesApi.onTap(JSON.stringify(event.offset))
  }

  onLinkActivated(href: string, outerHtml: string): void {
    this.gesturesApi.onLinkActivated(href, outerHtml)
  }

  onDecorationActivated(event: DecorationActivatedEvent): void {
    const stringOffset = JSON.stringify(event.offset)
    const stringRect = JSON.stringify(event.rect)
    this.gesturesApi.onDecorationActivated(
      event.id,
      event.group,
      stringRect,
      stringOffset
    )
  }

  onLayout(): void {
    if (!this.resizeObserverAdded) {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const observer = new ResizeObserver(() => {
        requestAnimationFrame(() => {
          const scrollingElement = this.window.document.scrollingElement

          if (
            !this.documentLoadedFired &&
            (scrollingElement == null ||
              scrollingElement.scrollHeight > 0 ||
              scrollingElement.scrollWidth > 0)
          ) {
            this.documentApi.onDocumentLoadedAndSized()
            this.documentLoadedFired = true
          } else {
            this.documentApi.onDocumentResized()
          }
        })
      })
      observer.observe(this.window.document.body)
    }
    this.resizeObserverAdded = true
  }
}
