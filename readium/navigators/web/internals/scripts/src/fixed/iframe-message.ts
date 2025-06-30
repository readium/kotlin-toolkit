import { DecorationActivatedEvent } from "../common/decoration"
import { Size } from "../common/types"

export interface ContentSizeMessage {
  kind: "contentSize"
  size?: Size
}

export interface TapMessage {
  kind: "tap"
  x: number
  y: number
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
