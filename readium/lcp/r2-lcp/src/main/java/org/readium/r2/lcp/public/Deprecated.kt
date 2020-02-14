/*
 * Module: r2-lcp-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.public

import android.content.Context

@Deprecated("Moved to the top-level package", ReplaceWith("org.readium.r2.lcp.LCPService"))
typealias LCPService = org.readium.r2.lcp.LCPService
@Deprecated("Moved to the top-level package", ReplaceWith("org.readium.r2.lcp.LCPImportedPublication"))
typealias LCPImportedPublication = org.readium.r2.lcp.LCPImportedPublication
@Deprecated("Moved to the top-level package", ReplaceWith("org.readium.r2.lcp.URLPresenter"))
typealias URLPresenter = org.readium.r2.lcp.URLPresenter
@Deprecated("Moved to the top-level package", ReplaceWith("org.readium.r2.lcp.LCPLicense"))
typealias LCPLicense = org.readium.r2.lcp.LCPLicense

@Deprecated("Moved to the top-level package", ReplaceWith("org.readium.r2.lcp.LCPAuthenticating"))
typealias LCPAuthenticating = org.readium.r2.lcp.LCPAuthenticating
@Deprecated("Moved to the top-level package", ReplaceWith("org.readium.r2.lcp.LCPAuthenticationDelegate"))
typealias LCPAuthenticationDelegate = org.readium.r2.lcp.LCPAuthenticationDelegate
@Deprecated("Moved to the top-level package", ReplaceWith("org.readium.r2.lcp.LCPAuthenticationReason"))
typealias LCPAuthenticationReason = org.readium.r2.lcp.LCPAuthenticationReason
@Deprecated("Moved to the top-level package", ReplaceWith("org.readium.r2.lcp.LCPAuthenticatedLicense"))
typealias LCPAuthenticatedLicense = org.readium.r2.lcp.LCPAuthenticatedLicense

@Deprecated("Moved to the top-level package", ReplaceWith("org.readium.r2.lcp.LCPError"))
typealias LCPError = org.readium.r2.lcp.LCPError
@Deprecated("Moved to the top-level package", ReplaceWith("org.readium.r2.lcp.StatusError"))
typealias StatusError = org.readium.r2.lcp.StatusError
@Deprecated("Moved to the top-level package", ReplaceWith("org.readium.r2.lcp.RenewError"))
typealias RenewError = org.readium.r2.lcp.RenewError
@Deprecated("Moved to the top-level package", ReplaceWith("org.readium.r2.lcp.ReturnError"))
typealias ReturnError = org.readium.r2.lcp.ReturnError
@Deprecated("Moved to the top-level package", ReplaceWith("org.readium.r2.lcp.ParsingError"))
typealias ParsingError = org.readium.r2.lcp.ParsingError
@Deprecated("Moved to the top-level package", ReplaceWith("org.readium.r2.lcp.LCPClientError"))
typealias LCPClientError = org.readium.r2.lcp.LCPClientError

@Deprecated("Moved to the top-level package", ReplaceWith("org.readium.r2.lcp.R2MakeLCPService(context)"))
fun R2MakeLCPService(context: Context) =
    org.readium.r2.lcp.R2MakeLCPService(context)
