//
//  Copyright 2024 Readium Foundation. All rights reserved.
//  Use of this source code is governed by the BSD-style license
//  available in the top-level LICENSE file of the project.
//

/**
 * Script loaded by reflowable resources.
 */

import { DecorationsBridge } from "./bridge/all-decoration-bridge"
import {
  ReflowableListenerAdapter,
  GesturesBridge,
} from "./bridge/all-listener-bridge"
import { DocumentBridge } from "./bridge/all-listener-bridge"
import { SelectionBridge } from "./bridge/all-selection-bridge"
import { CssBridge } from "./bridge/reflowable-css-bridge"
import { DecorationManager } from "./common/decoration"
import { GesturesDetector } from "./common/gestures"
import { SelectionManager } from "./common/selection"
import { appendVirtualColumnIfNeeded } from "./util/columns"

declare global {
  interface Window {
    documentState: DocumentBridge
    gestures: GesturesBridge
    readiumcss: CssBridge
    decorations: DecorationsBridge
    selection: SelectionBridge
  }
}

const bridgeListener = new ReflowableListenerAdapter(window.gestures)

const decorationManager = new DecorationManager(window)

Window.prototype.readiumcss = new CssBridge(window.document)

Window.prototype.decorations = new DecorationsBridge(window, decorationManager)

Window.prototype.selection = new SelectionBridge(
  window,
  new SelectionManager(window)
)

new GesturesDetector(window, bridgeListener, decorationManager)

window.documentState.onScriptsLoaded()

document.addEventListener("DOMContentLoaded", () => {
  // Setups the `viewport` meta tag to disable overview.
  const meta = document.createElement("meta")
  meta.setAttribute("name", "viewport")
  meta.setAttribute(
    "content",
    "width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, shrink-to-fit=no"
  )
  document.head.appendChild(meta)
})

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
