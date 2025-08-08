import { Offset, Rect } from "../common/geometry"

export interface TapEvent {
  offset: Offset
}

export interface DecorationActivatedEvent {
  id: string
  group: string
  rect: Rect
  offset: Offset
}
