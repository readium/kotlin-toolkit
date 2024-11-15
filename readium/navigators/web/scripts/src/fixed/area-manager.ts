import { TapEvent } from "../common/events"

export namespace AreaManager {
  export interface Listener {
    onTap(event: TapEvent): void
    onLinkActivated(href: string, outerHtml: string): void
  }
}
