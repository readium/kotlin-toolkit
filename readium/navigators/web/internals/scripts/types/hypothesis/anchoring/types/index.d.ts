/**
 * Represents a region of a document as a (start, end) pair of `TextPosition` points.
 *
 * Representing a range in this way allows for changes in the DOM content of the
 * range which don't affect its text content, without affecting the text content
 * of the range itself.
 */
export declare class TextRange {
  public start: TextPosition

  public end: TextPosition

  /**
   * Create a TextRange from a (DOM) Range
   */

  static fromRange(range: Range): TextRange
  /**
   * Create a new TextRange whose `start` and `end` are computed relative to
   * `element`. `element` must be an ancestor of both `start.element` and
   * `end.element`.
   */
  relativeTo(element: Element): TextRange
}

/**
 * Represents an offset within the text content of an element.
 *
 * This position can be resolved to a specific descendant node in the current
 * DOM subtree of the element using the `resolve` method.
 */
export declare class TextPosition {
  public element: Element

  public offset: number
}

export declare type TextQuoteAnchorContext = {
  prefix?: string
  suffix?: string
}

export declare class TextQuoteAnchor {
  constructor(root: Element, exact: string, context: TextQuoteAnchorContext)

  toRange(): Range
}
