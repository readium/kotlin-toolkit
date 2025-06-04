import { Size } from "../common/types"

export const enum Fit {
  Contain = "contain",
  Width = "width",
  Height = "height",
}

export function computeScale(fit: Fit, content: Size, container: Size): number {
  switch (fit) {
    case Fit.Contain:
      return fitContain(content, container)
    case Fit.Width:
      return fitWidth(content, container)
    case Fit.Height:
      return fitHeight(content, container)
  }
}

function fitContain(content: Size, container: Size): number {
  const widthRatio = container.width / content.width
  const heightRatio = container.height / content.height
  return Math.min(widthRatio, heightRatio)
}

function fitWidth(content: Size, container: Size): number {
  return container.width / content.width
}

function fitHeight(content: Size, container: Size): number {
  return container.height / content.height
}
