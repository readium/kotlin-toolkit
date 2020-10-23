/*
 * Module: r2-navigator-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.pdf

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.R
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.extensions.getPublication
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

@PdfSupport
abstract class R2PdfActivity : AppCompatActivity(), PdfNavigatorFragment.Listener {

    protected lateinit var publication: Publication

    protected val navigator: Navigator get() =
        supportFragmentManager.findFragmentById(R.id.r2_pdf_navigator) as Navigator

    /**
     * Override this event handler to save the current location in the publication in a database.
     */
    open fun onCurrentLocatorChanged(locator: Locator) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        publication = intent.getPublication(this)

        supportFragmentManager.fragmentFactory = PdfNavigatorFragment.createFactory(
            publication = publication,
            initialLocator = intent.getParcelableExtra("locator"),
            listener = this
        )

        super.onCreate(savedInstanceState)

        setContentView(R.layout.r2_pdf_activity)

        navigator.currentLocator.asLiveData().observe(this, Observer { locator ->
            locator ?: return@Observer

            onCurrentLocatorChanged(locator)
        })
    }

    override fun finish() {
        setResult(Activity.RESULT_OK, intent)
        super.finish()
    }

}
