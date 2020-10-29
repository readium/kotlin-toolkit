package org.readium.r2.testapp.audiobook


import android.app.ProgressDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.indeterminateProgressDialog
import org.jetbrains.anko.toast
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.NavigatorDelegate
import org.readium.r2.navigator.audiobook.R2AudiobookActivity
import org.readium.r2.shared.extensions.putPublicationFrom
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.services.isProtected
import org.readium.r2.testapp.R
import org.readium.r2.testapp.db.Bookmark
import org.readium.r2.testapp.db.BookmarksDatabase
import org.readium.r2.testapp.db.BooksDatabase
import org.readium.r2.testapp.library.LibraryActivity
import org.readium.r2.testapp.library.activitiesLaunched
import org.readium.r2.testapp.outline.R2OutlineActivity
import timber.log.Timber


class AudiobookActivity : R2AudiobookActivity(), NavigatorDelegate {

    override fun locationDidChange(navigator: Navigator?, locator: Locator) {
        Timber.d("locationDidChange $locator")
        booksDB.books.saveProgression(locator, bookId)
    }

    private lateinit var booksDB: BooksDatabase

    private lateinit var bookmarksDB: BookmarksDatabase
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        if (activitiesLaunched.incrementAndGet() > 1 || !LibraryActivity.isServerStarted) {
            finish()
        }
        super.onCreate(savedInstanceState)

        booksDB = BooksDatabase(this)
        bookmarksDB = BookmarksDatabase(this)

        navigatorDelegate = this

        bookId = intent.getLongExtra("bookId", -1)

        progressDialog = indeterminateProgressDialog(getString(R.string.progress_wait_while_preparing_audiobook))

        //Setting cover
        launch {
            delay(100)
            if (intent.hasExtra("cover")) {
                val byteArray = intent.getByteArrayExtra("cover")
                byteArray?.let {
                    val bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                    findViewById<ImageView>(R.id.imageView).setImageBitmap(bmp)
                }
            }
            menuDrm?.isVisible = publication.isProtected
        }
        mediaPlayer?.progress = progressDialog


        // Loads the last read location
        booksDB.books.currentLocator(bookId)?.let {
            go(it, false, {})
        }
    }

    private var menuDrm: MenuItem? = null
    private var menuToc: MenuItem? = null
    private var menuBmk: MenuItem? = null
    private var menuSettings: MenuItem? = null

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_audio, menu)
        menuDrm = menu?.findItem(R.id.drm)
        menuToc = menu?.findItem(R.id.toc)
        menuBmk = menu?.findItem(R.id.bookmark)
        menuSettings = menu?.findItem(R.id.settings)

        menuSettings?.isVisible = false
        menuDrm?.isVisible = false
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.toc -> {
                val intent = Intent(this, R2OutlineActivity::class.java).apply {
                    putPublicationFrom(this@AudiobookActivity)
                    putExtra("bookId", bookId)
                }
                startActivityForResult(intent, 2)
                return true
            }
            R.id.settings -> {
                // TODO do we need any settings ?
                return true
            }
            R.id.bookmark -> {
                val locator = currentLocator.value

                val bookmark = Bookmark(bookId, publicationIdentifier, resourceIndex = currentResource.toLong(), locator = locator)
                bookmarksDB.bookmarks.insert(bookmark)?.let {
                    launch {
                        toast("Bookmark added")
                    }
                } ?: run {
                    launch {
                        toast("Bookmark already exists")
                    }
                }

                return true
            }

            else -> return false
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        activitiesLaunched.getAndDecrement()
    }

    override fun onStop() {
        super.onStop()
        progressDialog.dismiss()
    }

}




