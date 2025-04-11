/**
 * Having an odd number of columns when displaying two columns per screen causes snapping and page
 * turning issues. To fix this, we insert a blank virtual column at the end of the resource.
 */
export function appendVirtualColumnIfNeeded(wnd: Window) {
  const id = "readium-virtual-page"
  let virtualCol = wnd.document.getElementById(id)
  if (getColumnCountPerScreen(wnd) !== 2) {
    if (virtualCol) {
      virtualCol.remove()
    }
  } else {
    const documentWidth = wnd.document.scrollingElement!.scrollWidth
    const colCount = documentWidth / wnd.innerWidth
    const hasOddColCount = (Math.round(colCount * 2) / 2) % 1 > 0.1
    if (hasOddColCount) {
      if (virtualCol) virtualCol.remove()
      else {
        virtualCol = wnd.document.createElement("div")
        virtualCol.setAttribute("id", id)
        virtualCol.dataset.readium = "true"
        virtualCol.style.breakBefore = "column"
        virtualCol.innerHTML = "&#8203;" // zero-width space
        wnd.document.body.appendChild(virtualCol)
      }
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
