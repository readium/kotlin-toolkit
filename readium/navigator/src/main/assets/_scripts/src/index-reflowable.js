//
//  Copyright 2021 Readium Foundation. All rights reserved.
//  Use of this source code is governed by the BSD-style license
//  available in the top-level LICENSE file of the project.
//

// Script used for reflowable resources.

import "./index";

window.readium.isReflowable = true;

document.addEventListener("DOMContentLoaded", function () {
  // Setups the `viewport` meta tag to disable zooming.
  let meta = document.createElement("meta");
  meta.setAttribute("name", "viewport");
  meta.setAttribute(
    "content",
    "width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, shrink-to-fit=no"
  );
  document.head.appendChild(meta);
});
