//
//  Copyright 2024 Readium Foundation. All rights reserved.
//  Use of this source code is governed by the BSD-style license
//  available in the top-level LICENSE file of the project.
//

/**
 * Script loaded by reflowable resources.
 */

import { ReflowableDecorationsBridge } from "./bridge/all-decoration-bridge"
import { GesturesBridge } from "./bridge/all-listener-bridge"
import { DocumentStateBridge } from "./bridge/all-listener-bridge"
import { ReflowableSelectionBridge } from "./bridge/all-selection-bridge"
import { CssBridge } from "./bridge/reflowable-css-bridge"
import {
  ReflowableApiStateListener,
  ReflowableInitializationBridge as ReflowableInitializer,
} from "./bridge/reflowable-initialization-bridge"
import { appendVirtualColumnIfNeeded } from "./util/columns"

declare global {
  interface Window {
    // Web APIs available for native code
    reflowableApiState: ReflowableApiStateListener
    readiumcss: CssBridge
    decorations: ReflowableDecorationsBridge
    selection: ReflowableSelectionBridge
    // Native APIs available for web code
    documentState: DocumentStateBridge
    gestures: GesturesBridge
  }
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
window.addEventListener("load", (event) => {
  let documentLoadedFired = false

  const observer = new ResizeObserver(() => {
    let colCountFixed = false

    requestAnimationFrame(() => {
      const scrollingElement = window.document.scrollingElement
      const scrollingElementEmpty =
        scrollingElement == null ||
        (scrollingElement.scrollHeight == 0 &&
          scrollingElement.scrollWidth == 0)

      if (!documentLoadedFired && scrollingElementEmpty) {
        // Document is not sized yet
        return
      }

      if (!colCountFixed && !scrollingElementEmpty) {
        const colChanged = appendVirtualColumnIfNeeded(window)
        colCountFixed = true
        if (colChanged) {
          // Column number has changed, wait for next resize callback.
          return
        }
      }

      colCountFixed = false

      if (!documentLoadedFired) {
        window.documentState.onDocumentLoadedAndSized()
        documentLoadedFired = true
      } else {
        window.documentState.onDocumentResized()
      }
    })
  })
  observer.observe(document.body)
})

new ReflowableInitializer(window, window.reflowableApiState)
