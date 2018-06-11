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
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.readium.r2.lcp.LcpHttpService
import org.readium.r2.lcp.LcpLicense
import org.readium.r2.lcp.LcpSession
import org.readium.r2.navigator.R2EpubActivity
import org.readium.r2.shared.Publication
import org.readium.r2.shared.drm.DRMMModel
import org.readium.r2.shared.drm.Drm
import org.readium.r2.streamer.Parser.EpubParser
import org.readium.r2.streamer.Parser.PubBox
import org.readium.r2.streamer.Server.BASE_URL
import org.readium.r2.streamer.Server.Server
import org.readium.r2.testapp.opds.GridAutoFitLayoutManager
import org.readium.r2.testapp.opds.OPDSListActivity
import org.readium.r2.testapp.permissions.PermissionHelper
import org.readium.r2.testapp.permissions.Permissions
import timber.log.Timber
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import java.io.IOException
import java.net.ServerSocket
import java.net.URL
import java.util.*

class CatalogActivity : AppCompatActivity(), BooksAdapter.RecyclerViewClickListener {

    private val TAG = this::class.java.simpleName

    private lateinit var server: Server
    private var localPort: Int = 0

    private lateinit var booksAdapter: BooksAdapter
    private lateinit var permissionHelper: PermissionHelper
    private lateinit var permissions: Permissions
    private lateinit var preferences: SharedPreferences
    private lateinit var R2TEST_DIRECTORY_PATH: String

