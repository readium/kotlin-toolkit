import { DecorationManager } from "../common/decoration"
import { SelectionManager } from "../common/selection"
import { DecorationWrapperIframeSide } from "../fixed/decoration-wrapper"
import { IframeMessageSender } from "../fixed/iframe-message"
import { SelectionWrapperIframeSide } from "../fixed/selection-wrapper"
import {
  FixedDoubleDecorationsBridge,
  FixedSingleDecorationsBridge,
} from "./all-decoration-bridge"
import {
  FixedDoubleSelectionBridge,
  FixedSingleSelectionBridge,
} from "./all-selection-bridge"
import {
  FixedDoubleAreaBridge,
  FixedSingleAreaBridge,
} from "./fixed-area-bridge"

export class FixedSingleInitializationBridge {
  private readonly window: Window

  private readonly iframe: HTMLIFrameElement

  private readonly listener: FixedApiStateListener

  private readonly areaBridge: FixedSingleAreaBridge

  private readonly selectionBridge: FixedSingleSelectionBridge

  private readonly decorationsBridge: FixedSingleDecorationsBridge

  constructor(
    window: Window,
    listener: FixedApiStateListener,
    iframe: HTMLIFrameElement,
    areaBridge: FixedSingleAreaBridge,
    selectionBridge: FixedSingleSelectionBridge,
    decorationsBridge: FixedSingleDecorationsBridge
  ) {
    this.window = window
    this.listener = listener
    this.iframe = iframe

    this.areaBridge = areaBridge
    this.selectionBridge = selectionBridge
    this.decorationsBridge = decorationsBridge
  }

  loadResource(url: string) {
    this.iframe.src = url

    this.window.addEventListener("message", (event) => {
      console.log("message")
      if (!event.ports[0]) {
        return
      }

      if (event.source === this.iframe.contentWindow) {
        this.onInitMessage(event)
      }
    })
  }

  private onInitMessage(event: MessageEvent) {
    console.log(`receiving init message ${event}`)
    const initMessage = event.data as InitMessage
    const messagePort = event.ports[0]
    switch (initMessage) {
      case "InitAreaManager":
        return this.initAreaManager(messagePort)
      case "InitSelection":
        return this.initSelection(messagePort)
      case "InitDecorations":
        return this.initDecorations(messagePort)
    }
  }

  private initAreaManager(messagePort: MessagePort) {
    this.areaBridge.setMessagePort(messagePort)
    this.listener.onAreaApiAvailable()
  }

  private initSelection(messagePort: MessagePort) {
    this.selectionBridge.setMessagePort(messagePort)
    this.listener.onSelectionApiAvailable()
  }

  private initDecorations(messagePort: MessagePort) {
    this.decorationsBridge.setMessagePort(messagePort)
    this.listener.onDecorationApiAvailable()
  }
}

export class FixedDoubleInitializationBridge {
  private readonly listener: FixedApiStateListener

  private readonly areaBridge: FixedDoubleAreaBridge

  private readonly selectionBridge: FixedDoubleSelectionBridge

  private readonly decorationsBridge: FixedDoubleDecorationsBridge

  private areaReadySemaphore = 2

  private selectionReadySemaphore = 2

  private decorationReadySemaphore = 2

  constructor(
    window: Window,
    listener: FixedApiStateListener,
    leftIframe: HTMLIFrameElement,
    rightIframe: HTMLIFrameElement,
    areaBridge: FixedDoubleAreaBridge,
    selectionBridge: FixedDoubleSelectionBridge,
    decorationsBridge: FixedDoubleDecorationsBridge
  ) {
    this.listener = listener
    this.areaBridge = areaBridge
    this.selectionBridge = selectionBridge
    this.decorationsBridge = decorationsBridge

    window.addEventListener("message", (event) => {
      if (!event.ports[0]) {
        return
      }

      if (event.source === leftIframe.contentWindow) {
        this.onInitMessageLeft(event)
      } else if (event.source == rightIframe.contentWindow) {
        this.onInitMessageRight(event)
      }
    })
  }

  loadSpread(spread: { left?: string; right?: string }) {
    const pageNb = (spread.left ? 1 : 0) + (spread.right ? 1 : 0)

    this.areaReadySemaphore = pageNb
    this.selectionReadySemaphore = pageNb
    this.decorationReadySemaphore = pageNb

    this.areaBridge.loadSpread(spread)
  }

  private onInitMessageLeft(event: MessageEvent) {
    const initMessage = event.data as InitMessage
    const messagePort = event.ports[0]
    switch (initMessage) {
      case "InitAreaManager":
        this.areaBridge.setLeftMessagePort(messagePort)
        this.onInitAreaMessage()
        break
      case "InitSelection":
        this.selectionBridge.setLeftMessagePort(messagePort)
        this.onInitSelectionMessage()
        break
      case "InitDecorations":
        this.decorationsBridge.setLeftMessagePort(messagePort)
        this.onInitDecorationMessage()
        break
    }
  }

  private onInitMessageRight(event: MessageEvent) {
    const initMessage = event.data as InitMessage
    const messagePort = event.ports[0]
    switch (initMessage) {
      case "InitAreaManager":
        this.areaBridge.setRightMessagePort(messagePort)
        this.onInitAreaMessage()
        break
      case "InitSelection":
        this.selectionBridge.setRightMessagePort(messagePort)
        this.onInitSelectionMessage()
        break
      case "InitDecorations":
        this.decorationsBridge.setRightMessagePort(messagePort)
        this.onInitDecorationMessage()
        break
    }
  }

  private onInitAreaMessage() {
    this.areaReadySemaphore -= 1
    if (this.areaReadySemaphore == 0) {
      this.listener.onAreaApiAvailable()
    }
  }

  private onInitSelectionMessage() {
    this.selectionReadySemaphore -= 1
    if (this.selectionReadySemaphore == 0) {
      this.listener.onSelectionApiAvailable()
    }
  }

  private onInitDecorationMessage() {
    this.decorationReadySemaphore -= 1
    if (this.decorationReadySemaphore == 0) {
      this.listener.onDecorationApiAvailable()
    }
  }
}

export interface FixedApiStateListener {
  onInitializationApiAvailable(): void
  onAreaApiAvailable(): void
  onSelectionApiAvailable(): void
  onDecorationApiAvailable(): void
}

export type InitMessage =
  | "InitAreaManager"
  | "InitSelection"
  | "InitDecorations"

export class FixedInitializerIframeSide {
  private readonly window: Window

  constructor(window: Window) {
    this.window = window
  }

  initAreaManager(): IframeMessageSender {
    const messagePort = this.initChannel("InitAreaManager")
    return new IframeMessageSender(messagePort)
  }

  initSelection(selectionManager: SelectionManager) {
    const messagePort = this.initChannel("InitSelection")
    new SelectionWrapperIframeSide(messagePort, selectionManager)
  }

  initDecorations(decorationManager: DecorationManager) {
    const messagePort = this.initChannel("InitDecorations")
    new DecorationWrapperIframeSide(messagePort, decorationManager)
  }

  private initChannel(initMessage: InitMessage): MessagePort {
    const messageChannel = new MessageChannel()
    this.window.parent.postMessage(initMessage, "*", [messageChannel.port2])
    return messageChannel.port1
  }
}
