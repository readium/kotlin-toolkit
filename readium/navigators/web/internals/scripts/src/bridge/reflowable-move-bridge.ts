import { TextQuoteAnchor } from "../vendor/hypothesis/annotator/anchoring/types"
import { log } from "../util/log"

export class ReflowableMoveBridge {
  readonly document: HTMLDocument

  constructor(document: HTMLDocument) {
    this.document = document
  }

  getOffsetForLocation(location: string, vertical: boolean): number | null {
    const actualLocation = parseLocation(location)

    if (actualLocation.textAfter || actualLocation.textBefore) {
      return this.getOffsetForTextAnchor(
        actualLocation.textBefore ?? "",
        actualLocation.textAfter ?? "",
        vertical
      )
    }

    if (actualLocation.cssSelector) {
      return this.getOffsetForCssSelector(actualLocation.cssSelector, vertical)
    }

    if (actualLocation.htmlId) {
      return this.getOffsetForHtmlId(actualLocation.htmlId, vertical)
    }

    return null
  }

  private getOffsetForTextAnchor(
    textBefore: string,
    textAfter: string,
    vertical: boolean
  ): number | null {
    const root = this.document.body

    const anchor = new TextQuoteAnchor(root, "", {
      prefix: textBefore,
      suffix: textAfter,
    })

    try {
      const range = anchor.toRange()
      return this.getOffsetForRect(range.getBoundingClientRect(), vertical)
    } catch (e) {
      log(e)
      return null
    }
  }

  private getOffsetForCssSelector(
    cssSelector: string,
    vertical: boolean
  ): number | null {
    let element
    try {
      element = this.document.querySelector(cssSelector)
    } catch (e) {
      log(e)
    }

    if (!element) {
      return null
    }

    return this.getOffsetForElement(element, vertical)
  }

  private getOffsetForHtmlId(htmlId: string, vertical: boolean): number | null {
    const element = this.document.getElementById(htmlId)
    if (!element) {
      return null
    }

    return this.getOffsetForElement(element, vertical)
  }

  private getOffsetForElement(element: Element, vertical: boolean): number {
    const rect = element.getBoundingClientRect()
    return this.getOffsetForRect(rect, vertical)
  }

  private getOffsetForRect(rect: DOMRect, vertical: boolean): number {
    if (vertical) {
      return rect.top + window.scrollY
    } else {
      const offset = rect.left + window.scrollX
      return offset
    }
  }
}

interface Location {
  progression: number
  htmlId: string
  cssSelector: string
  textBefore: string
  textAfter: string
}

function parseLocation(location: string): Location {
  const jsonLocation: Location = JSON.parse(location)
  return jsonLocation
}
