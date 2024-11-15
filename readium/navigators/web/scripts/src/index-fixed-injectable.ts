//
//  Copyright 2024 Readium Foundation. All rights reserved.
//  Use of this source code is governed by the BSD-style license
//  available in the top-level LICENSE file of the project.
//

/**
 * Script loaded by fixed layout resources.
 */

import { GesturesDetector, GesturesListener } from "./common/gestures"
import { Size } from "./common/types"
import { IframeMessageSender } from "./fixed/iframe-message"
import { parseViewportString } from "./util/viewport"

const messageChannel = new MessageChannel()
window.parent.postMessage("Init", "*", [messageChannel.port2])
const messageSender = new IframeMessageSender(messageChannel.port1)

const viewportSize = parseContentSize(window.document)
messageSender.send({ kind: "contentSize", size: viewportSize })

class MessagingGesturesListener implements GesturesListener {
  readonly messageSender: IframeMessageSender

  constructor(messageSender: IframeMessageSender) {
    this.messageSender = messageSender
  }

  onTap(event: MouseEvent): void {
    this.messageSender.send({ kind: "tap", x: event.clientX, y: event.clientY })
  }

  onLinkActivated(href: string, outerHtml: string): void {
    this.messageSender.send({
      kind: "linkActivated",
      href: href,
      outerHtml: outerHtml,
    })
  }
}

const messagingListener = new MessagingGesturesListener(messageSender)
new GesturesDetector(window, messagingListener)

function parseContentSize(document: Document): Size | undefined {
  const viewport = document.querySelector("meta[name=viewport]")

  if (!viewport || !(viewport instanceof HTMLMetaElement)) {
    return undefined
  }

  return parseViewportString(viewport.content)
}
