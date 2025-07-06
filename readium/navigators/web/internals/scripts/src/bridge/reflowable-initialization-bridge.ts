import { DecorationManager } from "../common/decoration"
import { GesturesDetector } from "../common/gestures"
import { SelectionManager } from "../common/selection"
import { ReflowableDecorationsBridge } from "./all-decoration-bridge"
import { ReflowableListenerAdapter } from "./all-listener-bridge"
import { ReflowableSelectionBridge } from "./all-selection-bridge"
import { CssBridge } from "./reflowable-css-bridge"

export class ReflowableInitializationBridge {
  private readonly window: Window

  private readonly listener: ReflowableApiStateListener

  constructor(window: Window, listener: ReflowableApiStateListener) {
    this.window = window
    this.listener = listener

    this.setupViewport()
    this.initApis()
  }

  private initApis() {
    const bridgeListener = new ReflowableListenerAdapter(window.gestures)

    const decorationManager = new DecorationManager(window)

    this.window.readiumcss = new CssBridge(window.document)
    this.listener.onCssApiAvailable()

    this.window.selection = new ReflowableSelectionBridge(
      window,
      new SelectionManager(window)
    )
    this.listener.onSelectionApiAvailable()

    this.window.decorations = new ReflowableDecorationsBridge(
      window,
      decorationManager
    )
    this.listener.onDecorationApiAvailable()

    new GesturesDetector(window, bridgeListener, decorationManager)
  }

  // Setups the `viewport` meta tag to disable overview.
  private setupViewport() {
    this.window.document.addEventListener("DOMContentLoaded", () => {
      const meta = document.createElement("meta")
      meta.setAttribute("name", "viewport")
      meta.setAttribute(
        "content",
        "width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, shrink-to-fit=no"
      )
      this.window.document.head.appendChild(meta)
    })
  }
}

export interface ReflowableApiStateListener {
  onCssApiAvailable(): void
  onSelectionApiAvailable(): void
  onDecorationApiAvailable(): void
}
