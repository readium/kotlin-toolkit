import { GesturesListener } from "../common/gestures"
import { AreaManager } from "../fixed/area-manager"

export interface GesturesBridge {
  onTap(event: string): void
  onLinkActivated(href: string, outerHtml: string): void
}

export interface TapEvent {
  x: number
  y: number
}

export interface DocumentBridge {
  onScriptsLoaded: () => void
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
}

export class FixedListenerAdapter implements AreaManager.Listener {
  readonly gesturesApi: GesturesBridge
  readonly documentApi: DocumentBridge
  readonly window: Window
  resizeObserverAdded: boolean
  documentLoadedFired: boolean

  constructor(
    window: Window,
    gesturesApi: GesturesBridge,
    documentApi: DocumentBridge
  ) {
    this.window = window
    this.gesturesApi = gesturesApi
    this.documentApi = documentApi
    this.resizeObserverAdded = false
    this.documentLoadedFired = false
  }

  onTap(event: TapEvent): void {
    this.gesturesApi.onTap(JSON.stringify(event))
  }

  onLinkActivated(href: string, outerHtml: string): void {
    this.gesturesApi.onLinkActivated(href, outerHtml)
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
