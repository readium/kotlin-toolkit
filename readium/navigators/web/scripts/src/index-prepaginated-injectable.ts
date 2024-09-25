import { GesturesManager, GesturesListener } from "./common/gestures";

interface Listener {

	onTap(event: string): void

}

interface TapEvent {

	x: number

	y: number
}

declare global {

	interface Window {
		
		gestures: Listener
	}
}

class AdapterGesturesListener implements GesturesListener {

	readonly listener: Listener

	constructor(listener: Listener) {
		this.listener = listener
	}

	onTap(event: MouseEvent): void {
		const tapEvent: TapEvent = { x: event.clientX, y: event.clientY }
		this.listener.onTap(JSON.stringify(tapEvent))
	}
}

new GesturesManager(window, new AdapterGesturesListener(window.gestures))
