package org.readium.r2.testapp

import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentResolver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.AdapterView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_catalog.*
import org.readium.r2.navigator.R2EpubActivity
import org.readium.r2.streamer.Parser.EpubParser
import org.readium.r2.streamer.Server.Server
import org.readium.r2.testapp.permissions.PermissionHelper
import org.readium.r2.testapp.permissions.Permissions
import java.io.File
import java.util.ArrayList

class CatalogActivity : AppCompatActivity() {

    private lateinit var books:ArrayList<Book>

    val server = Server()

    private lateinit var booksAdapter: BooksAdapter
    private lateinit var permissionHelper: PermissionHelper
    private lateinit var permissions: Permissions

    private val R2TEST_DIRECTORY_PATH = server.rootDir
    private var EPUB_FILE_NAME = "dummy.epub"
    private var PUBLICATION_PATH: String = R2TEST_DIRECTORY_PATH + EPUB_FILE_NAME

    override fun onStart() {
        super.onStart()
        
        permissionHelper.storagePermission {
            val prefs = getPreferences(Context.MODE_PRIVATE)
            if (!prefs.contains("dummy")) {
                val dir = File(R2TEST_DIRECTORY_PATH)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                copyEpubFromAssetsToStorage(EPUB_FILE_NAME)
                prefs.edit().putBoolean("dummy",true).apply()
            }

            // TODO change to a SQLite DB
            if (books.size == 0) {
                val listOfFiles = File(R2TEST_DIRECTORY_PATH).listFilesSafely()
                for (i in listOfFiles.indices) {
                    val file = listOfFiles.get(i)
                    val local_epub_path: String = R2TEST_DIRECTORY_PATH + file.name
                    val pub = EpubParser().parse(local_epub_path)
                    if (pub != null) {
                        val publication = pub.publication
                        var author = ""
                        if (!publication.metadata.authors.isEmpty()) {
                            author = publication.metadata.authors.get(0).name!!
                        }
                        val book = Book(file.name, publication.metadata.title, author, file.absolutePath, i.toLong())
                        books.add(book)
                    }
                }
                booksAdapter.notifyDataSetChanged()
            }
        }

        startServer()

    }

    override fun onDestroy() {
        super.onDestroy()
        stopServer()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catalog)

        permissions = Permissions(this)
        permissionHelper = PermissionHelper(this, permissions)

        books = arrayListOf<Book>()
        booksAdapter = BooksAdapter(this, books)
        gridview.adapter = booksAdapter

        gridview.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val book = books[position]

            EPUB_FILE_NAME = book.fileName

            parseAndShowEpub()

