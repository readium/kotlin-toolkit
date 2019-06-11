/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi, Mostapha Idoubihi
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.pager

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import org.readium.r2.navigator.R
import org.zeroturnaround.zip.ZipUtil
import java.io.ByteArrayInputStream
import java.io.File


class R2CbzPageFragment : androidx.fragment.app.Fragment() {

    private val zipFile: String?
        get() = arguments!!.getString("zipFile")
    private val zipEntry: String?
        get() = arguments!!.getString("zipEntry")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val v = inflater.inflate(R.layout.fragment_page_cbz, container, false)
        val imageView = v.findViewById<ImageView>(R.id.imageView)

        val blob = ZipUtil.unpackEntry(File(zipFile), zipEntry)
        blob?.let {
            val arrayInputStream = ByteArrayInputStream(it)
            val bitmap = BitmapFactory.decodeStream(arrayInputStream)
            imageView.setImageBitmap(bitmap)
        }

        return v
    }

    companion object {

        fun newInstance(zipFile: String, zipEntry: String): R2CbzPageFragment {
            val args = Bundle()
            args.putString("zipFile", zipFile)
            args.putString("zipEntry", zipEntry)
            val fragment = R2CbzPageFragment()
            fragment.arguments = args
            return fragment
        }
    }

}


