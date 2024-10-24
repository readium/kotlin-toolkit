import { Size } from "../common/types"

interface ContentSizeMessage {
  kind: "contentSize"
  size?: Size
}

interface TapMessage {
  kind: "tap"
  x: number
  y: number
}

interface LinkActivatedMessage {
  kind: "linkActivated"
  href: string
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
