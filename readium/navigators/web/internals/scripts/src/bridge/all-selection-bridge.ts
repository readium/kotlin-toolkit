import {
  SelectionManager,
  Selection,
  selectionToParentCoordinates,
} from "../common/selection"
import { SelectionWrapperParentSide } from "../fixed/selection-wrapper"

export class ReflowableSelectionBridge {
  readonly window: Window

  readonly manager: SelectionManager

  constructor(window: Window, manager: SelectionManager) {
    this.window = window
    this.manager = manager
  }

  getCurrentSelection(): Selection | null {
    return this.manager.getCurrentSelection()
  }

  clearSelection() {
    this.manager.clearSelection()
  }
}

export class FixedSingleSelectionBridge {
  private readonly wrapper: SelectionWrapperParentSide

  private readonly listener: FixedSingleSelectionBridge.Listener

  constructor(listener: FixedSingleSelectionBridge.Listener) {
    this.listener = listener
    const wrapperListener = {
      onSelectionAvailable: (
        requestId: string,
        selection: Selection | null
      ) => {
        const selectionAsJson = JSON.stringify(selection)
        this.listener.onSelectionAvailable(requestId, selectionAsJson)
      },
    }
    this.wrapper = new SelectionWrapperParentSide(wrapperListener)
  }

  setMessagePort(messagePort: MessagePort) {
    this.wrapper.setMessagePort(messagePort)
  }

  requestSelection(requestId: string) {
    this.wrapper.requestSelection(requestId)
  }

  clearSelection() {
    this.wrapper.clearSelection()
  }
}

export namespace FixedSingleSelectionBridge {
  export interface Listener {
    onSelectionAvailable(requestId: string, selection: string): void
  }
}

export class FixedDoubleSelectionBridge {
  private readonly leftIframe: HTMLIFrameElement

  private readonly rightIframe: HTMLIFrameElement

  private readonly leftWrapper: SelectionWrapperParentSide

  private readonly rightWrapper: SelectionWrapperParentSide

  private readonly listener: FixedDoubleSelectionBridge.Listener

  private readonly requestStates: Map<string, RequestState> = new Map()

  private isLeftInitialized: boolean = false

  private isRightInitialized: boolean = false

  constructor(
    leftIframe: HTMLIFrameElement,
    rightIframe: HTMLIFrameElement,
    listener: FixedDoubleSelectionBridge.Listener
  ) {
    this.leftIframe = leftIframe
    this.rightIframe = rightIframe
    this.listener = listener

    const leftWrapperListener = {
      onSelectionAvailable: (
        requestId: string,
        selection: Selection | null
      ) => {
        if (selection) {
          const resolvedSelection = selectionToParentCoordinates(
            selection,
            this.leftIframe
          )
          this.onSelectionAvailable(requestId, "left", resolvedSelection)
        } else {
          this.onSelectionAvailable(requestId, "left", selection)
        }
      },
    }
    this.leftWrapper = new SelectionWrapperParentSide(leftWrapperListener)

    const rightWrapperListener = {
      onSelectionAvailable: (
        requestId: string,
        selection: Selection | null
      ) => {
        if (selection) {
          const resolvedSelection = selectionToParentCoordinates(
            selection,
            this.rightIframe
          )
          this.onSelectionAvailable(requestId, "right", resolvedSelection)
        } else {
          this.onSelectionAvailable(requestId, "right", selection)
        }
      },
    }
    this.rightWrapper = new SelectionWrapperParentSide(rightWrapperListener)
  }

  setLeftMessagePort(messagePort: MessagePort) {
    this.leftWrapper.setMessagePort(messagePort)
    this.isLeftInitialized = true
  }

  setRightMessagePort(messagePort: MessagePort) {
    this.rightWrapper.setMessagePort(messagePort)
    this.isRightInitialized = true
  }

  requestSelection(requestId: string) {
    if (this.isLeftInitialized && this.isRightInitialized) {
      this.requestStates.set(requestId, "pending")
      this.leftWrapper.requestSelection(requestId)
      this.rightWrapper.requestSelection(requestId)
    } else if (this.isLeftInitialized) {
      this.requestStates.set(requestId, "firstResponseWasNull")
      this.leftWrapper.requestSelection(requestId)
    } else if (this.isRightInitialized) {
      this.requestStates.set(requestId, "firstResponseWasNull")
      this.rightWrapper.requestSelection(requestId)
    } else {
      this.requestStates.set(requestId, "firstResponseWasNull")
      this.onSelectionAvailable(requestId, "left", null)
    }
  }

  clearSelection() {
    this.leftWrapper.clearSelection()
    this.rightWrapper.clearSelection()
  }

  onSelectionAvailable(
    requestId: string,
    iframe: FixedDoubleSelectionBridge.Iframe,
    selection: Selection | null
  ) {
    const requestState = this.requestStates.get(requestId)
    if (!requestState) {
      return
    }

    if (!selection && requestState === "pending") {
      this.requestStates.set(requestId, "firstResponseWasNull")
      return
    }

    this.requestStates.delete(requestId)
    const selectionAsJson = JSON.stringify(selection)
    this.listener.onSelectionAvailable(requestId, iframe, selectionAsJson)
  }
}

type RequestState = "pending" | "firstResponseWasNull"

export namespace FixedDoubleSelectionBridge {
  export type Iframe = "left" | "right"

  export interface Listener {
    onSelectionAvailable(
      requestId: string,
      iframe: string,
      selection: string
    ): void
  }
}
