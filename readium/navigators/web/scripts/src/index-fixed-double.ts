//
//  Copyright 2024 Readium Foundation. All rights reserved.
//  Use of this source code is governed by the BSD-style license
//  available in the top-level LICENSE file of the project.
//

/**
 *  Script loaded by the single area HTML wrapper for fixed layout resources.
 */

import { GesturesBridge } from "./bridge/fixed-gestures-bridge"
import { FixedDoubleBridge } from "./bridge/fixed-double-bridge"
import { InitializationBridge } from "./bridge/fixed-initialization-bridge"

declare global {
  interface Window {
    initialization: InitializationBridge
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
  window.gestures
)

window.initialization.onScriptsLoaded()
