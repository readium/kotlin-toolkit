import { Offset } from "../util/offset"
import { Rect } from "../util/rect"
import { TapEvent, DecorationActivatedEvent } from "./events"

export namespace AreaManager {
  export interface Listener {
    onTap(event: TapEvent): void
    onLinkActivated(href: string, outerHtml: string): void
    onDecorationActivated(event: DecorationActivatedEvent): void
    onLayout(): void
  }
}

export function shiftOffset(offset: Offset, iframeRect: DOMRect) {
  return {
    x:
      (offset.x + iframeRect.left - visualViewport!.offsetLeft) *
      visualViewport!.scale,
    y:
      (offset.y + iframeRect.top - visualViewport!.offsetTop) *
      visualViewport!.scale,
  }
}

export function shiftRect(rect: Rect, iframeRect: DOMRect) {
  const topLeft = { x: rect.left, y: rect.top }
  const bottomRight = { x: rect.right, y: rect.bottom }
  const shiftedTopLeft = shiftOffset(topLeft, iframeRect)
  const shiftedBottomRight = shiftOffset(bottomRight, iframeRect)
  return {
    left: shiftedTopLeft.x,
    top: shiftedTopLeft.y,
    right: shiftedBottomRight.x,
    bottom: shiftedBottomRight.y,
    width: shiftedBottomRight.x - shiftedTopLeft.x,
    height: shiftedBottomRight.y - shiftedTopLeft.y,
  }
}
