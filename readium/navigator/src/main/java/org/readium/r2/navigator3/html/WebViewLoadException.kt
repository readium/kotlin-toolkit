package org.readium.r2.navigator3.html

import java.io.IOException

internal class WebViewLoadException(
    override val message: String,
    val failures: Map<String, String>
) : IOException(message)
