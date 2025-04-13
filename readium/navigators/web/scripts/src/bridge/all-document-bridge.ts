export interface DocumentBridge {
  onScriptsLoaded: () => void
  onDocumentLoadedAndSized: () => void
  onDocumentResized: () => void
}
