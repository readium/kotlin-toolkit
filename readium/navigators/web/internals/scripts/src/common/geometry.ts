export interface Size {
  width: number
  height: number
}

export interface Margins {
  top: number
  left: number
  bottom: number
  right: number
}

export interface Insets {
  top: number
  left: number
  bottom: number
  right: number
}

export interface Rect {
  bottom: number
  height: number
  left: number
  right: number
  top: number
  width: number
}

export interface Offset {
  x: number
  y: number
}

export function offsetToParentCoordinates(offset: Offset, iframeRect: DOMRect) {
  return {
    x:
      (offset.x + iframeRect.left - visualViewport!.offsetLeft) *
      visualViewport!.scale,
    y:
      (offset.y + iframeRect.top - visualViewport!.offsetTop) *
      visualViewport!.scale,
  }
}

export function rectToParentCoordinates(rect: Rect, iframeRect: DOMRect) {
  const topLeft = { x: rect.left, y: rect.top }
  const bottomRight = { x: rect.right, y: rect.bottom }
  const shiftedTopLeft = offsetToParentCoordinates(topLeft, iframeRect)
  const shiftedBottomRight = offsetToParentCoordinates(bottomRight, iframeRect)
  return {
    left: shiftedTopLeft.x,
    top: shiftedTopLeft.y,
    right: shiftedBottomRight.x,
    bottom: shiftedBottomRight.y,
    width: shiftedBottomRight.x - shiftedTopLeft.x,
    height: shiftedBottomRight.y - shiftedTopLeft.y,
  }
}
