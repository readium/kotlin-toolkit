//
//  Copyright 2024 Readium Foundation. All rights reserved.
//  Use of this source code is governed by the BSD-style license
//  available in the top-level LICENSE file of the project.
//

/**
 *  Script loaded by the single area HTML wrapper for fixed layout resources.
 */

import { DocumentBridge, GesturesBridge } from "./bridge/all-listener-bridge"
import { FixedDoubleBridge } from "./bridge/fixed-double-bridge"

declare global {
  interface Window {
    documentState: DocumentBridge
    doubleArea: FixedDoubleBridge
    gestures: GesturesBridge
  }
}

const leftIframe = document.getElementById("page-left") as HTMLIFrameElement

const rightIframe = document.getElementById("page-right") as HTMLIFrameElement

const metaViewport = document.querySelector(
  "meta[name=viewport]"
) as HTMLMetaElement

Window.prototype.doubleArea = new FixedDoubleBridge(
  window,
  leftIframe,
  rightIframe,
  metaViewport,
  window.gestures,
  window.documentState
)

window.documentState.onScriptsLoaded()
