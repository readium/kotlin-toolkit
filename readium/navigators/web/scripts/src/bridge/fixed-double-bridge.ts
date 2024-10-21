import { Insets, Size } from "../common/types"
import { DoubleAreaManager } from "../fixed/double-area-manager"
import { GesturesBridge, BridgeGesturesAdapter } from "./fixed-gestures-bridge"
import { Fit } from "../util/fit"

export class FixedDoubleBridge {
  private readonly manager: DoubleAreaManager

  constructor(
    window: Window,
    leftIframe: HTMLIFrameElement,
    rightIframe: HTMLIFrameElement,
    metaViewport: HTMLMetaElement,
    gestures: GesturesBridge
  ) {
    const listener = new BridgeGesturesAdapter(gestures)
    this.manager = new DoubleAreaManager(
      window,
      leftIframe,
      rightIframe,
      metaViewport,
      listener
    )
  }

  loadSpread(spread: { left?: string; right?: string }) {
    this.manager.loadSpread(spread)
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
