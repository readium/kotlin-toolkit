import { TapEvent, DecorationActivatedEvent } from "./events"

export namespace AreaManager {
  export interface Listener {
    onTap(event: TapEvent): void
    onLinkActivated(href: string, outerHtml: string): void
    onDecorationActivated(event: DecorationActivatedEvent): void
    onLayout(): void
  }
}
