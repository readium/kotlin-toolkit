//
//  Copyright 2024 Readium Foundation. All rights reserved.
//  Use of this source code is governed by the BSD-style license
//  available in the top-level LICENSE file of the project.
//

/**
 * Script loaded by reflowable resources.
 */

import {
  ReflowableListenerAdapter,
  GesturesBridge,
} from "./bridge/all-listener-bridge"
import { DocumentBridge } from "./bridge/all-listener-bridge"
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

const bridgeListener = new ReflowableListenerAdapter(window.gestures)

new GesturesDetector(window, bridgeListener)

Window.prototype.readiumcss = new CssBridge(window.document)

window.documentState.onScriptsLoaded()
// eslint-disable-next-line @typescript-eslint/no-unused-vars
window.addEventListener("load", (event) => {
  let documentLoadedFired = false

  const observer = new ResizeObserver(() => {
    requestAnimationFrame(() => {
      if (appendVirtualColumnIfNeeded(window)) {
        // Column has been added or removed, wait for next resize callback.
        return
      }

      if (!documentLoadedFired) {
        const scrollingElement = window.document.scrollingElement

        if (
          scrollingElement != null &&
          scrollingElement.scrollHeight == 0 &&
          scrollingElement.scrollWidth == 0
        ) {
          // Document is not sized yet
          return
        }

        window.documentState.onDocumentLoadedAndSized()
        documentLoadedFired = true
      } else {
        window.documentState.onDocumentResized()
      }
    })
  })
  observer.observe(document.body)
})
