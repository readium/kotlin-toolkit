//
//  Copyright 2024 Readium Foundation. All rights reserved.
//  Use of this source code is governed by the BSD-style license
//  available in the top-level LICENSE file of the project.
//

/**
 * Script loaded by fixed layout resources.
 */

import { GesturesDetector, GesturesListener } from "./common/gestures"

window.addEventListener("message", (event) => {
  if (event.ports[0]) {
    const messagePort = event.ports[0]
    const messagingListener = new MessagingGesturesListener(messagePort)
    new GesturesDetector(window, messagingListener)
  }
})

class MessagingGesturesListener implements GesturesListener {
  readonly messagePort: MessagePort

  constructor(messagePort: MessagePort) {
    this.messagePort = messagePort
  }

  onTap(event: MouseEvent): void {
    const tapEvent = { x: event.clientX, y: event.clientY }
    this.messagePort.postMessage(tapEvent)
  }

  onLinkActivated(href: string): void {
    this.messagePort.postMessage({ href: href })
  }
}
