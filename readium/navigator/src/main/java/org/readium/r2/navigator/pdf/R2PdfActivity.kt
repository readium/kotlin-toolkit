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
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.R
import org.readium.r2.navigator.util.CompositeFragmentFactory
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

        // This must be done before the call to super.onCreate, including by reading apps.
        // Because they may want to set their own factories, let's use a CompositeFragmentFactory that retains
        // previously set factories.
        supportFragmentManager.fragmentFactory = CompositeFragmentFactory(
            supportFragmentManager.fragmentFactory,
            PdfNavigatorFragment.createFactory(
                publication = publication,
                initialLocator = intent.getParcelableExtra("locator"),
                listener = this
            )
        )

        super.onCreate(savedInstanceState)

        setContentView(R.layout.r2_pdf_activity)

        navigator.currentLocator.asLiveData().observe(this, Observer { locator ->
            locator ?: return@Observer

            onCurrentLocatorChanged(locator)
        })

        // Display cutouts are not compatible with the underlying `PdfNavigatorFragment` yet.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
        }
    }

    override fun finish() {
        setResult(Activity.RESULT_OK, intent)
        super.finish()
    }

}
