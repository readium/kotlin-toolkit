/**
 * In paginated mode, the width of each resource must be a multiple of the viewport size
 * for proper snapping.  This may not be automatically the case if the number of
 * columns in the resource is not a multiple of the number of columns fitting in the viewport.
 * To fix this, we insert a blank virtual column at the end of the resource.
 *
 * Returns if a virtual column has been added or removed.
 */
export function appendVirtualColumnIfNeeded(wnd: Window): boolean {
  // FIXME: what about scroll mode?
  const id = "readium-virtual-page"
  let virtualCol = wnd.document.getElementById(id)
  if (getColumnCountPerScreen(wnd) !== 2) {
    if (virtualCol) {
      virtualCol.remove()
      return true
    } else {
      return false
    }
  } else {
    const documentWidth = wnd.document.scrollingElement!.scrollWidth
    const colCount = documentWidth / wnd.innerWidth
    const hasOddColCount = (Math.round(colCount * 2) / 2) % 1 > 0.1
    if (hasOddColCount) {
      if (virtualCol) {
        virtualCol.remove()
      } else {
        virtualCol = wnd.document.createElement("div")
        virtualCol.setAttribute("id", id)
        virtualCol.dataset.readium = "true"
        virtualCol.style.breakBefore = "column"
        virtualCol.innerHTML = "&#8203;" // zero-width space
        wnd.document.body.appendChild(virtualCol)
      }
      return true
    } else {
      return false
    }
  }
}

function getColumnCountPerScreen(wnd: Window) {
  return parseInt(
    wnd
      .getComputedStyle(wnd.document.documentElement)
      .getPropertyValue("column-count")
  )
}
