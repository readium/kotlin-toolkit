//
//  Copyright 2024 Readium Foundation. All rights reserved.
//  Use of this source code is governed by the BSD-style license
//  available in the top-level LICENSE file of the project.
//

/**
 *  Script loaded by the single area HTML wrapper for fixed layout resources.
 */

import { GesturesBridge } from "./bridge/all-gestures-bridge"
import { InitializationBridge } from "./bridge/all-initialization-bridge"
import { FixedSingleBridge } from "./bridge/fixed-single-bridge"

declare global {
  interface Window {
    initialization: InitializationBridge
    singleArea: FixedSingleBridge
    gestures: GesturesBridge
  }
}

const iframe = document.getElementById("page") as HTMLIFrameElement

const metaViewport = document.querySelector(
  "meta[name=viewport]"
) as HTMLMetaElement

window.singleArea = new FixedSingleBridge(
  window,
  iframe,
  metaViewport,
  window.gestures
)

window.initialization.onScriptsLoaded()
// eslint-disable-next-line @typescript-eslint/no-unused-vars
window.addEventListener("load", (event) => {
  window.initialization.onDocumentLoaded()
})
