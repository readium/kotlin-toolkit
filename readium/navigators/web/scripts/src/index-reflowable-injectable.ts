//
//  Copyright 2024 Readium Foundation. All rights reserved.
//  Use of this source code is governed by the BSD-style license
//  available in the top-level LICENSE file of the project.
//

/**
 * Script loaded by reflowable resources.
 */

import {
  BridgeGesturesAdapter,
  GesturesBridge,
} from "./bridge/all-gestures-bridge"
import { DocumentBridge } from "./bridge/all-document-bridge"
import { CssBridge } from "./bridge/reflowable-css-bridge"
import { GesturesDetector } from "./common/gestures"
import { appendVirtualColumnIfNeeded } from "./util/columns"

declare global {
  interface Window {
    documentState: DocumentBridge
    gestures: GesturesBridge
    readiumcss: CssBridge
  }
}

const bridgeListener = new BridgeGesturesAdapter(window.gestures)

const gesturesListener = {
  onTap: (event: MouseEvent) => {
    const tapEvent = {
      x: (event.clientX - visualViewport!.offsetLeft) * visualViewport!.scale,
      y: (event.clientY - visualViewport!.offsetTop) * visualViewport!.scale,
    }
    bridgeListener.onTap(tapEvent)
  },
  onLinkActivated: (href: string, outerHtml: string) => {
    bridgeListener.onLinkActivated(href, outerHtml)
  },
}

new GesturesDetector(window, gesturesListener)

Window.prototype.readiumcss = new CssBridge(window.document)

window.documentState.onScriptsLoaded()
// eslint-disable-next-line @typescript-eslint/no-unused-vars
window.addEventListener("load", (event) => {
  let documentLoadedFired = false

  const observer = new ResizeObserver(() => {
    requestAnimationFrame(() => {
      appendVirtualColumnIfNeeded(window)

      const scrollingElement = window.document.scrollingElement

      if (
        !documentLoadedFired &&
        (scrollingElement == null ||
          (scrollingElement.scrollHeight > 0 &&
            scrollingElement.scrollWidth > 0))
      ) {
        console.log(`scrollWidth ${scrollingElement?.scrollWidth}`)
        window.documentState.onDocumentLoaded()
        documentLoadedFired = true
      }

      window.documentState.onDocumentResized()
    })
  })
  observer.observe(document.body)
})
