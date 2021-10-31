package org.readium.r2.navigator2.view

import org.readium.r2.shared.publication.Locator

interface ResourceAdapter {

    val currentLocation: Locator
}