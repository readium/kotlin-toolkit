import { log } from "../util/log"
import {
  getClientRectsNoOverlap,
  dezoomDomRect,
  dezoomRect,
  rectContainsPoint,
  domRectToRect,
} from "../util/rect"
import { TextQuoteAnchor } from "../vendor/hypothesis/annotator/anchoring/types"
import { Rect } from "./geometry"

export interface DecorationActivatedEvent {
  id: string
  group: string
  rect: Rect
  event: MouseEvent
}

export interface DecorationTemplate {
  layout: DecorationLayout
  width: DecorationWidth
  stylesheet: string
}

export type DecorationWidth = "wrap" | "page" | "viewport" | "bounds"

export type DecorationLayout = "bounds" | "boxes"

export interface Decoration {
  id: string
  style: string
  element: string
  cssSelector?: string
  textQuote?: TextQuote
}

export interface TextQuote {
  quotedText: string
  textBefore: string
  textAfter: string
}

export class DecorationManager {
  private readonly window: Window

  private readonly styles = new Map()

  private readonly groups: Map<string, DecorationGroup> = new Map()

  private lastGroupId = 0

  constructor(window: Window) {
    this.window = window

    // Relayout all the decorations when the document body is resized.
    window.addEventListener(
      "load",
      () => {
        const body = window.document.body
        let lastSize = { width: 0, height: 0 }
        const observer = new ResizeObserver(() => {
          requestAnimationFrame(() => {
            if (
              lastSize.width === body.clientWidth &&
              lastSize.height === body.clientHeight
            ) {
              return
            }

            lastSize = {
              width: body.clientWidth,
              height: body.clientHeight,
            }

            this.relayoutDecorations()
          })
        })
        observer.observe(body)
      },
      false
    )
  }

  registerTemplates(templates: Map<string, DecorationTemplate>) {
    let stylesheet = ""

    for (const [id, template] of templates) {
      this.styles.set(id, template)
      if (template.stylesheet) {
        stylesheet += template.stylesheet + "\n"
      }
    }

    if (stylesheet) {
      const styleElement = document.createElement("style")
      styleElement.innerHTML = stylesheet
      document.getElementsByTagName("head")[0].appendChild(styleElement)
    }
  }

  addDecoration(decoration: Decoration, groupName: string) {
    console.log(`addDecoration ${decoration.id} ${groupName}`)
    const group = this.getGroup(groupName)
    group.add(decoration)
  }

  removeDecoration(id: string, groupName: string) {
    console.log(`removeDecoration ${id} ${groupName}`)
    const group = this.getGroup(groupName)
    group.remove(id)
  }

  private relayoutDecorations() {
    console.log("relayoutDecorations")
    for (const group of this.groups.values()) {
      group.relayout()
    }
  }

  private getGroup(name: string): DecorationGroup {
    let group = this.groups.get(name)
    if (!group) {
      const id = "readium-decoration-" + this.lastGroupId++
      group = new DecorationGroup(id, name, this.styles)
      this.groups.set(name, group)
    }
    return group
  }

  /**
   * Handles click events on a Decoration.
   * Returns whether a decoration matched this event.
   */
  handleDecorationClickEvent(
    event: MouseEvent
  ): DecorationActivatedEvent | null {
    if (this.groups.size === 0) {
      return null
    }

    const findTarget = () => {
      for (const [group, groupContent] of this.groups) {
        for (const item of groupContent.items.reverse()) {
          if (!item.clickableElements) {
            continue
          }
          for (const element of item.clickableElements) {
            const rect = domRectToRect(element.getBoundingClientRect())
            if (rectContainsPoint(rect, event.clientX, event.clientY, 1)) {
              return { group, item, element }
            }
          }
        }
      }
    }

    const target = findTarget()
    if (!target) {
      return null
    }

    return {
      id: target.item.decoration.id,
      group: target.group,
      rect: domRectToRect(target.item.range.getBoundingClientRect()),
      event: event,
    }
  }
}

class DecorationGroup {
  readonly items: Array<DecorationItem> = []

  readonly groupId: string

  readonly groupName: string

  readonly styles: Map<string, DecorationTemplate>

  lastItemId = 0

  container: HTMLDivElement | null = null

  constructor(id: string, name: string, styles: Map<string, any>) {
    this.groupId = id
    this.groupName = name
    this.styles = styles
  }

  add(decoration: Decoration) {
    const id = this.groupId + "-" + this.lastItemId++

    const range = rangeFromDecorationTarget(
      decoration.cssSelector,
      decoration.textQuote
    )
    if (!range) {
      log("Can't locate DOM range for decoration", decoration)
      return
    }

    const item = {
      id,
      decoration,
      range,
      container: null,
      clickableElements: null,
    }
    this.items.push(item)
    this.layout(item)
  }

