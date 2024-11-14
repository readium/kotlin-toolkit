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

export type IframeMessage =
  | ContentSizeMessage
  | TapMessage
  | LinkActivatedMessage

export class IframeMessageSender {
  private messagePort: MessagePort

  constructor(messagePort: MessagePort) {
    this.messagePort = messagePort
  }

  send(message: IframeMessage) {
    this.messagePort.postMessage(message)
  }
}
