const debug = false

export function log(...args: any[]) {
  if (debug) {
    console.log(...args)
  }
}
