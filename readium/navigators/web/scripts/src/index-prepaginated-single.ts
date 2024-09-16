//
//  Copyright 2021 Readium Foundation. All rights reserved.
//  Use of this source code is governed by the BSD-style license
//  available in the top-level LICENSE file of the project.
//

import { Insets, Size } from "./common/types";
import { Fit } from "./util/fit";
import { SingleAreaManager } from "./prepaginated/single-area-manager";

// Script used for the single spread wrapper HTML page for fixed layout resources.



/*class WebListener implements SingleAreaManager.Listener{

	onError(error: SingleAreaManager.Error): void {
		console.error(error);
	}
}

//declare let listener: FixedPageManager.Listener
const listener = new WebListener();*/


namespace Layout {

	const iframe = document.getElementById("page") as HTMLIFrameElement;

	const metaViewport = document.querySelector("meta[name=viewport]") as HTMLMetaElement;

	const manager = new SingleAreaManager(iframe, metaViewport);


	export function loadResource(
		url: string
	) {
		manager.loadResource(url);
	}

	export function setViewport(
		viewporttWidth: number,
		viewportHeight: number,
		insetTop: number,
		insetLeft: number,
		insetBottom: number,
		insetRight: number

	) {
		const viewport: Size = { width: viewporttWidth, height: viewportHeight};
		const insets: Insets = { top: insetTop, left: insetLeft, bottom: insetBottom, right: insetRight };
		manager.setViewport(viewport, insets);
	}

	export function setFit(
		fit: string
	) {	
		if (fit != "contain" && fit != "width" && fit != "height") {
			throw Error(`Invalid fit value: ${fit}`)
		}

		manager.setFit(fit as Fit);
	}
}

declare global {

	interface Window {
		
		layout: any
	}
}

Window.prototype.layout = Layout

