//
//  Copyright 2023 Readium Foundation. All rights reserved.
//  Use of this source code is governed by the BSD-style license
//  available in the top-level LICENSE file of the project.
//


import { FrameClickEvent } from "@readium/navigator-html-injectables/src/modules/ReflowablePeripherals";
import { EpubNavigator, EpubNavigatorListeners } from "@readium/navigator/src/"
import { Locator, Manifest, Publication } from "@readium/shared/src";
import { Fetcher } from "@readium/shared/src/fetcher";
import { HttpFetcher } from "@readium/shared/src/fetcher/HttpFetcher";
import { Link } from "@readium/shared/src";

console.log("Scrip loaded")

// Public API used by the navigator.
window.readium = {
  load: load,
  navigator: navigator
};


interface AndroidInterface {

}


declare var Android: AndroidInterface;

export var navigator: EpubNavigator

export async function load() {
  let manifestUrl = "https://readium/publication/manifest.json" 
  let container: HTMLElement = document.body.firstElementChild as HTMLElement
  let manifestLink = new Link({href: manifestUrl})
  let fetcher: Fetcher =  new HttpFetcher(window.fetch.bind(window), manifestUrl)
  await fetcher.get(manifestLink).readAsJSON()
  .then((response: string) => {
    let manifest = Manifest.deserialize(response)
    console.log(manifest)
    let publication = new Publication({ manifest: manifest, fetcher: fetcher })
    let listeners: EpubNavigatorListeners = {
      frameLoaded: function (wnd: Window): void {
        navigator._cframes.forEach((frameManager) => {
          frameManager.comms.send(
            "set_property",
            ["--USER__colCount", 1],
            (ok: boolean) => (ok ? {} : {})
          );
        })
      },
      positionChanged: function (locator: Locator): void {
      },
      tap: function (e: FrameClickEvent): boolean {
        return false
      },
      click: function (e: FrameClickEvent): boolean {
        return false
      },
      zoom: function (scale: number): void {
      },
      miscPointer: function (amount: number): void {
      },
      customEvent: function (key: string, data: unknown): void {
      },
      handleLocator: function (locator: Locator): boolean {
        return false
      }
    }
    navigator = new EpubNavigator(container, publication, listeners)
    navigator.load()
  })
}
