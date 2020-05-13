/*
 * Module: r2-navigator-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.pdf

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.readium.r2.navigator.NavigatorFragmentFactory
import org.readium.r2.navigator.R
import org.readium.r2.shared.extensions.getPublication
import org.readium.r2.shared.publication.Publication

class R2PdfActivity : AppCompatActivity() {

    private lateinit var publication: Publication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        publication = intent.getPublication(this)

        supportFragmentManager.fragmentFactory = NavigatorFragmentFactory(publication)

        setContentView(R.layout.activity_r2_pdf)
    }

}