package org.readium.r2.navigator3.html

import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import timber.log.Timber

/**
 * A web chrome client that provides logging.
 */

internal class WebChromeClient : WebChromeClient() {

    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        Timber.v("onConsoleMessage: ${consoleMessage.sourceId()}:${consoleMessage.lineNumber()}: ${consoleMessage.message()}")
        return super.onConsoleMessage(consoleMessage)
    }
}
