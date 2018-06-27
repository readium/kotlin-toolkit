package org.readium.r2.testapp.opds

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.widget.LinearLayout
import com.mcxiaoke.koi.ext.onClick
import com.squareup.picasso.Picasso
import nl.komponents.kovenant.ui.successUi
import org.jetbrains.anko.*
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.support.v4.nestedScrollView
import org.readium.r2.shared.Publication
import org.readium.r2.testapp.Book
import org.readium.r2.testapp.BooksDatabase
import org.readium.r2.testapp.R
import org.readium.r2.testapp.books
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class OPDSDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database: BooksDatabase = BooksDatabase(this)

        val opdsDownloader: OPDSDownloader = OPDSDownloader(this)
        val publication: Publication = intent.getSerializableExtra("publication") as Publication
        nestedScrollView {
            fitsSystemWindows = true
            lparams(width = matchParent, height = matchParent)
            padding = dip(10)

            linearLayout {
                orientation = LinearLayout.VERTICAL

                imageView {
                    this@linearLayout.gravity = Gravity.CENTER
                    Picasso.with(act).load(publication.images.first().href).into(this);
                }.lparams {
                    height = 800
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
                        text = "Download"
                        onClick {
                            val progress = indeterminateProgressDialog(getString(R.string.progress_wait_while_downloading_book))
                            progress.show()

//                            for (link in publication.links) {
//                                if (link.typeLink.equals(mimetype)) {
                                    opdsDownloader.publicationUrl(downloadUrl.toString()).successUi { pair ->

                                        val publicationIdentifier = publication.metadata.identifier
                                        val author = authorName(publication)
                                        val bitmap = getBitmapFromURL(publication.images.first().href!!)
                                        val stream = ByteArrayOutputStream()
                                        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream)

                                        val book = Book(pair.second, publication.metadata.title, author, pair.first, -1.toLong(), publication.coverLink?.href, publicationIdentifier, stream.toByteArray())
                                        database.books.insert(book, false)?.let {
                                            books.add(book)
                                            snackbar(this, "download completed")
                                            progress.dismiss()
                                        }?: run {
                                            snackbar(this, "download failed")
                                            progress.dismiss()
                                        }
                                    }
//                                }
//                            }
                        }
                    }
                }
            }
        }
    }


    private fun getDownloadURL(publication:Publication) : URL? {
        var url: URL? = null
        val links = publication.links
        for (link in links) {
            val href = link.href
            if (href != null) {
                if (href.contains(".epub") || href.contains(".lcpl")) {
                    url = URL(href)
                    break
                }
            }
        }
        return url
    }


    fun getBitmapFromURL(src: String): Bitmap? {
        try {
            val url = URL(src)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            return BitmapFactory.decodeStream(input)
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

    }

    private fun authorName(publication: Publication): String {
        val author = publication.metadata.authors.firstOrNull()?.name?.let {
            return@let it
        } ?: run {
            return@run String()
        }
        return author
    }


}
