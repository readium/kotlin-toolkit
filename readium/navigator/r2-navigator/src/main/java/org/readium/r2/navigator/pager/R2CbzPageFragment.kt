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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.readium.r2.navigator.R
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import kotlin.coroutines.CoroutineContext


class R2CbzPageFragment(private val publication: Publication)
    : androidx.fragment.app.Fragment(), CoroutineScope  {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    private val link: Link
        get() = requireArguments().getParcelable<Link>("link")!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.viewpager_fragment_cbz, container, false)
        val imageView = view.findViewById<ImageView>(R.id.imageView)

       launch {
           publication.get(link)
               .read()
               .getOrNull()
               ?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
               ?.let { imageView.setImageBitmap(it) }
       }

       return view
    }

}


