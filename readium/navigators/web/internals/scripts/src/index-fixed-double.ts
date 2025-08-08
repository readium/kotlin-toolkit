//
//  Copyright 2024 Readium Foundation. All rights reserved.
//  Use of this source code is governed by the BSD-style license
//  available in the top-level LICENSE file of the project.
//

/**
 *  Script loaded by the single area HTML wrapper for fixed layout resources.
 */

import {
  DocumentStateBridge,
  GesturesBridge,
} from "./bridge/all-listener-bridge"
import { FixedDoubleAreaBridge as FixedDoubleAreaBridge } from "./bridge/fixed-area-bridge"
import { FixedDoubleSelectionBridge } from "./bridge/all-selection-bridge"
import { FixedDoubleDecorationsBridge } from "./bridge/all-decoration-bridge"
import {
  FixedApiStateListener,
  FixedDoubleInitializationBridge,
} from "./bridge/all-initialization-bridge"

declare global {
  interface Window {
    // Web APIs available for native code
    doubleInitialization: FixedDoubleInitializationBridge
    doubleArea: FixedDoubleAreaBridge
    doubleSelection: FixedDoubleSelectionBridge
    doubleDecorations: FixedDoubleDecorationsBridge
    // Native APIs available for web code
    fixedApiState: FixedApiStateListener
    documentState: DocumentStateBridge
    gestures: GesturesBridge
    doubleSelectionListener: FixedDoubleSelectionBridge.Listener
  }
}

const leftIframe = document.getElementById("page-left") as HTMLIFrameElement

const rightIframe = document.getElementById("page-right") as HTMLIFrameElement

const metaViewport = document.querySelector(
  "meta[name=viewport]"
) as HTMLMetaElement

Window.prototype.doubleArea = new FixedDoubleAreaBridge(
  window,
  leftIframe,
  rightIframe,
  metaViewport,
  window.gestures,
  window.documentState
)

window.doubleSelection = new FixedDoubleSelectionBridge(
  leftIframe,
  rightIframe,
  window.doubleSelectionListener
)

window.doubleDecorations = new FixedDoubleDecorationsBridge()

window.doubleInitialization = new FixedDoubleInitializationBridge(
  window,
  window.fixedApiState,
  leftIframe,
  rightIframe,
  window.doubleArea,
  window.doubleSelection,
  window.doubleDecorations
)

window.fixedApiState.onInitializationApiAvailable()
