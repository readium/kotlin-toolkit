export interface DocumentBridge {
  onScriptsLoaded: () => void
  onDocumentLoaded: () => void
  onDocumentResized: () => void
}
