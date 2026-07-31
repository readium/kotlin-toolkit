//
//  Copyright 2026 Readium Foundation. All rights reserved.
//  Use of this source code is governed by the BSD-style license
//  available in the top-level LICENSE file of the project.
//

export interface CopyListenerBridge {
  shouldInterceptCopy(): boolean
  onCopyIntercepted(text: string): void
  onSelectionStart(): void
  onSelectionEnd(): void
}

/**
 * Intercepts copy events (system selection menu Copy, Ctrl+C) to let native
 * code enforce the publication's copy allowance.
 *
 * `shouldInterceptCopy()` is the single gate: when it returns true, the
 * default clipboard write is prevented and the raw selected text is forwarded
 * to native code, which performs the counted copy itself.
 */
export class CopyInterceptor {
  constructor(window: Window, listener: CopyListenerBridge) {
    window.document.addEventListener("copy", (event) => {
      if (!listener.shouldInterceptCopy()) {
        return
      }
      event.preventDefault()
      const text = window.getSelection()?.toString()
      if (text) {
        listener.onCopyIntercepted(text)
      }
    })
  }
}