  remove(id: string) {
    const index = this.items.findIndex((it) => it.decoration.id === id)
    if (index === -1) {
      return
    }

    const item = this.items[index]
    this.items.splice(index, 1)
    item.clickableElements = null
    if (item.container) {
      item.container.remove()
      item.container = null
    }
  }

  relayout() {
    this.clearContainer()
    for (const item of this.items) {
      this.layout(item)
    }
  }

  /**
   * Returns the group container element, after making sure it exists.
   */
  private requireContainer(): HTMLDivElement {
    if (!this.container) {
      this.container = document.createElement("div")
      this.container.id = this.groupId
      this.container.dataset.group = this.groupName
      this.container.style.pointerEvents = "none"
      document.body.append(this.container)
    }
    return this.container
  }

  /**
   * Removes the group container.
   */
  private clearContainer() {
    if (this.container) {
      this.container.remove()
      this.container = null
    }
  }

  /**
   * Layouts a single Decoration item.
   */
  private layout(item: DecorationItem) {
    const groupContainer = this.requireContainer()

    const unsafeStyle = this.styles.get(item.decoration.style)
    if (!unsafeStyle) {
      console.log(`Unknown decoration style: ${item.decoration.style}`)
      return
    }

    const style: DecorationTemplate = unsafeStyle!

    const itemContainer = document.createElement("div")
    itemContainer.id = item.id
    itemContainer.dataset.style = item.decoration.style
    itemContainer.style.pointerEvents = "none"

    const documentWritingMode = getDocumentWritingMode()
    const isVertical =
      documentWritingMode === "vertical-rl" ||
      documentWritingMode === "vertical-lr"

    const zoom = groupContainer.currentCSSZoom
    const scrollingElement = document.scrollingElement!
    const xOffset = scrollingElement.scrollLeft / zoom
    const yOffset = scrollingElement.scrollTop / zoom

    const viewportWidth = isVertical ? window.innerHeight : window.innerWidth
    const viewportHeight = isVertical ? window.innerWidth : window.innerHeight

    const columnCount =
      parseInt(
        getComputedStyle(document.documentElement).getPropertyValue(
          "column-count"
        )
      ) || 1
    const pageSize = (isVertical ? viewportHeight : viewportWidth) / columnCount

    function positionElement(
      element: HTMLElement,
      rect: Rect,
      boundingRect: DOMRect,
      writingMode: string
    ) {
      element.style.position = "absolute"
      const isVerticalRL = writingMode === "vertical-rl"
      const isVerticalLR = writingMode === "vertical-lr"

      if (isVerticalRL || isVerticalLR) {
        if (style.width === "wrap") {
          element.style.width = `${rect.width}px`
          element.style.height = `${rect.height}px`
          if (isVerticalRL) {
            element.style.right = `${
              -rect.right - xOffset + scrollingElement.clientWidth
            }px`
          } else {
            // vertical-lr
            element.style.left = `${rect.left + xOffset}px`
          }
          element.style.top = `${rect.top + yOffset}px`
        } else if (style.width === "viewport") {
          element.style.width = `${rect.height}px`
          element.style.height = `${viewportWidth}px`
          const top = Math.floor(rect.top / viewportWidth) * viewportWidth
          if (isVerticalRL) {
            element.style.right = `${-rect.right - xOffset}px`
          } else {
            // vertical-lr
            element.style.left = `${rect.left + xOffset}px`
          }
          element.style.top = `${top + yOffset}px`
        } else if (style.width === "bounds") {
          element.style.width = `${boundingRect.height}px`
          element.style.height = `${viewportWidth}px`
          if (isVerticalRL) {
            element.style.right = `${
              -boundingRect.right - xOffset + scrollingElement.clientWidth
            }px`
          } else {
            // vertical-lr
            element.style.left = `${boundingRect.left + xOffset}px`
          }
          element.style.top = `${boundingRect.top + yOffset}px`
        } else if (style.width === "page") {
          element.style.width = `${rect.height}px`
          element.style.height = `${pageSize}px`
          const top = Math.floor(rect.top / pageSize) * pageSize
          if (isVerticalRL) {
            element.style.right = `${
              -rect.right - xOffset + scrollingElement.clientWidth
            }px`
          } else {
            // vertical-lr
            element.style.left = `${rect.left + xOffset}px`
          }
          element.style.top = `${top + yOffset}px`
        }
      } else {
        if (style.width === "wrap") {
          element.style.width = `${rect.width}px`
          element.style.height = `${rect.height}px`
          element.style.left = `${rect.left + xOffset}px`
          element.style.top = `${rect.top + yOffset}px`
        } else if (style.width === "viewport") {
          element.style.width = `${viewportWidth}px`
          element.style.height = `${rect.height}px`
          const left = Math.floor(rect.left / viewportWidth) * viewportWidth
          element.style.left = `${left + xOffset}px`
          element.style.top = `${rect.top + yOffset}px`
        } else if (style.width === "bounds") {
          element.style.width = `${boundingRect.width}px`
          element.style.height = `${rect.height}px`
          element.style.left = `${boundingRect.left + xOffset}px`
          element.style.top = `${rect.top + yOffset}px`
        } else if (style.width === "page") {
          element.style.width = `${pageSize}px`
          element.style.height = `${rect.height}px`
          const left = Math.floor(rect.left / pageSize) * pageSize
          element.style.left = `${left + xOffset}px`
          element.style.top = `${rect.top + yOffset}px`
        }
      }
    }

    const rawBoundingRect = item.range.getBoundingClientRect()
    const boundingRect = dezoomDomRect(rawBoundingRect, zoom)

    let elementTemplate
    try {
      const template = document.createElement("template")
      template.innerHTML = item.decoration.element.trim()
      elementTemplate = template.content.firstElementChild as HTMLElement | null
    } catch (error: any) {
      let message: string | null
      if ("message" in error) {
        message = error.message
      } else {
        message = null
      }
      console.log(
        `Invalid decoration element "${item.decoration.element}": ${message}`
      )
      return
    }

    if (style.layout === "boxes") {
      const doNotMergeHorizontallyAlignedRects =
        !documentWritingMode.startsWith("vertical")
      const startElement = getContainingElement(
        item.range.startContainer
      ) as Element
      // Decorated text may have a different writingMode from document body
      const decoratorWritingMode = getComputedStyle(startElement).writingMode

      const clientRects = getClientRectsNoOverlap(
        item.range,
        doNotMergeHorizontallyAlignedRects
      )
        .map((rect: Rect) => {
          return dezoomRect(rect, zoom)
        })
        .sort((r1, r2) => {
          if (r1.top !== r2.top) return r1.top - r2.top
          if (decoratorWritingMode === "vertical-rl") {
            return r2.left - r1.left
          } else if (decoratorWritingMode === "vertical-lr") {
            return r1.left - r2.left
          } else {
            return r1.left - r2.left
          }
        })

      for (const clientRect of clientRects) {
        const line = elementTemplate!.cloneNode(true) as HTMLElement
        line.style.pointerEvents = "none"
        line.dataset.writingMode = decoratorWritingMode
        positionElement(line, clientRect, boundingRect, documentWritingMode)
        itemContainer.append(line)
      }
    } else if (style.layout === "bounds") {
      const bounds = elementTemplate!.cloneNode(true) as HTMLElement
      bounds.style.pointerEvents = "none"
      bounds.dataset.writingMode = documentWritingMode
      positionElement(bounds, boundingRect, boundingRect, documentWritingMode)

      itemContainer.append(bounds)
    }

    groupContainer.append(itemContainer)
    item.container = itemContainer
    item.clickableElements = Array.from(
      itemContainer.querySelectorAll("[data-activable='1']")
    )
    if (item.clickableElements.length === 0) {
      item.clickableElements = Array.from(itemContainer.children)
    }
  }
}

interface DecorationItem {
  id: string
  decoration: Decoration
  range: Range
  container: HTMLDivElement | null
  clickableElements: Array<Element> | null
}

/**
 * Returns the document body's writing mode.
 */
function getDocumentWritingMode(): string {
  return getComputedStyle(document.body).writingMode
}

/**
 * Returns the closest element ancestor of the given node.
 */
function getContainingElement(node: Node) {
  return node.nodeType === Node.ELEMENT_NODE ? node : node.parentElement
}

/*
 * Compute DOM range from decoration target.
 */

export function rangeFromDecorationTarget(
  cssSelector?: string,
  textQuote?: TextQuote
): Range | null {
  let root
  if (cssSelector) {
    try {
      root = document.querySelector(cssSelector)
    } catch (e) {
      log(e)
    }
  }

  if (!root && !textQuote) {
    return null
  } else if (!root) {
    root = document.body
  }

  if (textQuote) {
    const anchor = new TextQuoteAnchor(root, textQuote.quotedText, {
      prefix: textQuote.textBefore,
      suffix: textQuote.textAfter,
    })

    return anchor.toRange()
  } else {
    const range = document.createRange()
    range.setStartBefore(root)
    range.setEndAfter(root)
    return range
  }
}