    private var database: BooksDatabase = BooksDatabase(this)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catalog)

        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)

        val s = ServerSocket(0)
        s.localPort
        s.close()

        localPort = s.localPort
        server = Server(localPort)
        R2TEST_DIRECTORY_PATH = this.getExternalFilesDir(null).path + "/" //server.rootDir

        permissions = Permissions(this)
        permissionHelper = PermissionHelper(this, permissions)

        books = database.books.list()

        booksAdapter = BooksAdapter(this, books, "$BASE_URL:$localPort", this)
        catalogView.adapter = booksAdapter

        catalogView.layoutManager = GridAutoFitLayoutManager(act, 120)

        parseIntent();

    }

    override fun onResume() {
        super.onResume()
        booksAdapter.notifyDataSetChanged()
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
                if (!preferences.contains("samples")) {
                    val dir = File(R2TEST_DIRECTORY_PATH)
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                    copyEpubFromAssetsToStorage()
                    preferences.edit().putBoolean("samples", true).apply()
                }
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
        val uriString: String? = intent.getStringExtra(R2IntentHelper.URI)
        val lcp: Boolean = intent.getBooleanExtra(R2IntentHelper.LCP, false)
        if (uriString != null && lcp == false) {
            val uri: Uri? = Uri.parse(uriString)
            if (uri != null) {

                val progress = indeterminateProgressDialog(getString(R.string.progress_wait_while_downloading_book))
                progress.show()
                val thread = Thread(Runnable {
                    val fileName = UUID.randomUUID().toString()
                    val publicationPath = R2TEST_DIRECTORY_PATH + fileName
                    val path = RealPathUtil.getRealPathFromURI_API19(this, uri)

                    if (path != null) {
                        val input = File(path).toURL().openStream()
                        input.toFile(publicationPath)
                    }
                    else {
                        val input = java.net.URL(uri.toString()).openStream()
                        input.toFile(publicationPath)
                    }
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
        } else if (uriString != null && lcp == true) {
            val uri: Uri? = Uri.parse(uriString)
            if (uri != null) {
                val progress = indeterminateProgressDialog(getString(R.string.progress_wait_while_downloading_book))
                progress.show()
                val thread = Thread(Runnable {
                    val lcpLicense = LcpLicense(URL(uri.toString()), false, this)
                    task {
                        lcpLicense.fetchStatusDocument().get()
                    } then {
                        Log.i(TAG, "LCP fetchStatusDocument: $it")
                        lcpLicense.checkStatus()
                        lcpLicense.updateLicenseDocument().get()
                    } then {
                        Log.i(TAG, "LCP updateLicenseDocument: $it")
                        lcpLicense.areRightsValid()
                        lcpLicense.register()
                        lcpLicense.fetchPublication()
                    } then {
                        Log.i(TAG, "LCP fetchPublication: $it")
                        it?.let {
                            lcpLicense.moveLicense(it, lcpLicense.archivePath)
                        }
                        it!!
                    } successUi { path ->
                        val file = File(path)
                        try {
                            runOnUiThread(Runnable {
                                val parser = EpubParser()
                                val pub = parser.parse(path)
                                if (pub != null) {
                                    val pair = parser.parseRemainingResource(pub.container, pub.publication, pub.container.drm)
                                    pub.container = pair.first
                                    pub.publication = pair.second
                                    prepareToServe(parser, pub, file.name, file.absolutePath, true)
                                    progress.dismiss()

                                }
                            })
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    }
                })
                thread.start()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.opds -> {
                startActivity(intentFor<OPDSListActivity>())
                return false
            }
            R.id.about -> {
                startActivity(intentFor<R2AboutActivity>())
                return false
            }

            else -> return super.onOptionsItemSelected(item)
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
            } catch (e: IOException) {
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
            runOnUiThread {
                val publicationIdentifier = publication.metadata.identifier
                preferences.edit().putString("$publicationIdentifier-publicationPort", localPort.toString()).apply()
                val baseUrl = URL("$BASE_URL:$localPort" + "/" + fileName)
                val link = publication.uriTo(publication.coverLink, baseUrl)
                val author = authorName(publication)
                if (add) {
                    publication.coverLink?.href?.let {
                        val blob = ZipUtil.unpackEntry(File(absolutePath), it.removePrefix("/"))
                        blob?.let {
                            val book = Book(fileName, publication.metadata.title, author, absolutePath, books.size.toLong(), publication.coverLink?.href, publicationIdentifier, blob)
                            if (add) {
                                database.books.insert(book)?.let {
                                    books.add(book)
                                } ?: run {
                                    val test = "test"
                                }
                            }
                        }
                    } ?: run {
                        val book = Book(fileName, publication.metadata.title, author, absolutePath, books.size.toLong(), publication.coverLink?.href, publicationIdentifier, null)
                        if (add) {
                            database.books.insert(book)?.let {
                                books.add(book)
                            } ?: run {
                                val test = "test"
                            }
                        }
                    }
                }
                booksAdapter.notifyDataSetChanged()
            }


            server.addEpub(publication, container, "/" + fileName)
            addBookToView()
        }
    }

    override fun recyclerViewListLongClicked(v: View, position: Int) {
        val layout = LayoutInflater.from(this).inflate(R.layout.popup_delete, catalogView, false) //Inflating the layout
        val popup = PopupWindow(this)
        popup.setContentView(layout)
        popup.setWidth(ListPopupWindow.WRAP_CONTENT)
        popup.setHeight(ListPopupWindow.WRAP_CONTENT)
        popup.isOutsideTouchable = true
        popup.isFocusable = true
        popup.showAsDropDown(v, 24, -350)
        val delete: Button = layout.findViewById(R.id.delete) as Button
        delete.setOnClickListener {
            val book = books[position]
            val publicationPath = R2TEST_DIRECTORY_PATH + book.fileName
            books.remove(book)
            booksAdapter.notifyDataSetChanged()
            val file = File(publicationPath)
            file.delete()
            popup.dismiss()
            database.books.delete(book)
        }
    }

    override fun recyclerViewListClicked(v: View, position: Int) {
        val progress = indeterminateProgressDialog(getString(R.string.progress_wait_while_preparing_book))
        progress.show()
        task {
            val book = books[position]
            val publicationPath = R2TEST_DIRECTORY_PATH + book.fileName
            val file = File(publicationPath)
            val parser = EpubParser()
            val pub = parser.parse(publicationPath)
            if (pub != null) {
                prepareToServe(parser, pub, book.fileName, file.absolutePath, false)
                val publication = pub.publication
                if (publication.spine.size > 0) {
                    pub.container.drm?.let { drm ->
                        if (drm.brand == Drm.Brand.lcp) {
                            handleLcpPublication(publicationPath, drm, {
                                val pair = parser.parseRemainingResource(pub.container, publication, it)
                                pub.container = pair.first
                                pub.publication = pair.second
                            }, {
                                if (supportedProfiles.contains(it.profile)) {
                                    server.addEpub(publication, pub.container, "/" + book.fileName)
                                    Log.i(TAG, "handle lcp done")

                                    val license = (drm.license as LcpLicense)
                                    val drmModel = DRMMModel(drm.brand.name,
                                            license.currentStatus(),
                                            license.provider().toString(),
                                            DateTime(license.issued()).toString(DateTimeFormat.shortDateTime()),
                                            DateTime(license.lastUpdate()).toString(DateTimeFormat.shortDateTime()),
                                            DateTime(license.rightsStart()).toString(DateTimeFormat.shortDateTime()),
                                            DateTime(license.rightsEnd()).toString(DateTimeFormat.shortDateTime()),
                                            license.rightsPrints().toString(),
                                            license.rightsCopies().toString())

                                    startActivity(intentFor<R2EpubActivity>("publicationPath" to publicationPath, "epubName" to book.fileName, "publication" to publication, "drmModel" to drmModel))
                                } else {
                                    alert(Appcompat, "The profile of this DRM is not supported.") {
                                        negativeButton("Ok") { }
                                    }.show()
                                }
                            }, {
                                // Do nothing
                            }).get()

                        }
                    } ?: run {
                        startActivity(intentFor<R2EpubActivity>("publicationPath" to publicationPath, "epubName" to book.fileName, "publication" to publication))
                    }
                }
            }
        } then {
            progress.dismiss()
        }
    }

    private fun handleLcpPublication(publicationPath: String, drm: Drm, parsingCallback: (drm: Drm) -> Unit, callback: (drm: Drm) -> Unit, callbackUI: () -> Unit): Promise<Unit, Exception> {

        val lcpHttpService = LcpHttpService()
        val session = LcpSession(publicationPath, this)

        fun validatePassphrase(passphraseHash: String): Promise<LcpLicense, Exception> {
            return task {
                lcpHttpService.certificateRevocationList("http://crl.edrlab.telesec.de/rl/EDRLab_CA.crl").get()
            } then { pemCrtl ->
                session.resolve(passphraseHash, pemCrtl).get()
            }
        }

        fun promptPassphrase(reason: String? = null, callback: (pass: String) -> Unit) {
            runOnUiThread {
                val hint = session.getHint()
                alert(Appcompat, hint, reason ?: "LCP Passphrase") {
                    var editText: EditText? = null
                    customView {
                        verticalLayout {
                            textInputLayout {
                                editText = editText { }
                            }
                        }
                    }
                    positiveButton("OK") {
                        task {
                            editText!!.text.toString()
                        } then { clearPassphrase ->
                            val passphraseHash = HASH.sha256(clearPassphrase)
                            session.checkPassphrases(listOf(passphraseHash))
                        } then { validPassphraseHash ->
                            session.storePassphrase(validPassphraseHash)
                            callback(validPassphraseHash)
                        }
                    }
                    negativeButton("Cancel") { }
                }.show()
            }
        }

        return task {
            val passphrases = session.passphraseFromDb()
            passphrases?.let {
                val lcpLicense = validatePassphrase(it).get()
                drm.license = lcpLicense
                drm.profile = session.getProfile()
                parsingCallback(drm)
                callback(drm)
            } ?: run {
                promptPassphrase(null, {
                    val lcpLicense = validatePassphrase(it).get()
                    drm.license = lcpLicense
                    drm.profile = session.getProfile()
                    parsingCallback(drm)
                    callback(drm)
                    callbackUI()
                })
            }
        }
    }

}
