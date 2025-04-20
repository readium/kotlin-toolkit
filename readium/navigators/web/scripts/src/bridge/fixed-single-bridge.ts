import { Insets, Size } from "../common/types"
import { SingleAreaManager } from "../fixed/single-area-manager"
import { Fit } from "../util/fit"
import {
  GesturesBridge,
  FixedListenerAdapter,
  DocumentBridge,
} from "./all-listener-bridge"

export class FixedSingleBridge {
  private readonly manager: SingleAreaManager

  constructor(
    window: Window,
    iframe: HTMLIFrameElement,
    metaViewport: HTMLMetaElement,
    gesturesBridge: GesturesBridge,
    documentBridge: DocumentBridge
  ) {
    const listener = new FixedListenerAdapter(
      window,
      gesturesBridge,
      documentBridge
    )
    this.manager = new SingleAreaManager(window, iframe, metaViewport, listener)
  }

  loadResource(url: string) {
    this.manager.loadResource(url)
  }

  setViewport(
    viewporttWidth: number,
    viewportHeight: number,
    insetTop: number,
    insetRight: number,
    insetBottom: number,
    insetLeft: number
  ) {
    const viewport: Size = { width: viewporttWidth, height: viewportHeight }
    const insets: Insets = {
      top: insetTop,
      left: insetLeft,
      bottom: insetBottom,
      right: insetRight,
    }
    this.manager.setViewport(viewport, insets)
  }

  setFit(fit: string) {
    if (fit != "contain" && fit != "width" && fit != "height") {
      throw Error(`Invalid fit value: ${fit}`)
    }

    this.manager.setFit(fit as Fit)
  }
}
