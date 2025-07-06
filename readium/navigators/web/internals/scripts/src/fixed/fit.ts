import { Size } from "../common/geometry"

export type Fit = "contain" | "width" | "height"

export function computeScale(fit: Fit, content: Size, container: Size): number {
  switch (fit) {
    case "contain":
      return fitContain(content, container)
    case "width":
      return fitWidth(content, container)
    case "height":
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
