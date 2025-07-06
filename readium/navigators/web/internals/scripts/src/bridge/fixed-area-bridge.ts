import { Insets, Size } from "../common/geometry"
import { DoubleAreaManager } from "../fixed/double-area-manager"
import {
  GesturesBridge,
  FixedListenerAdapter,
  DocumentStateBridge,
} from "./all-listener-bridge"
import { Fit } from "../fixed/fit"
import { SingleAreaManager } from "../fixed/single-area-manager"

export class FixedSingleAreaBridge {
  private readonly manager: SingleAreaManager

  constructor(
    window: Window,
    iframe: HTMLIFrameElement,
    metaViewport: HTMLMetaElement,
    gesturesBridge: GesturesBridge,
    documentBridge: DocumentStateBridge
  ) {
    const listener = new FixedListenerAdapter(
      window,
      gesturesBridge,
      documentBridge
    )
    this.manager = new SingleAreaManager(window, iframe, metaViewport, listener)
  }

  setMessagePort(messagePort: MessagePort) {
    this.manager.setMessagePort(messagePort)
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

export class FixedDoubleAreaBridge {
  private readonly manager: DoubleAreaManager

  constructor(
    window: Window,
    leftIframe: HTMLIFrameElement,
    rightIframe: HTMLIFrameElement,
    metaViewport: HTMLMetaElement,
    gesturesBridge: GesturesBridge,
    documentBridge: DocumentStateBridge
  ) {
    const listener = new FixedListenerAdapter(
      window,
      gesturesBridge,
      documentBridge
    )
    this.manager = new DoubleAreaManager(
      window,
      leftIframe,
      rightIframe,
      metaViewport,
      listener
    )
  }

  setLeftMessagePort(messagePort: MessagePort) {
    this.manager.setLeftMessagePort(messagePort)
  }

  setRightMessagePort(messagePort: MessagePort) {
    this.manager.setRightMessagePort(messagePort)
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
