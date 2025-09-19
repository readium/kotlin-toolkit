//
//  Copyright 2021 Readium Foundation. All rights reserved.
//  Use of this source code is governed by the BSD-style license
//  available in the top-level LICENSE file of the project.
//

import { dezoomRect, domRectToRect } from "../util/rect"
import { log } from "../util/log"
import { TextRange } from "../vendor/hypothesis/annotator/anchoring/text-range"

// Polyfill for Android API 26
import matchAll from "string.prototype.matchall"
import { Rect } from "./geometry"
import { rectToParentCoordinates } from "./geometry"
matchAll.shim()

export interface SelectionListener {
  onSelectionStart(): void
  onSelectionEnd(): void
}

export interface Selection {
  selectedText: string
  textBefore: string
  textAfter: string
  selectionRect: Rect
}

export function selectionToParentCoordinates(
  selection: Selection,
  iframe: HTMLIFrameElement
): Selection {
  const boundingRect = iframe.getBoundingClientRect()
  const shiftedRect = rectToParentCoordinates(
    selection!.selectionRect,
    boundingRect
  )
  return {
    selectedText: selection?.selectedText,
    selectionRect: shiftedRect,
    textBefore: selection.textBefore,
    textAfter: selection.textAfter,
  }
}

export class SelectionManager {
  private readonly window: Window

  isSelecting = false

  constructor(window: Window) {
    //, listener: SelectionListener) {
    this.window = window
    /*this.listener = listener
    document.addEventListener(
      "selectionchange",
      () => {
        const selection = window.getSelection()!
        const collapsed = selection.isCollapsed

        if (collapsed && this.isSelecting) {
          this.isSelecting = false
          this.listener.onSelectionEnd()
        } else if (!collapsed && !this.isSelecting) {
          this.isSelecting = true
          this.listener.onSelectionStart()
        }
      },
      false
    )*/
  }

  clearSelection() {
    this.window.getSelection()?.removeAllRanges()
  }

  getCurrentSelection(): Selection | null {
    const text = this.getCurrentSelectionText()
    if (!text) {
      return null
    }
    const rect = this.getSelectionRect()
    return {
      selectedText: text.highlight,
      textBefore: text.before,
      textAfter: text.after,
      selectionRect: rect,
    }
  }

  private getSelectionRect(): Rect {
    try {
      const selection = this.window.getSelection()!
      const range = selection.getRangeAt(0)
      const zoom = this.window.document.body.currentCSSZoom
      return dezoomRect(domRectToRect(range.getBoundingClientRect()), zoom)
    } catch (e) {
      log(e)
      throw e
      //return null
    }
  }

  private getCurrentSelectionText() {
    const selection = this.window.getSelection()!

    if (selection.isCollapsed) {
      return undefined
    }
    const highlight = selection.toString()
    const cleanHighlight = highlight
      .trim()
      .replace(/\n/g, " ")
      .replace(/\s\s+/g, " ")
    if (cleanHighlight.length === 0) {
      return undefined
    }
    if (!selection.anchorNode || !selection.focusNode) {
      return undefined
    }
    const range =
      selection.rangeCount === 1
        ? selection.getRangeAt(0)
        : createOrderedRange(
            selection.anchorNode,
            selection.anchorOffset,
            selection.focusNode,
            selection.focusOffset
          )
    if (!range || range.collapsed) {
      log("$$$$$$$$$$$$$$$$$ CANNOT GET NON-COLLAPSED SELECTION RANGE?!")
      return undefined
    }

    const text = document.body.textContent!
    const textRange = TextRange.fromRange(range).relativeTo(document.body)
    const start = textRange.start.offset
    const end = textRange.end.offset

    const snippetLength = 200

    // Compute the text before the highlight, ignoring the first "word", which might be cut.
    let before = text.slice(Math.max(0, start - snippetLength), start)
    const firstWordStart = before.search(/\P{L}\p{L}/gu)
    if (firstWordStart !== -1) {
      before = before.slice(firstWordStart + 1)
    }

    // Compute the text after the highlight, ignoring the last "word", which might be cut.
    let after = text.slice(end, Math.min(text.length, end + snippetLength))
    const lastWordEnd = Array.from(after.matchAll(/\p{L}\P{L}/gu)).pop()
    if (lastWordEnd !== undefined && lastWordEnd.index > 1) {
      after = after.slice(0, lastWordEnd.index + 1)
    }

    return { highlight, before, after }
  }
}

