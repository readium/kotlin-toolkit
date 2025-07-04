//
//  Copyright 2024 Readium Foundation. All rights reserved.
//  Use of this source code is governed by the BSD-style license
//  available in the top-level LICENSE file of the project.
//

/**
 *  Script loaded by the single area HTML wrapper for fixed layout resources.
 */

import { DocumentBridge, GesturesBridge } from "./bridge/all-listener-bridge"
import { FixedSingleBridge as FixedSingleAreaBridge } from "./bridge/fixed-area-bridge"
import { FixedSingleInitializerParentSide } from "./fixed/comm-initialization"
import { FixedSingleDecorationsBridge } from "./bridge/all-decoration-bridge"
import { FixedSingleSelectionBridge } from "./bridge/all-selection-bridge"

declare global {
  interface Window {
    // Web APIs available for native code
    singleArea: FixedSingleAreaBridge
    singleSelection: FixedSingleSelectionBridge
    singleDecorations: FixedSingleDecorationsBridge
    // Native APIs available for web code
    documentState: DocumentBridge
    gestures: GesturesBridge
    singleSelectionListener: FixedSingleSelectionBridge.Listener
  }
}

const iframe = document.getElementById("page") as HTMLIFrameElement

const metaViewport = document.querySelector(
  "meta[name=viewport]"
) as HTMLMetaElement

window.singleArea = new FixedSingleAreaBridge(
  window,
  iframe,
  metaViewport,
  window.gestures,
  window.documentState
)

window.singleSelection = new FixedSingleSelectionBridge(
  window.singleSelectionListener
)

window.singleDecorations = new FixedSingleDecorationsBridge()

new FixedSingleInitializerParentSide(
  window,
  iframe,
  window.singleArea,
  window.singleSelection,
  window.singleDecorations
)

window.documentState.onScriptsLoaded()
