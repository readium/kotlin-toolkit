package org.readium.r2.testapp

import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentResolver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.GridView
import android.widget.Toast
import org.readium.r2.navigator.R2EpubActivity
import org.readium.r2.shared.Publication
import org.readium.r2.streamer.Parser.EpubParser
import org.readium.r2.streamer.Server.Server
import org.readium.r2.testapp.permissions.PermissionHelper
import org.readium.r2.testapp.permissions.Permissions
import java.io.File
import java.io.InputStream

class CatalogActivity : AppCompatActivity() {

    private var books = arrayListOf<Book>()
    val r2test_directory_path = Environment.getExternalStorageDirectory().path + "/r2test/"
    var epub_name = "dummy.epub"
    var publication_path: String = r2test_directory_path + epub_name

    val server = Server()
    var publi: Publication? = null

    private lateinit var booksAdapter: BooksAdapter
    private lateinit var permissionHelper: PermissionHelper
    private lateinit var permissions: Permissions

    override fun onStart() {
        super.onStart()

        permissionHelper.storagePermission {

            val prefs = getPreferences(Context.MODE_PRIVATE)
            if (!prefs.contains("dummy")) {
                val dir = File(R2TEST_DIRECTORY_PATH)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                copyEpubFromAssetsToSdCard(EPUB_FILE_NAME)
                prefs.edit().putBoolean("dummy",true).apply()
            }

        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catalog)


        permissions = Permissions(this)
        permissionHelper = PermissionHelper(this, permissions)

        startServer()

        val r2test_path = Environment.getExternalStorageDirectory().path + "/r2test/"
        val listOfFiles = File(r2test_path).listFilesSafely()
        for (i in listOfFiles.indices) {
            val file = listOfFiles.get(i)
            val local_epub_path: String = r2test_directory_path + file.name
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

        val gridView = findViewById<View>(R.id.gridview) as GridView
        booksAdapter = BooksAdapter(this, books)
        gridView.adapter = booksAdapter


        gridView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val book = books[position]

            epub_name = book.fileName
            val local_epub_path: String = r2test_directory_path + epub_name
            publication_path = local_epub_path

            parseAndShowEpub()

            val pub = EpubParser().parse(publication_path)
            if (pub != null) {
                val publication = pub.publication
                publi = publication
                if (publication.spine.size > 0) {
                    val intent = Intent(this, R2EpubActivity::class.java)
                    intent.putExtra("publication_path", publication_path)
                    intent.putExtra("epub_name", epub_name)
                    intent.putExtra("publication", publication)
                    startActivity(intent)
                }
            }
        }


        if (intent.action.compareTo(Intent.ACTION_VIEW) == 0) {

            if (intent.scheme.compareTo(ContentResolver.SCHEME_CONTENT) == 0) {
                val uri = intent.data
                epub_name = getContentName(contentResolver, uri)!!
                Log.v("tag", "Content intent detected: " + intent.action + " : " + intent.dataString + " : " + intent.type + " : " + epub_name)
                val input = contentResolver.openInputStream(uri)
                val local_epub_path: String = r2test_directory_path + epub_name

                publication_path = local_epub_path
                input.toFile(local_epub_path)

                parseAndShowEpub()

            } else if (intent.scheme.compareTo(ContentResolver.SCHEME_FILE) == 0) {
                val uri = intent.data
                epub_name = uri.lastPathSegment
                Log.v("tag", "File intent detected: " + intent.action + " : " + intent.dataString + " : " + intent.type + " : " + epub_name)
                val input = contentResolver.openInputStream(uri)
                val local_path: String = r2test_directory_path + epub_name

                publication_path = local_path
                input.toFile(local_path)
                val file = File(local_path)

                val pub = EpubParser().parse(local_path)
                if (pub != null) {
                    val publication = pub.publication
                    val book = Book(file.name, publication.metadata.title, publication.metadata.authors.get(0).name!!, file.absolutePath, books.size.toLong())
                    books.add(book)
                    booksAdapter.notifyDataSetChanged()

                }

                parseAndShowEpub()

            } else if (intent.scheme.compareTo("http") == 0) {
                val uri = intent.data
                epub_name = uri.lastPathSegment
                Log.v("tag", "HTTP intent detected: " + intent.action + " : " + intent.dataString + " : " + intent.type + " : " + epub_name)
                val local_epub_path: String = r2test_directory_path + epub_name

                publication_path = local_epub_path

                val progress = showProgress(this, null, getString(R.string.progress_wait_while_downloading_book))
                progress.show()
                val thread = Thread(Runnable {
                    try {
                        val input = java.net.URL(uri.toString()).openStream()
                        input.toFile(local_epub_path)
                        runOnUiThread(Runnable {

                            val file = File(local_epub_path)
                            val pub = EpubParser().parse(local_epub_path)
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
                // TODO Import from FTP!
            }
        }
    }

    private fun copyEpubFromAssetsToSdCard(epubFileName: String) {
        val input = assets.open(epubFileName)
        input.toFile(publication_path)
        val file = File(publication_path)
        val pub = EpubParser().parse(publication_path)
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

        val pub = EpubParser().parse(publication_path)
        if (pub == null) {
            Toast.makeText(applicationContext, "Invalid Epub", Toast.LENGTH_SHORT).show()
            return
        }
        val publication = pub.publication
        val container = pub.container
        publi = publication

        server.addEpub(publication, container, "/" + epub_name)

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

    private fun InputStream.toFile(path: String) {
        use { input ->
            File(path).outputStream().use { input.copyTo(it) }
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

    override fun onDestroy() {
        super.onDestroy()
        server.stop()

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        this.permissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }



    public override fun onActivityResult(requestCode: Int, resultCode: Int,
                                         data: Intent?) {

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
                epub_name = getContentName(contentResolver, uri)!!
                val input = contentResolver.openInputStream(uri)
                val local_epub_path: String = r2test_directory_path + epub_name

                publication_path = local_epub_path
                input.toFile(local_epub_path)

                val file = File(local_epub_path)

                val pub = EpubParser().parse(local_epub_path)
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

                epub_name = data.getStringExtra("name")
                val local_epub_path: String = r2test_directory_path + epub_name
                publication_path = local_epub_path

                parseAndShowEpub()

            }

        }
    }
}