function createOrderedRange(
  startNode: Node,
  startOffset: number,
  endNode: Node,
  endOffset: number
) {
  const range = new Range()
  range.setStart(startNode, startOffset)
  range.setEnd(endNode, endOffset)
  if (!range.collapsed) {
    return range
  }
  log(">>> createOrderedRange COLLAPSED ... RANGE REVERSE?")
  const rangeReverse = new Range()
  rangeReverse.setStart(endNode, endOffset)
  rangeReverse.setEnd(startNode, startOffset)
  if (!rangeReverse.collapsed) {
    log(">>> createOrderedRange RANGE REVERSE OK.")
    return range
  }
  log(">>> createOrderedRange RANGE REVERSE ALSO COLLAPSED?!")
  return undefined
}

/*
export function convertRangeInfo(document: Document, rangeInfo) {
  const startElement = document.querySelector(
    rangeInfo.startContainerElementCssSelector
  );
  if (!startElement) {
    log("^^^ convertRangeInfo NO START ELEMENT CSS SELECTOR?!");
    return undefined;
  }
  let startContainer = startElement;
  if (rangeInfo.startContainerChildTextNodeIndex >= 0) {
    if (
      rangeInfo.startContainerChildTextNodeIndex >=
      startElement.childNodes.length
    ) {
      log(
        "^^^ convertRangeInfo rangeInfo.startContainerChildTextNodeIndex >= startElement.childNodes.length?!"
      );
      return undefined;
    }
    startContainer =
      startElement.childNodes[rangeInfo.startContainerChildTextNodeIndex];
    if (startContainer.nodeType !== Node.TEXT_NODE) {
      log("^^^ convertRangeInfo startContainer.nodeType !== Node.TEXT_NODE?!");
      return undefined;
    }
  }
  const endElement = document.querySelector(
    rangeInfo.endContainerElementCssSelector
  );
  if (!endElement) {
    log("^^^ convertRangeInfo NO END ELEMENT CSS SELECTOR?!");
    return undefined;
  }
  let endContainer = endElement;
  if (rangeInfo.endContainerChildTextNodeIndex >= 0) {
    if (
      rangeInfo.endContainerChildTextNodeIndex >= endElement.childNodes.length
    ) {
      log(
        "^^^ convertRangeInfo rangeInfo.endContainerChildTextNodeIndex >= endElement.childNodes.length?!"
      );
      return undefined;
    }
    endContainer =
      endElement.childNodes[rangeInfo.endContainerChildTextNodeIndex];
    if (endContainer.nodeType !== Node.TEXT_NODE) {
      log("^^^ convertRangeInfo endContainer.nodeType !== Node.TEXT_NODE?!");
      return undefined;
    }
  }
  return createOrderedRange(
    startContainer,
    rangeInfo.startOffset,
    endContainer,
    rangeInfo.endOffset
  );
}

export function location2RangeInfo(location) {
  const locations = location.locations;
  const domRange = locations.domRange;
  const start = domRange.start;
  const end = domRange.end;

  return {
    endContainerChildTextNodeIndex: end.textNodeIndex,
    endContainerElementCssSelector: end.cssSelector,
    endOffset: end.offset,
    startContainerChildTextNodeIndex: start.textNodeIndex,
    startContainerElementCssSelector: start.cssSelector,
    startOffset: start.offset,
  };
}
*/
