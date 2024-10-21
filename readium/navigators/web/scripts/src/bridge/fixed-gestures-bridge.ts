import { AreaManager } from "../fixed/area-manager"

export interface GesturesBridge {
  onTap(event: string): void
}

export interface TapEvent {
  x: number
  y: number
}

export class BridgeGesturesAdapter implements AreaManager.Listener {
  readonly nativeApi: GesturesBridge

  constructor(gesturesApi: GesturesBridge) {
    this.nativeApi = gesturesApi
  }

  onTap(event: TapEvent): void {
    this.nativeApi.onTap(JSON.stringify(event))
  }
}
