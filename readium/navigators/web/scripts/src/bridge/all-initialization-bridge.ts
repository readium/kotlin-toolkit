export interface InitializationBridge {
  onScriptsLoaded: () => void
  onDocumentLoaded: () => void
  onDocumentResized: () => void
}
