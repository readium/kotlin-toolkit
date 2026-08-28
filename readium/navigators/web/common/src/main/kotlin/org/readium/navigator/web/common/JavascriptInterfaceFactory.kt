/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.navigator.web.common

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Url

/**
 * Creates the object to expose to the JavaScript running in the web view of the resource
 * at [href], or `null` to skip it for that resource.
 *
 * The returned object's methods must be annotated with `@android.webkit.JavascriptInterface`
 * to be reachable from JavaScript, and are called on a background thread.
 */
@ExperimentalReadiumApi
public typealias JavascriptInterfaceFactory = (href: Url) -> Any?
