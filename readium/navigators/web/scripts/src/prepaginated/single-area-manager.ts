import { Insets, Size } from "../common/types"
import { computeScale, Fit } from "../util/fit"
import { PageManager } from "./page-manager"
import { ViewportStringBuilder } from "../util/viewport"

export class SingleAreaManager {

  private readonly metaViewport: HTMLMetaElement

  private readonly page: PageManager

  private fit: Fit = Fit.Contain

  private insets: Insets = { top: 0, right: 0, bottom: 0, left: 0 }

  private viewport?: Size

  constructor(iframe: HTMLIFrameElement, metaViewport: HTMLMetaElement) {
    this.metaViewport = metaViewport
    const listener = { onIframeLoaded: () => { this.onIframeLoaded() } }
    this.page = new PageManager(iframe, listener)
  }

  setViewport(viewport: Size, insets: Insets) {
    if (this.viewport == viewport && this.insets == insets) {
      return
    }

    this.viewport = viewport
    this.insets = insets
    this.layout()
  }

  setFit(fit: Fit) {
    if (this.fit == fit) {
      return
    }

    this.fit = fit
    this.layout()
  }

  loadResource(url: string) {
    this.page.hide()
    this.page.loadPage(url)
  }

  private onIframeLoaded() {
    if (!this.page.size) {
      // FIXME: raise error
    } else {
      this.layout()
    }    
  }

  private layout() {
    if (!this.page.size || !this.viewport) {
      return
    }

    const margins = {
      top: this.insets.top,
      right: this.insets.right, 
      bottom: this.insets.bottom,
      left: this.insets.left
    }
    this.page.setMargins(margins)

    const safeDrawingSize = {
      width: this.viewport.width - this.insets.left - this.insets.right,
      height: this.viewport.height - this.insets.top - this.insets.bottom
    }
    const scale = computeScale(this.fit, this.page.size, safeDrawingSize)
    this.metaViewport.content = new ViewportStringBuilder()
    .setInitialScale(scale)
    .setMinimumScale(scale)
    .setWidth(this.page.size.width)
    .setHeight(this.page.size.height)
    .build()

    this.page.show()
  }
}
