function getWritingMode(wnd: Window): string {
  const el = wnd.document.documentElement || wnd.document.body;
  const wm = wnd.getComputedStyle(el).writingMode || '';
  return /vertical-/.test(wm) ? 'vertical' : 'horizontal';
}

/**
 * In paginated mode, the width of each resource must be a multiple of the viewport size
 * for proper snapping.  This may not be automatically the case if the number of
 * columns in the resource is not a multiple of the number of columns displayed in the viewport.
 * To fix this, we insert a blank virtual column at the end of the resource.
 *
 * Returns if the column number has changed.
 */
export function appendVirtualColumnIfNeeded(wnd: Window): boolean {
  const wm = getWritingMode(wnd);
  const colCountPerScreen = getColumnCountPerScreen(wnd)
  if (!colCountPerScreen) {
    // scroll mode
    return false
  }

  const virtualCols = wnd.document.querySelectorAll(
    "div[id^='readium-virtual-page']"
  )
  const virtualColsCount = virtualCols.length

  // Remove first so that we don't end up with an incorrect scrollWidth
  // Even when removing their width we risk having an incorrect scrollWidth
  // so removing them entirely is the most robust solution
  for (const virtualCol of virtualCols) {
    virtualCol.remove()
  }

  const documentWidth = wnd.document.scrollingElement!.scrollWidth
  const windowWidth = wnd.visualViewport!.width

  const totalColCount = Math.round(
    (documentWidth / windowWidth) * colCountPerScreen
  )
  const lonelyColCount = totalColCount % colCountPerScreen

  console.log(`EPUB_MULTICOLUMN: wm=${wm}, documentWidth=${documentWidth}, windowWidth=${windowWidth}, totalColCount=${totalColCount}`)

  const needed =
    colCountPerScreen === 1 || lonelyColCount === 0
      ? 0
      : colCountPerScreen - lonelyColCount

  if (needed > 0) {
    for (let i = 0; i < needed; i++) {
      const virtualCol = wnd.document.createElement("div")
      virtualCol.setAttribute("id", `readium-virtual-page-${i}`)
      virtualCol.dataset.readium = "true"
      virtualCol.style.breakBefore = "column"
      virtualCol.innerHTML = "&#8203;" // zero-width space
      wnd.document.body.appendChild(virtualCol)
    }
  }

  return virtualColsCount != needed
}

export function getAxisAwareSizesTS(wnd: Window): { pageExtent: number; documentRange: number } {
  const wm = getWritingMode(wnd);
  const doc = wnd.document.scrollingElement || wnd.document.documentElement;
  return (wm === 'vertical')
    ? { pageExtent: wnd.innerHeight, documentRange: doc.scrollHeight }
    : { pageExtent: wnd.innerWidth,  documentRange: doc.scrollWidth  };
}

function getColumnCountPerScreen(wnd: Window) {
  return parseInt(
    wnd
      .getComputedStyle(wnd.document.documentElement)
      .getPropertyValue("column-count")
  )
}
