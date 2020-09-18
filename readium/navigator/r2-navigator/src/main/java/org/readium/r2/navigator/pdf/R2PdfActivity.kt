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
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.NavigatorFragmentFactory
import org.readium.r2.navigator.R
import org.readium.r2.shared.FragmentNavigator
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.extensions.getPublication
import org.readium.r2.shared.extensions.putPublication
import org.readium.r2.shared.publication.Publication

@PdfSupport
open class R2PdfActivity : AppCompatActivity() {

    protected lateinit var publication: Publication

    protected val navigator: Navigator get() =
        supportFragmentManager.findFragmentById(R.id.r2_pdf_navigator) as Navigator

    @OptIn(FragmentNavigator::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val baseUrl = intent.getStringExtra(EXTRA_BASE_URL)
            ?: throw IllegalArgumentException("baseUrl is required")

        publication = intent.getPublication(this)

        supportFragmentManager.fragmentFactory = NavigatorFragmentFactory(
            publication = publication,
            baseUrl = baseUrl,
            initialLocator = intent.getParcelableExtra("locator")
        )

        super.onCreate(savedInstanceState)

        setContentView(R.layout.r2_pdf_activity)
    }

    override fun finish() {
        setResult(Activity.RESULT_OK, intent)
        super.finish()
    }

    companion object {
        private const val EXTRA_BASE_URL = "baseUrl"

        fun createIntent(context: Context, publication: Publication, baseUrl: String): Intent = Intent(context, R2PdfActivity::class.java).apply {
            putPublication(publication)
            putExtra(EXTRA_BASE_URL, baseUrl)
        }

    }

}
