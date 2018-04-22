package org.readium.r2.testapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import com.mcxiaoke.koi.HASH
import kotlinx.android.synthetic.main.activity_catalog.*
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import nl.komponents.kovenant.ui.successUi
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.jetbrains.anko.design.textInputLayout
import org.readium.r2.navigator.R2EpubActivity
import org.readium.r2.shared.Drm
import org.readium.r2.shared.Publication
import org.readium.r2.streamer.Parser.EpubParser
import org.readium.r2.streamer.Parser.PubBox
import org.readium.r2.streamer.Server.BASE_URL
import org.readium.r2.streamer.Server.Server
import org.readium.r2.testapp.permissions.PermissionHelper
import org.readium.r2.testapp.permissions.Permissions
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.net.ServerSocket
import java.net.URL
import java.util.*

val supportedProfiles = arrayListOf("http://readium.org/lcp/basic-profile", "http://readium.org/lcp/profile-1.0")

class CatalogActivity : AppCompatActivity() {

    private val TAG = this::class.java.simpleName

    private lateinit var server:Server
    private var localPort:Int = 0

    private lateinit var books:ArrayList<Book>
    private lateinit var booksAdapter: BooksAdapter
    private lateinit var permissionHelper: PermissionHelper
    private lateinit var permissions: Permissions
    private lateinit var preferences: SharedPreferences
    private lateinit var R2TEST_DIRECTORY_PATH:String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catalog)

        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)

        val s = ServerSocket(0)
        s.localPort
        s.close()

        localPort = s.localPort
        server = Server(localPort)
        R2TEST_DIRECTORY_PATH = server.rootDir

        permissions = Permissions(this)
        permissionHelper = PermissionHelper(this, permissions)

        books = arrayListOf<Book>()
        booksAdapter = BooksAdapter(this, books)
        catalogView.adapter = booksAdapter

        catalogView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val book = books[position]

            val publicationPath = R2TEST_DIRECTORY_PATH + book.fileName
            val file = File(publicationPath)
            val parser = EpubParser()
            val pub = parser.parse(publicationPath)

            if (pub != null) {
                prepareToServe(parser, pub, book.fileName, file.absolutePath, false)
                val publication = pub.publication
                if (publication.spine.size > 0) {


                        val intent = Intent(this, R2EpubActivity::class.java)
                        intent.putExtra("publicationPath", publicationPath)
                        intent.putExtra("epubName", book.fileName)
                        intent.putExtra("publication", publication)
                        startActivity(intent)

                }
            }
        }

        catalogView.setOnItemLongClickListener { parent, view, position, _ ->

            val layoutInflater = LayoutInflater.from(this)
            val layout = layoutInflater.inflate(R.layout.popup_delete,  parent, false)

            val popup = PopupWindow(this)
            popup.setContentView(layout)
            popup.setWidth(ListPopupWindow.WRAP_CONTENT)
            popup.setHeight(ListPopupWindow.WRAP_CONTENT)
            popup.isOutsideTouchable = true
            popup.isFocusable = true
            popup.showAsDropDown(view, 24, -350)

            val delete: Button = layout.findViewById(R.id.delete) as Button

            delete.setOnClickListener {
                val book = books[position]
                val publicationPath = R2TEST_DIRECTORY_PATH + book.fileName
                books.remove(book)
                booksAdapter.notifyDataSetChanged()
                val file = File(publicationPath)
                file.delete()
                popup.dismiss()
            }

            true
        }

        parseIntent();

    }

    override fun onStart() {
        super.onStart()

        startServer()

        permissionHelper.storagePermission {
            if (books.isEmpty()) {
                val listOfFiles = File(R2TEST_DIRECTORY_PATH).listFilesSafely()
                for (i in listOfFiles.indices) {
                    val file = listOfFiles.get(i)
                    val publicationPath = R2TEST_DIRECTORY_PATH + file.name
                    val parser = EpubParser()
                    val pub = parser.parse(publicationPath)
                    if (pub != null) {
                        prepareToServe(parser, pub, file.name, file.absolutePath, true)
                    }
                }
            }

            if (!preferences.contains("samples")) {
                val dir = File(R2TEST_DIRECTORY_PATH)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                copyEpubFromAssetsToStorage()
                preferences.edit().putBoolean("samples",true).apply()
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //TODO not sure if this is needed
        stopServer()
    }


    private fun parseIntent() {
        val intent = intent
        val uriString:String? = intent.getStringExtra(R2IntentHelper.URI)
        val lcp:Boolean = intent.getBooleanExtra(R2IntentHelper.LCP, false)
        if (uriString != null && lcp == false) {
            val uri: Uri? = Uri.parse(uriString)
            if (uri != null) {

                val progress = indeterminateProgressDialog(getString(R.string.progress_wait_while_downloading_book))
                progress.show()
                val thread = Thread(Runnable {
                    val fileName = UUID.randomUUID().toString()
                    val publicationPath = R2TEST_DIRECTORY_PATH + fileName

                    val input = java.net.URL(uri.toString()).openStream()
                    input.toFile(publicationPath)

                    val file = File(publicationPath)

                    try {
                        runOnUiThread(Runnable {
                            val parser = EpubParser()
                            val pub = parser.parse(publicationPath)
                            if (pub != null) {
                                prepareToServe(parser, pub, fileName, file.absolutePath, true)
                                progress.dismiss()
                            }
                        })
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                })
                thread.start()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        this.permissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun startServer() {
        if (!server.isAlive()) {
            try {
                server.start()
            }
            catch (e: IOException) {
                // do nothing
                Timber.e(e)
            }
            server.loadResources(assets)
        }
    }

    fun stopServer() {
        if (server.isAlive()) {
            server.stop()
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

    private fun copyEpubFromAssetsToStorage() {
        val list = assets.list("Samples")
        for (file_name in list) {
            val input = assets.open("Samples/" + file_name)
            val fileName = UUID.randomUUID().toString()
            val publicationPath = R2TEST_DIRECTORY_PATH + fileName
            input.toFile(publicationPath)
            val parser = EpubParser()
            val file = File(publicationPath)
            val pub = parser.parse(publicationPath)
            if (pub != null) {
                prepareToServe(parser, pub, fileName, file.absolutePath, true)
            }
        }
    }

    private fun prepareToServe(parser: EpubParser, pub: PubBox?, fileName: String, absolutePath: String, add: Boolean) {
        if (pub == null) {
            Toast.makeText(applicationContext, "Invalid ePub", Toast.LENGTH_SHORT).show()
            return
        }
        val publicationPath = R2TEST_DIRECTORY_PATH + fileName
        val publication = pub.publication
        val container = pub.container

        fun addBookToView() {
            runOnUiThread{
                val publicationIdentifier = publication.metadata.identifier
                preferences.edit().putString("$publicationIdentifier-publicationPort", localPort.toString()).apply()
                val baseUrl = URL("$BASE_URL:$localPort" + "/" + fileName)
                val link = publication.uriTo(publication.coverLink, baseUrl)
                val author = authorName(publication)
                val book = Book(fileName, publication.metadata.title, author, absolutePath, books.size.toLong(), link)
                if (add) {
                    books.add(book)
                }
                booksAdapter.notifyDataSetChanged()
            }
        }

            server.addEpub(publication, container, "/" + fileName)
            addBookToView()

    }

}
