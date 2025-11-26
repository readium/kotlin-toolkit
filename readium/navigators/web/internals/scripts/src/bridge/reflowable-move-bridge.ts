export class ReflowableMoveBridge {
  readonly document: HTMLDocument

  constructor(document: HTMLDocument) {
    this.document = document
  }

  getOffsetForLocation(location: string, vertical: boolean): number | null {
    const actualLocation = parseLocation(location)

    if (actualLocation.htmlId) {
      return this.getOffsetForHtmlId(actualLocation.htmlId, vertical)
    }

    return null
  }

  private getOffsetForHtmlId(htmlId: string, vertical: boolean): number | null {
    const element = this.document.getElementById(htmlId)
    if (!element) {
      return null
    }

    const rect = element.getBoundingClientRect()

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
}

function parseLocation(location: string): Location {
  const jsonLocation: Location = JSON.parse(location)
  return jsonLocation
}
