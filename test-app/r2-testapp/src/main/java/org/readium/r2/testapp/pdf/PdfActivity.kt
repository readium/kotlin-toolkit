/*
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp.pdf

import android.graphics.PointF
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.Observer
import org.readium.r2.navigator.pdf.PdfNavigatorFragment
import org.readium.r2.navigator.pdf.R2PdfActivity
import org.readium.r2.shared.PdfSupport
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.testapp.db.BooksDatabase

@OptIn(PdfSupport::class)
class PdfActivity : R2PdfActivity() {

    private var bookId: Long = -1
    private val books = BooksDatabase(this).books

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bookId = intent.getLongExtra("bookId", -1)
    }

    override fun onResourceLoadFailed(link: Link, error: Resource.Exception) {
        val message = when (error) {
            is Resource.Exception.OutOfMemory -> "The PDF is too large to be rendered on this device"
            else -> "Failed to render this PDF"
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        // There's nothing we can do to recover, so we quit the Activity.
        finish()
    }

    override fun onCurrentLocatorChanged(locator: Locator) {
        // Save last read location in the database.
        books.saveProgression(locator, bookId)
    }

    override fun onTap(point: PointF): Boolean {
        // Toggle app bar...
        return true
    }

}