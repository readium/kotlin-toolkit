/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp.opds

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.mcxiaoke.koi.ext.onClick
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.komponents.kovenant.ui.successUi
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.support.v4.nestedScrollView
import org.readium.r2.shared.extensions.destroyPublication
import org.readium.r2.shared.extensions.getPublication
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.opds.images
import org.readium.r2.testapp.R
import org.readium.r2.testapp.db.Book
import org.readium.r2.testapp.db.BooksDatabase
import org.readium.r2.testapp.db.books
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.CoroutineContext

class OPDSDetailActivity : AppCompatActivity(), CoroutineScope {
    /**
     * Context of this scope.
     */
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = BooksDatabase(this)

        val opdsDownloader = OPDSDownloader(this)
        val publication: Publication = intent.getPublication(this)

        nestedScrollView {
            fitsSystemWindows = true
            lparams(width = matchParent, height = matchParent)
            padding = dip(10)

            linearLayout {
                orientation = LinearLayout.VERTICAL

                imageView {
                    this@linearLayout.gravity = Gravity.CENTER

                    publication.coverLink?.let { link ->
                        Picasso.with(this@OPDSDetailActivity).load(link.href).into(this)
                    } ?: run {
                        if (publication.images.isNotEmpty()) {
                            Picasso.with(this@OPDSDetailActivity).load(publication.images.first().href).into(this)
                        }
                    }

                }.lparams {
                    height = 800
                    width = matchParent
                }

                textView {
                    padding = dip(10)
                    text = publication.metadata.title
                    textSize = 20f
                }
                textView {
                    padding = dip(10)
                    text = publication.metadata.description
                }

                val downloadUrl = getDownloadURL(publication)
                downloadUrl?.let {
                    button {
                        text = context.getString(R.string.opds_detail_download_button)
                        onClick {
                            val progress = indeterminateProgressDialog(getString(R.string.progress_wait_while_downloading_book))
                            progress.show()

                            opdsDownloader.publicationUrl(downloadUrl.toString()).successUi { pair ->

                                val publicationIdentifier = publication.metadata.identifier!!
                                val author = authorName(publication)
                                Thread {
                                    val stream = ByteArrayOutputStream()

                                    publication.coverLink?.let { link ->
                                        val bitmap = getBitmapFromURL(link.href)
                                        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream)
                                    } ?: run {
                                        if (publication.images.isNotEmpty()) {
                                            val bitmap = getBitmapFromURL(publication.images.first().href)
                                            bitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream)
                                        }
                                    }

                                    val book = Book(id = (-1).toLong(), title = publication.metadata.title, author = author, href = pair.first, identifier = publicationIdentifier, cover = stream.toByteArray(), ext = Publication.EXTENSION.EPUB, progression = "{}")
                                    database.books.insert(book, false)?.let {
                                        book.id = it
                                        books.add(0,book)
                                        this.snackbar("download completed")
                                        progress.dismiss()
                                    } ?: run {
                                        progress.dismiss()
                                        launch {

                                            val duplicateAlert = alert(Appcompat, "Publication already exists") {

                                                positiveButton("Add anyway") { }
                                                negativeButton("Cancel") { }

                                            }.build()
                                            duplicateAlert.apply {
                                                setCancelable(false)
                                                setCanceledOnTouchOutside(false)
                                                setOnShowListener {
                                                    val b2 = getButton(AlertDialog.BUTTON_POSITIVE)
                                                    b2.setOnClickListener {
                                                        database.books.insert(book, true)?.let {
                                                            book.id = it
                                                            books.add(0,book)
                                                            duplicateAlert.dismiss()
                                                        }
                                                    }
                                                    val bCancel = getButton(AlertDialog.BUTTON_NEGATIVE)
                                                    bCancel.setOnClickListener {
                                                        File(book.url!!).delete()
                                                        duplicateAlert.dismiss()
                                                    }
                                                }
                                            }
                                            duplicateAlert.show()
                                        }

                                    }

                                }.start()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        intent.destroyPublication(this)
    }

    private fun getDownloadURL(publication: Publication): URL? {
        var url: URL? = null
        val links = publication.links
        for (link in links) {
            val href = link.href
            if (href.contains(Publication.EXTENSION.EPUB.value) || href.contains(Publication.EXTENSION.LCPL.value)) {
                url = URL(href)
                break
            }
        }
        return url
    }


    private fun getBitmapFromURL(src: String): Bitmap? {
        return try {
            val url = URL(src)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun authorName(publication: Publication): String {
        return publication.metadata.authors.firstOrNull()?.name?.let {
            return@let it
        } ?: run {
            return@run String()
        }
    }


}
