import {
  Decoration,
  DecorationManager,
  DecorationTemplate,
} from "../common/decoration"
import { DecorationWrapperParentSide } from "../fixed/decoration-wrapper"

export class ReflowableDecorationsBridge {
  readonly window: Window

  readonly manager: DecorationManager

  constructor(window: Window, manager: DecorationManager) {
    this.window = window
    this.manager = manager
  }

  registerTemplates(templates: string) {
    const templatesAsMap = parseTemplates(templates)
    this.manager.registerTemplates(templatesAsMap)
  }

  addDecoration(decoration: string, group: string) {
    const actualDecoration = parseDecoration(decoration)
    this.manager.addDecoration(actualDecoration, group)
  }

  removeDecoration(id: string, group: string) {
    this.manager.removeDecoration(id, group)
  }
}

export class FixedSingleDecorationsBridge {
  private readonly wrapper: DecorationWrapperParentSide

  constructor() {
    this.wrapper = new DecorationWrapperParentSide()
  }

  setMessagePort(messagePort: MessagePort) {
    this.wrapper.setMessagePort(messagePort)
  }

  registerTemplates(templates: string) {
    const actualTemplates = parseTemplates(templates)
    this.wrapper.registerTemplates(actualTemplates)
  }

  addDecoration(decoration: string, group: string) {
    const actualDecoration = parseDecoration(decoration)
    this.wrapper.addDecoration(actualDecoration, group)
  }

  removeDecoration(id: string, group: string) {
    this.wrapper.removeDecoration(id, group)
  }
}

export class FixedDoubleDecorationsBridge {
  private readonly leftWrapper: DecorationWrapperParentSide

  private readonly rightWrapper: DecorationWrapperParentSide

  constructor() {
    this.leftWrapper = new DecorationWrapperParentSide()
    this.rightWrapper = new DecorationWrapperParentSide()
  }

  setLeftMessagePort(messagePort: MessagePort) {
    this.leftWrapper.setMessagePort(messagePort)
  }

  setRightMessagePort(messagePort: MessagePort) {
    this.rightWrapper.setMessagePort(messagePort)
  }

  registerTemplates(templates: string) {
    const actualTemplates = parseTemplates(templates)
    this.leftWrapper.registerTemplates(actualTemplates)
    this.rightWrapper.registerTemplates(actualTemplates)
  }

  addDecoration(decoration: string, iframe: string, group: string) {
    const actualDecoration = parseDecoration(decoration)
    switch (iframe) {
      case "left":
        this.leftWrapper.addDecoration(actualDecoration, group)
        break
      case "right":
        this.rightWrapper.addDecoration(actualDecoration, group)
        break
      default:
        throw Error(`Unknown iframe type: ${iframe}`)
    }
  }

  removeDecoration(id: string, group: string) {
    this.leftWrapper.removeDecoration(id, group)
    this.rightWrapper.removeDecoration(id, group)
  }
}

function parseTemplates(templates: string): Map<string, DecorationTemplate> {
  return new Map<string, DecorationTemplate>(
    Object.entries(JSON.parse(templates))
  )
}

function parseDecoration(decoration: string): Decoration {
  const jsonDecoration: Decoration = JSON.parse(decoration)
  return jsonDecoration
}
