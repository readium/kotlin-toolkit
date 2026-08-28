import { SelectionManager, Selection } from "../common/selection"

export namespace SelectionWrapperParentSide {
  export interface Listener {
    onSelectionAvailable(requestId: string, selection: Selection | null): void
  }
}

export class SelectionWrapperParentSide {
  private readonly selectionListener: SelectionWrapperParentSide.Listener

  private messagePort?: MessagePort

  constructor(listener: SelectionWrapperParentSide.Listener) {
    this.selectionListener = listener
  }

  setMessagePort(messagePort: MessagePort) {
    this.messagePort = messagePort

    messagePort.onmessage = (message) => {
      this.onFeedback(message.data as SelectionFeedback)
    }
  }

  requestSelection(requestId: string) {
    this.send({ kind: "requestSelection", requestId: requestId })
  }

  clearSelection() {
    this.send({ kind: "clearSelection" })
  }

  private onFeedback(feedback: SelectionFeedback) {
    switch (feedback.kind) {
      case "selectionAvailable":
        return this.onSelectionAvailable(feedback.requestId, feedback.selection)
    }
  }
  private onSelectionAvailable(requestId: string, selection: Selection | null) {
    this.selectionListener.onSelectionAvailable(requestId, selection)
  }

  private send(message: SelectionCommand) {
    this.messagePort?.postMessage(message)
  }
}

export class SelectionWrapperIframeSide {
  private readonly selectionManager: SelectionManager

  private readonly messagePort: MessagePort

  constructor(messagePort: MessagePort, manager: SelectionManager) {
    this.selectionManager = manager
    this.messagePort = messagePort

    messagePort.onmessage = (message) => {
      this.onCommand(message.data as SelectionCommand)
    }
  }

  onCommand(command: SelectionCommand) {
    switch (command.kind) {
      case "requestSelection":
        return this.onRequestSelection(command.requestId)
      case "clearSelection":
        return this.onClearSelection()
    }
  }

  private onRequestSelection(requestId: string) {
    const selection = this.selectionManager.getCurrentSelection()
    const feedback: SelectionFeedback = {
      kind: "selectionAvailable",
      requestId: requestId,
      selection: selection,
    }
    this.sendFeedback(feedback)
  }

  private onClearSelection() {
    this.selectionManager.clearSelection()
  }

  private sendFeedback(message: SelectionFeedback) {
    this.messagePort.postMessage(message)
  }
}

interface RequestSelection {
  kind: "requestSelection"
  requestId: string
}

interface ClearSelection {
  kind: "clearSelection"
}

type SelectionCommand = RequestSelection | ClearSelection

interface SelectionAvailable {
  kind: "selectionAvailable"
  requestId: string
  selection: Selection | null
}

type SelectionFeedback = SelectionAvailable
