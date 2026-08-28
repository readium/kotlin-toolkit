//
//  Copyright 2024 Readium Foundation. All rights reserved.
//  Use of this source code is governed by the BSD-style license
//  available in the top-level LICENSE file of the project.
//

/**
 * Script loaded by fixed layout resources.
 */

import {
  DecorationActivatedEvent,
  DecorationManager,
} from "./common/decoration"
import { GesturesDetector, GesturesListener } from "./common/gestures"
import { SelectionManager } from "./common/selection"
import { Size } from "./common/geometry"
import { IframeMessageSender } from "./fixed/iframe-message"
import { parseViewportString } from "./util/viewport"
import { FixedInitializerIframeSide } from "./bridge/all-initialization-bridge"

const initializer = new FixedInitializerIframeSide(window)
const messageSender = initializer.initAreaManager()

const selectionManager = new SelectionManager(window)
initializer.initSelection(selectionManager)

const decorationManager = new DecorationManager(window)
initializer.initDecorations(decorationManager)

const viewportSize = parseContentSize(window.document)
messageSender.send({ kind: "contentSize", size: viewportSize })

class MessagingGesturesListener implements GesturesListener {
  readonly messageSender: IframeMessageSender

  constructor(messageSender: IframeMessageSender) {
    this.messageSender = messageSender
  }

  onTap(gestureEvent: MouseEvent): void {
    const event = {
      offset: { x: gestureEvent.clientX, y: gestureEvent.clientY },
    }
    this.messageSender.send({ kind: "tap", event: event })
  }

  onLinkActivated(href: string, outerHtml: string): void {
    this.messageSender.send({
      kind: "linkActivated",
      href: href,
      outerHtml: outerHtml,
    })
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  onDecorationActivated(gestureEvent: DecorationActivatedEvent): void {
    const event = {
      id: gestureEvent.id,
      group: gestureEvent.group,
      rect: gestureEvent.rect,
      offset: { x: gestureEvent.event.clientX, y: gestureEvent.event.clientY },
    }
    this.messageSender.send({
      kind: "decorationActivated",
      event: event,
    })
  }
}

const messagingListener = new MessagingGesturesListener(messageSender)

new GesturesDetector(window, messagingListener, decorationManager)

function parseContentSize(document: Document): Size | undefined {
  const viewport = document.querySelector("meta[name=viewport]")

  if (!viewport || !(viewport instanceof HTMLMetaElement)) {
    return undefined
  }

  return parseViewportString(viewport.content)
}
