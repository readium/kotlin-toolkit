//
//  Copyright 2021 Readium Foundation. All rights reserved.
//  Use of this source code is governed by the BSD-style license
//  available in the top-level LICENSE file of the project.
//

import { Insets, Size } from "./common/types";
import { DoubleAreaManager } from "./prepaginated/double-area-manager";
import { Fit } from "./util/fit";

namespace Layout {

  const leftIframe = document.getElementById("page-left") as HTMLIFrameElement;

  const rightIframe = document.getElementById("page-right") as HTMLIFrameElement;

  const metaViewport = document.querySelector("meta[name=viewport]") as HTMLMetaElement;

	const manager = new DoubleAreaManager(leftIframe, rightIframe, metaViewport);

  export function loadSpread(
    spread: { left?: string, right?: string }
  ) {
    manager.loadSpread(spread)
  }

	export function setViewport(
		viewporttWidth: number,
		viewportHeight: number,
		insetTop: number,
		insetRight: number,
		insetBottom: number,
		insetLeft: number,
	) {
		const viewport: Size = { width: viewporttWidth, height: viewportHeight}
		const insets: Insets = { top: insetTop, left: insetLeft, bottom: insetBottom, right: insetRight }
		manager.setViewport(viewport, insets)
	}

  export function setFit(
		fit: string
	) {
		if (fit != "contain" && fit != "width" && fit != "height") {
			throw Error(`Invalid fit value: ${fit}`)
		}

		manager.setFit(fit as Fit)
  }
}

declare global {

	interface Window {
		
		layout: any
	}
}

Window.prototype.layout = Layout
