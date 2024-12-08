export class CssBridge {
  readonly document: Document

  constructor(document: Document) {
    this.document = document
  }

  setProperties(properties: Map<string, string>) {
    for (const [key, value] of properties) {
      this.setProperty(key, value)
    }
  }

  // For setting user setting.
  setProperty(key: string, value: string) {
    if (value === null || value === "") {
      this.removeProperty(key)
    } else {
      const root = document.documentElement
      // The `!important` annotation is added with `setProperty()` because if it's part of the
      // `value`, it will be ignored by the Web View.
      root.style.setProperty(key, value, "important")
    }
  }

  // For removing user setting.
  removeProperty(key: string) {
    const root = document.documentElement
    root.style.removeProperty(key)
  }
}
