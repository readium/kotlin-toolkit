import {
  FixedDoubleDecorationsBridge,
  FixedSingleDecorationsBridge,
} from "../bridge/all-decoration-bridge"
import {
  FixedDoubleSelectionBridge,
  FixedSingleSelectionBridge,
} from "../bridge/all-selection-bridge"
import { FixedDoubleBridge } from "../bridge/fixed-area-bridge"
import { FixedSingleBridge } from "../bridge/fixed-area-bridge"
import { DecorationManager } from "../common/decoration"
import { SelectionManager } from "../common/selection"
import { DecorationWrapperIframeSide } from "./decoration-wrapper"
import { IframeMessageSender } from "./iframe-message"
import { SelectionWrapperIframeSide } from "./selection-wrapper"

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

export class FixedSingleInitializerParentSide {
  private readonly areaBridge: FixedSingleBridge

  private readonly selectionBridge: FixedSingleSelectionBridge

  private readonly decorationsBridge: FixedSingleDecorationsBridge

  constructor(
    window: Window,
    iframe: HTMLIFrameElement,
    areaBridge: FixedSingleBridge,
    selectionBridge: FixedSingleSelectionBridge,
    decorationsBridge: FixedSingleDecorationsBridge
  ) {
    this.areaBridge = areaBridge
    this.selectionBridge = selectionBridge
    this.decorationsBridge = decorationsBridge

    window.addEventListener("message", (event) => {
      if (!event.ports[0]) {
        return
      }

      if (event.source === iframe.contentWindow) {
        this.onInitMessage(event)
      }
    })
  }

  private onInitMessage(event: MessageEvent) {
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
  }

  private initSelection(messagePort: MessagePort) {
    this.selectionBridge.setMessagePort(messagePort)
  }

  private initDecorations(messagePort: MessagePort) {
    this.decorationsBridge.setMessagePort(messagePort)
  }
}

export class FixedDoubleInitializerParentSide {
  private readonly areaBridge: FixedDoubleBridge

  private readonly selectionBridge: FixedDoubleSelectionBridge

  private readonly decorationsBridge: FixedDoubleDecorationsBridge

  constructor(
    window: Window,
    leftIframe: HTMLIFrameElement,
    rightIframe: HTMLIFrameElement,
    areaBridge: FixedDoubleBridge,
    selectionBridge: FixedDoubleSelectionBridge,
    decorationsBridge: FixedDoubleDecorationsBridge
  ) {
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

  private onInitMessageLeft(event: MessageEvent) {
    const initMessage = event.data as InitMessage
    const messagePort = event.ports[0]
    switch (initMessage) {
      case "InitAreaManager":
        this.areaBridge.setLeftMessagePort(messagePort)
        break
      case "InitSelection":
        this.selectionBridge.setLeftMessagePort(messagePort)
        break
      case "InitDecorations":
        this.decorationsBridge.setLeftMessagePort(messagePort)
        break
    }
  }

  private onInitMessageRight(event: MessageEvent) {
    const initMessage = event.data as InitMessage
    const messagePort = event.ports[0]
    switch (initMessage) {
      case "InitAreaManager":
        this.areaBridge.setRightMessagePort(messagePort)
        break
      case "InitSelection":
        this.selectionBridge.setRightMessagePort(messagePort)
        break
      case "InitDecorations":
        this.decorationsBridge.setRightMessagePort(messagePort)
        break
    }
  }
}
