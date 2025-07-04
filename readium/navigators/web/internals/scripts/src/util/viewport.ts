import { Size } from "../common/geometry"

export class ViewportStringBuilder {
  private initialScale?: number

  private minimumScale?: number

  private width?: number

  private height?: number

  setInitialScale(scale: number): ViewportStringBuilder {
    this.initialScale = scale
    return this
  }

  setMinimumScale(scale: number): ViewportStringBuilder {
    this.minimumScale = scale
    return this
  }

  setWidth(width: number): ViewportStringBuilder {
    this.width = width
    return this
  }

  setHeight(height: number): ViewportStringBuilder {
    this.height = height
    return this
  }

  build(): string {
    const components: string[] = []

    if (this.initialScale) {
      components.push("initial-scale=" + this.initialScale)
    }

    if (this.minimumScale) {
      components.push("minimum-scale=" + this.minimumScale)
    }

    if (this.width) {
      components.push("width=" + this.width)
    }

    if (this.height) {
      components.push("height=" + this.height)
    }

    return components.join(", ")
  }
}

export function parseViewportString(viewportString: string): Size | undefined {
  const regex = /(\w+) *= *([^\s,]+)/g
  const properties = new Map<any, any>()
  let match
  while ((match = regex.exec(viewportString))) {
    if (match != null) {
      properties.set(match[1], match[2])
    }
  }
  const width = parseFloat(properties.get("width"))
  const height = parseFloat(properties.get("height"))
  if (width && height) {
    return { width, height }
  } else {
    return undefined
  }
}