            val pub = EpubParser().parse(PUBLICATION_PATH)
            if (pub != null) {
                val publication = pub.publication
                if (publication.spine.size > 0) {
                    val intent = Intent(this, R2EpubActivity::class.java)

                    intent.putExtra("publication_path", PUBLICATION_PATH)
                    intent.putExtra("epub_name", EPUB_FILE_NAME)
                    intent.putExtra("publication", publication)
                    startActivity(intent)
                }
            }
        }

        gridview.setOnItemLongClickListener { parent, view, position, id ->


            Log.v("tag", "long click detected, deleting book")
            val book = books[position]
            EPUB_FILE_NAME = book.fileName
            books.remove(book)
            booksAdapter.notifyDataSetChanged()
            val file = File(PUBLICATION_PATH)
            file.delete()

            true
        }




        if (intent.action.compareTo(Intent.ACTION_VIEW) == 0) {

            if (intent.scheme.compareTo(ContentResolver.SCHEME_CONTENT) == 0) {
                val uri = intent.data
                EPUB_FILE_NAME = getContentName(contentResolver, uri)!!
                Log.v("tag", "Content intent detected: ${intent.action} : ${intent.dataString} : ${intent.type} : $EPUB_FILE_NAME")
                val input = contentResolver.openInputStream(uri)
                input.toFile(PUBLICATION_PATH)

                parseAndShowEpub()

            } else if (intent.scheme.compareTo(ContentResolver.SCHEME_FILE) == 0) {
                val uri = intent.data
                EPUB_FILE_NAME = uri.lastPathSegment
                Log.v("tag", "File intent detected: ${intent.action} : ${intent.dataString} : ${intent.type} : $EPUB_FILE_NAME")
                val input = contentResolver.openInputStream(uri)
                input.toFile(PUBLICATION_PATH)
                val file = File(PUBLICATION_PATH)

                val pub = EpubParser().parse(PUBLICATION_PATH)
                if (pub != null) {
                    val publication = pub.publication
                    val book = Book(file.name, publication.metadata.title, publication.metadata.authors.get(0).name!!, file.absolutePath, books.size.toLong())
                    books.add(book)
                    booksAdapter.notifyDataSetChanged()

                }

                parseAndShowEpub()

            } else if (intent.scheme.compareTo("http") == 0) {
                val uri = intent.data
                EPUB_FILE_NAME = uri.lastPathSegment
                Log.v("tag", "HTTP intent detected: ${intent.action} : ${intent.dataString} : ${intent.type} : $EPUB_FILE_NAME")

                val progress = showProgress(this, null, getString(R.string.progress_wait_while_downloading_book))
                progress.show()
                val thread = Thread(Runnable {
                    try {
                        val input = java.net.URL(uri.toString()).openStream()
                        input.toFile(PUBLICATION_PATH)
                        runOnUiThread(Runnable {

                            val file = File(PUBLICATION_PATH)
                            val pub = EpubParser().parse(PUBLICATION_PATH)
                            if (pub != null) {
                                val publication = pub.publication
                                val book = Book(file.name, publication.metadata.title, publication.metadata.authors.get(0).name!!, file.absolutePath, books.size.toLong())
                                books.add(book)
                                booksAdapter.notifyDataSetChanged()

                            }


                            parseAndShowEpub()
                            progress.dismiss()
                        })
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                })
                thread.start()
            } else if (intent.scheme.compareTo("ftp") == 0) {
            }
        }
    }

    private fun copyEpubFromAssetsToStorage(epubFileName: String) {
        val input = assets.open(epubFileName)
        input.toFile(PUBLICATION_PATH)
        val file = File(PUBLICATION_PATH)
        val pub = EpubParser().parse(PUBLICATION_PATH)
        if (pub != null) {
            val publication = pub.publication
            val book = Book(file.name, publication.metadata.title, publication.metadata.authors.get(0).name!!, file.absolutePath, books.size.toLong())
            books.add(book)
            booksAdapter.notifyDataSetChanged()
        }
        parseAndShowEpub()
    }

    fun startServer() {
        if (!server.wasStarted()) {
            server.start()
            server.loadResources(assets)
        }
    }

    fun stopServer() {
        if (server.wasStarted()) {
            server.stop()
        }
    }

    private fun parseAndShowEpub() {

        val pub = EpubParser().parse(PUBLICATION_PATH)
        if (pub == null) {
            Toast.makeText(applicationContext, "Invalid ePub", Toast.LENGTH_SHORT).show()
            return
        }
        val publication = pub.publication
        val container = pub.container

        server.addEpub(publication, container, "/" + EPUB_FILE_NAME)

    }

    private fun getContentName(resolver: ContentResolver, uri: Uri): String? {
        val cursor = resolver.query(uri, null, null, null, null)
        cursor!!.moveToFirst()
        val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
        if (nameIndex >= 0) {
            val name = cursor.getString(nameIndex)
            cursor.close()
            return name
        } else {
            return null
        }
    }

    private fun showProgress(context: Context, title: String?, message: String?): ProgressDialog {

        val b = ProgressDialog(context)
        b.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.button_dismiss), DialogInterface.OnClickListener { dialogInterface, i ->
            dialogInterface.dismiss()
        })
        b.setMessage(message)
        b.setTitle(title)

        return b
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        this.permissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            var uri: Uri? = null
            if (data != null) {
                uri = data.data
                EPUB_FILE_NAME = getContentName(contentResolver, uri)!!
                val input = contentResolver.openInputStream(uri)
                input.toFile(PUBLICATION_PATH)

                val file = File(PUBLICATION_PATH)

                val pub = EpubParser().parse(PUBLICATION_PATH)
                if (pub != null) {
                    val publication = pub.publication
                    val book = Book(file.name, publication.metadata.title, publication.metadata.authors.get(0).name!!, file.absolutePath, books.size.toLong())
                    books.add(book)
                    booksAdapter.notifyDataSetChanged()

                }

                parseAndShowEpub()

            }
        } else if (requestCode == 2 && resultCode == Activity.RESULT_OK) {

            // existing epub selected through the list activity
            if (data != null) {

                EPUB_FILE_NAME = data.getStringExtra("name")

                parseAndShowEpub()

            }

        }
    }
}
