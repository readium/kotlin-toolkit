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
import org.readium.r2.lcp.LcpAuthenticating
import org.readium.r2.lcp.LcpException
import org.readium.r2.lcp.LcpLicense
import org.readium.r2.lcp.LcpService

@Deprecated(
    "Renamed to `LcpService`",
    ReplaceWith("org.readium.r2.lcp.LcpService"),
    level = DeprecationLevel.ERROR
)
public typealias LCPService = LcpService

@Deprecated(
    "Renamed to `LcpService.AcquiredPublication`",
    ReplaceWith("org.readium.r2.lcp.LcpService.AcquiredPublication"),
    level = DeprecationLevel.ERROR
)
public typealias LCPImportedPublication = LcpService.AcquiredPublication

@Deprecated("Not used anymore", level = DeprecationLevel.ERROR)
public typealias URLPresenter = () -> Unit

@Deprecated(
    "Renamed to `LcpLicense`",
    ReplaceWith("org.readium.r2.lcp.LcpLicense"),
    level = DeprecationLevel.ERROR
)
public typealias LCPLicense = LcpLicense

@Deprecated(
    "Renamed to `LcpAuthenticating`",
    ReplaceWith("org.readium.r2.lcp.LcpAuthenticating"),
    level = DeprecationLevel.ERROR
)
public typealias LCPAuthenticating = LcpAuthenticating

@Deprecated("Not used anymore", level = DeprecationLevel.ERROR)
public interface LCPAuthenticationDelegate

@Deprecated(
    "Renamed to `LcpAuthenticating.AuthenticationReason`",
    ReplaceWith("org.readium.r2.lcp.LcpAuthenticating.AuthenticationReason"),
    level = DeprecationLevel.ERROR
)
public typealias LCPAuthenticationReason = LcpAuthenticating.AuthenticationReason

@Deprecated(
    "Renamed to `LcpAuthenticating.AuthenticatedLicense`",
    ReplaceWith("org.readium.r2.lcp.LcpAuthenticating.AuthenticatedLicense"),
    level = DeprecationLevel.ERROR
)
public typealias LCPAuthenticatedLicense = LcpAuthenticating.AuthenticatedLicense

@Deprecated(
    "Renamed to `LcpException",
    ReplaceWith("org.readium.r2.lcp.LcpException"),
    level = DeprecationLevel.ERROR
)
public typealias LCPError = LcpException

@Deprecated(
    "Renamed to `LcpService()`",
    ReplaceWith("LcpService()"),
    level = DeprecationLevel.ERROR
)
@Suppress("UNUSED_PARAMETER")
public fun R2MakeLCPService(context: Context): LcpService? =
    throw NotImplementedError()
