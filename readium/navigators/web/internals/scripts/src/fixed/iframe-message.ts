import { Size } from "../common/geometry"
import { DecorationActivatedEvent, TapEvent } from "./events"

export interface ContentSizeMessage {
  kind: "contentSize"
  size?: Size
}

export interface TapMessage {
  kind: "tap"
  event: TapEvent
}

export interface LinkActivatedMessage {
  kind: "linkActivated"
  href: string
  outerHtml: string
}

export interface DecorationActivatedMessage {
  kind: "decorationActivated"
  event: DecorationActivatedEvent
}

export type IframeMessage =
  | ContentSizeMessage
  | TapMessage
  | LinkActivatedMessage
  | DecorationActivatedMessage

export class IframeMessageSender {
  private messagePort: MessagePort

  constructor(messagePort: MessagePort) {
    this.messagePort = messagePort
  }

  send(message: IframeMessage) {
    this.messagePort.postMessage(message)
  }
}
