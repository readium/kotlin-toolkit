import {
  DecorationManager,
  DecorationTarget,
  DecorationTemplate,
} from "../common/decoration"

export class DecorationsBridge {
  readonly window: Window

  readonly manager: DecorationManager

  constructor(window: Window, manager: DecorationManager) {
    this.window = window
    this.manager = manager
  }

  registerTemplates(templates: string) {
    console.log(`templates ${templates}`)

    const templatesAsMap = new Map<string, DecorationTemplate>(
      Object.entries(JSON.parse(templates))
    )
    this.manager.registerTemplates(templatesAsMap)
  }

  addDecoration(decoration: string, group: string) {
    const jsonDecoration: JsonDecoration = JSON.parse(decoration)
    console.log(`Decoration ${jsonDecoration}`)
    const targetedText = jsonDecoration.locator.text?.highlight
    let decorationTarget: DecorationTarget

    const locations = jsonDecoration.locator.locations!
    let cssSelector: string | undefined = undefined
    if (locations.cssSelector) {
      cssSelector = locations.cssSelector
    } else if (locations.fragments) {
      const fragment = locations.fragments![0]
      cssSelector = `#${fragment}`
    }

    if (targetedText) {
      decorationTarget = {
        type: "text",
        targetedText: targetedText,
        textBefore: jsonDecoration.locator.text!.before,
        textAfter: jsonDecoration.locator.text!.after,
        cssSelector: cssSelector,
      }
    } else {
      decorationTarget = {
        type: "element",
        cssSelector: cssSelector!,
      }
    }

    const actualDecoration = {
      id: jsonDecoration.id,
      style: jsonDecoration.style,
      target: decorationTarget,
      element: jsonDecoration.element,
    }
    this.manager.addDecoration(actualDecoration, group)
  }

  removeDecoration(id: string, group: string) {
    this.manager.removeDecoration(id, group)
  }
}

interface JsonDecoration {
  id: string
  locator: JsonLocator
  element: string
  style: string
}

interface JsonLocator {
  href: string
  type: string
  title?: string
  locations?: JsonLocations
  text?: JsonText
}

interface JsonLocations {
  fragments?: Array<string>
  progression: number
  totalProgression: number
  cssSelector: string
}

interface JsonText {
  before?: string
  highlight?: string
  after?: string
}
