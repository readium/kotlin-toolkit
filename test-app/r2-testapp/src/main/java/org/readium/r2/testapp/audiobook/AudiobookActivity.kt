package org.readium.r2.testapp.audiobook


import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.Observer
import org.jetbrains.anko.toast
import org.readium.r2.navigator.NavigatorDelegate
import org.readium.r2.navigator.audio.AudioActivity
import org.readium.r2.shared.AudioSupport
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.services.isProtected
import org.readium.r2.testapp.R
import org.readium.r2.testapp.db.Bookmark
import org.readium.r2.testapp.db.BookmarksDatabase
import org.readium.r2.testapp.db.BooksDatabase
import org.readium.r2.testapp.outline.R2OutlineActivity
import timber.log.Timber


@OptIn(AudioSupport::class)
class AudiobookActivity : AudioActivity(), NavigatorDelegate {

    private var bookId: Long = -1
    private lateinit var booksDB: BooksDatabase
    private lateinit var bookmarksDB: BookmarksDatabase

    private lateinit var outlineLauncher: ActivityResultLauncher<R2OutlineActivity.Contract.Input>

    private var menuDrm: MenuItem? = null
    private var menuToc: MenuItem? = null
    private var menuBmk: MenuItem? = null
    private var menuSettings: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        booksDB = BooksDatabase(this)
        bookmarksDB = BookmarksDatabase(this)
        bookId = intent.getLongExtra("bookId", -1)

        menuDrm?.isVisible = navigator.publication.isProtected

        navigator.currentLocator.observe(this, Observer { locator ->
            locator ?: return@Observer
            Timber.d("currentLocator $locator")
            booksDB.books.saveProgression(locator, bookId)
        })

        outlineLauncher = registerForActivityResult(R2OutlineActivity.Contract()) { locator: Locator? ->
            if (locator != null) {
                navigator.go(locator)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        navigator.stop()
    }

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
                outlineLauncher.launch(R2OutlineActivity.Contract.Input(
                    publication = navigator.publication,
                    bookId = bookId
                ))
                return true
            }
            R.id.bookmark -> {
                val locator = navigator.currentLocator.value ?:
                     return true

                val bookmark = Bookmark(bookId, navigator.publication.metadata.identifier ?: bookId.toString(), resourceIndex = 0, locator = locator)
                if (bookmarksDB.bookmarks.insert(bookmark) != null) {
                    toast("Bookmark added")
                } else {
                    toast("Bookmark already exists")
                }

                return true
            }

            else -> return false
        }

    }

}
