import { Offset } from "../util/offset"
import { Rect } from "../util/rect"

export interface TapEvent {
  offset: Offset
}

export interface DecorationActivatedEvent {
  id: string
  group: string
  rect: Rect
  offset: Offset
}
