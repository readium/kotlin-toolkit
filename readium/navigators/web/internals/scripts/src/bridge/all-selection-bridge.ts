import { SelectionManager, Selection } from "../common/selection"

export class SelectionBridge {
  readonly window: Window

  readonly manager: SelectionManager

  constructor(window: Window, manager: SelectionManager) {
    this.window = window
    this.manager = manager
  }

  getCurrentSelection(): Selection | null {
    return this.manager.getCurrentSelection()
  }
}
