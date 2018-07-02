package org.readium.r2.testapp


// uncomment for lcp
/*
import org.readium.r2.lcp.LcpHttpService
import org.readium.r2.lcp.LcpLicense
import org.readium.r2.lcp.LcpSession
 */

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.view.*
import android.webkit.URLUtil
import android.support.v7.widget.RecyclerView
import android.widget.Button
import android.widget.EditText
import android.widget.ListPopupWindow
import android.widget.PopupWindow
import com.github.kittinunf.fuel.Fuel
import com.mcxiaoke.koi.ext.onClick
import net.theluckycoder.materialchooser.Chooser
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.jetbrains.anko.design.coordinatorLayout
import org.jetbrains.anko.design.floatingActionButton
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.design.textInputLayout
import org.json.JSONObject
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.readium.r2.navigator.R2EpubActivity
import org.readium.r2.opds.OPDS2Parser
import org.readium.r2.opds.OPDSParser
import org.readium.r2.shared.Publication
import org.readium.r2.shared.drm.Drm
import org.readium.r2.shared.opds.ParseData
import org.readium.r2.shared.promise
import org.readium.r2.streamer.Parser.EpubParser
import org.readium.r2.streamer.Parser.PubBox
import org.readium.r2.streamer.Server.BASE_URL
import org.readium.r2.streamer.Server.Server
import org.readium.r2.testapp.opds.GridAutoFitLayoutManager
import org.readium.r2.testapp.opds.OPDSDownloader
import org.readium.r2.testapp.opds.OPDSListActivity
import org.readium.r2.testapp.opds.OPDSModel
import org.readium.r2.testapp.permissions.PermissionHelper
import org.readium.r2.testapp.permissions.Permissions
import org.zeroturnaround.zip.ZipUtil
import org.zeroturnaround.zip.commons.IOUtils
import timber.log.Timber
import java.io.*
import java.net.HttpURLConnection
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

    lateinit var database: BooksDatabase
    lateinit var opdsDownloader: OPDSDownloader
    lateinit var publication: Publication
    
    private lateinit var catalogView: RecyclerView
    private lateinit var alertDialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)

        val s = ServerSocket(0)
        s.localPort
        s.close()

        localPort = s.localPort
        server = Server(localPort)
        R2TEST_DIRECTORY_PATH = this.getExternalFilesDir(null).path + "/"

        permissions = Permissions(this)
        permissionHelper = PermissionHelper(this, permissions)

        opdsDownloader = OPDSDownloader(this)
        database = BooksDatabase(this)
        books = database.books.list()

        booksAdapter = BooksAdapter(this, books, "$BASE_URL:$localPort", this)

        parseIntent(null);

        coordinatorLayout {
            lparams {
                topMargin = dip(8)
                bottomMargin = dip(8)
                padding = dip(0)
                width = matchParent
                height = matchParent
            }

            catalogView = recyclerView {
                layoutManager = GridAutoFitLayoutManager(act, 120)
                adapter = booksAdapter

                lparams {
                    elevation = 2F
                    width = matchParent
                }

                addItemDecoration(VerticalSpaceItemDecoration(10))

            }
           
            floatingActionButton {
                imageResource = R.drawable.icon_plus_white
                onClick {
                    
                    alertDialog = alert(Appcompat, "Add an ePub to your library") {
                        customView {
                            verticalLayout {
                                lparams {
                                    bottomPadding = dip(16)
                                }
                                button {
                                    text =  "select from your device"
                                    onClick {
                                        alertDialog.dismiss()
                                        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
                                        // browser.
                                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)

                                        // Filter to only show results that can be "opened", such as a
                                        // file (as opposed to a list of contacts or timezones)
                                        intent.addCategory(Intent.CATEGORY_OPENABLE)

                                        // Filter to show only epubs, using the image MIME data type.
                                        // To search for all documents available via installed storage providers,
                                        // it would be "*/*".
                                        intent.type = "application/epub+zip"

                                        startActivityForResult(intent, 1)

                                    }
                                }
                                button {
                                    text =  "download from a url"
                                    onClick {
                                        alertDialog.dismiss()
                    
                    var editTextHref: EditText? = null
                    alert (Appcompat, "Add a publication from URL") {

                        customView {
                            verticalLayout {
                                textInputLayout {
                                    padding = dip(10)
                                    editTextHref = editText {
                                        hint = "URL"
                                    }
                                }
                            }
                        }
                        positiveButton("Add") { }
                        negativeButton("Cancel") { }

                    }.build().apply {
                        setCancelable(false)
                        setCanceledOnTouchOutside(false)
                        setOnShowListener(DialogInterface.OnShowListener {
                            val b = getButton(AlertDialog.BUTTON_POSITIVE)
                            b.setOnClickListener(View.OnClickListener {
                                if (TextUtils.isEmpty(editTextHref!!.text)) {
                                    editTextHref!!.setError("Please Enter A URL.");
                                    editTextHref!!.requestFocus();
                                } else if (!URLUtil.isValidUrl(editTextHref!!.text.toString())) {
                                    editTextHref!!.setError("Please Enter A Valid URL.");
                                    editTextHref!!.requestFocus();
                                } else {
                                    var parseData: Promise<ParseData, Exception>? = null
                                    parseData = parseURL(URL(editTextHref!!.text.toString()))
                                    parseData.successUi {
                                        val opds = OPDSModel(
                                                "title",
                                                editTextHref!!.text.toString(),
                                                it.type)
                                        publication = it.publication ?: return@successUi
                                        val downloadUrl = getDownloadURL(publication)!!.toString()
                                        opdsDownloader.publicationUrl(downloadUrl).successUi { pair ->

                                            val publicationIdentifier = publication.metadata.identifier
                                            val author = authorName(publication)
                                            task {
                                                getBitmapFromURL(publication.images.first().href!!)
                                            }.then {
                                                val bitmap = it
                                                val stream = ByteArrayOutputStream()
                                                bitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream)

                                                val book = Book(pair.second, publication.metadata.title, author, pair.first, -1.toLong(), publication.coverLink?.href, publicationIdentifier, stream.toByteArray())

                                                database.books.insert(book, false)?.let {
                                                    books.add(book)
                                                } ?: run {
                                                    alert (Appcompat, "Publication already exists") {

                                                        positiveButton("Add anyways") { }
                                                        negativeButton("Cancel") { }

                                                    }.build().apply {
                                                        setCancelable(false)
                                                        setCanceledOnTouchOutside(false)
                                                        setOnShowListener(DialogInterface.OnShowListener {
                                                            val b = getButton(AlertDialog.BUTTON_POSITIVE)
                                                            b.setOnClickListener(View.OnClickListener {
                                                                database.books.insert(book, true)?.let {
                                                                    books.add(book)
                                                                    dismiss()
                                                                    booksAdapter.notifyDataSetChanged()
                                                                }
                                                            })
                                                        })
                                                    }.show()
                                                }
                                            }
                                        }
                                        dismiss()
                                    }
                                    parseData.failUi {
                                        editTextHref!!.setError("Please Enter A Valid OPDS Book URL.");
                                        editTextHref!!.requestFocus();
                                    }
                                }
                            })
                        })

                    }.show()
                                        
                                    }
                                }
                            }
                        }
                    }.show()                                        
                                        
                }
            }.lparams {
                gravity = Gravity.END or Gravity.BOTTOM
                margin = dip(16)
            }
        }

    }

    private fun parseURL(url: URL) : Promise<ParseData, Exception> {
        return Fuel.get(url.toString(),null).promise() then {
            val (request, response, result) = it
            if (isJson(result)) {
                OPDS2Parser.parse(result, url)
            } else {
                OPDSParser.parse(result, url)
            }
        }
    }

    private fun isJson(byteArray: ByteArray) : Boolean {
        return try {
            JSONObject(String(byteArray))
            true
        } catch(e: Exception){
            false
        }
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


    private fun parseIntent(filePath: String?) {

        if (filePath != null) {
            val progress = indeterminateProgressDialog(getString(R.string.progress_wait_while_downloading_book))
            progress.show()

            val fileName = UUID.randomUUID().toString()
            val publicationPath = R2TEST_DIRECTORY_PATH + fileName

            copyFile(File(filePath), File(publicationPath))
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
        } else {
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
                            copyFile(File(path), File(publicationPath))
                        } else {
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

            }

            // uncomment for lcp
            /*
                else if (uriString != null && lcp == true) {
                    val uri: Uri? = Uri.parse(uriString)
                    if (uri != null) {
                        val progress = indeterminateProgressDialog(getString(R.string.progress_wait_while_downloading_book))
                        progress.show()
                        val thread = Thread(Runnable {
                            val lcpLicense = LcpLicense(URL(uri.toString()), false, this)
                            task {
                                lcpLicense.fetchStatusDocument().get()
                            } then {
                                Timber.i(TAG, "LCP fetchStatusDocument: $it")
                                lcpLicense.checkStatus()
                                lcpLicense.updateLicenseDocument().get()
                            } then {
                                Timber.i(TAG, "LCP updateLicenseDocument: $it")
                                lcpLicense.areRightsValid()
                                lcpLicense.register()
                                lcpLicense.fetchPublication()
                            } then {
                                Timber.i(TAG, "LCP fetchPublication: $it")
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
    //        */
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
            server.loadResources(assets, applicationContext)
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

    fun copyFile(src: File, dst: File) {
        var `in`: InputStream? = null
        var out: OutputStream? = null
        try {
            `in` = FileInputStream(src)
            out = FileOutputStream(dst)
            IOUtils.copy(`in`, out)
        } catch (ioe: IOException) {
            Timber.e(ioe)
        } finally {
            IOUtils.closeQuietly(out)
            IOUtils.closeQuietly(`in`)
        }
    }

    private fun prepareToServe(parser: EpubParser, pub: PubBox?, fileName: String, absolutePath: String, add: Boolean) {
        if (pub == null) {
            snackbar(catalogView, "Invalid ePub")
            return
        }
        val publication = pub.publication
        val container = pub.container

        fun addBookToView() {
            runOnUiThread {
                val publicationIdentifier = publication.metadata.identifier
                preferences.edit().putString("$publicationIdentifier-publicationPort", localPort.toString()).apply()
                val author = authorName(publication)
                if (add) {
                    publication.coverLink?.href?.let {
                        val blob = ZipUtil.unpackEntry(File(absolutePath), it.removePrefix("/"))
                        blob?.let {
                            val book = Book(fileName, publication.metadata.title, author, absolutePath, books.size.toLong(), publication.coverLink?.href, publicationIdentifier, blob)
                            if (add) {
                                database.books.insert(book, false)?.let {
                                    books.add(book)
                                } ?: run {
                                    alert (Appcompat, "Publication already exists") {

                                        positiveButton("Add anyways") { }
                                        negativeButton("Cancel") { }

                                    }.build().apply {
                                        setCancelable(false)
                                        setCanceledOnTouchOutside(false)
                                        setOnShowListener(DialogInterface.OnShowListener {
                                            val b = getButton(AlertDialog.BUTTON_POSITIVE)
                                            b.setOnClickListener(View.OnClickListener {
                                                database.books.insert(book, true)?.let {
                                                    books.add(book)
                                                    dismiss()
                                                    booksAdapter.notifyDataSetChanged()
                                                }
                                            })
                                        })
                                    }.show()
                                }
                            }
                        }
                    } ?: run {
                        val book = Book(fileName, publication.metadata.title, author, absolutePath, books.size.toLong(), publication.coverLink?.href, publicationIdentifier, null)
                        if (add) {
                            database.books.insert(book, false)?.let {
                                books.add(book)
                            } ?: run {
                                alert (Appcompat, "Publication already exists") {

                                    positiveButton("Add anyways") { }
                                    negativeButton("Cancel") { }

                                }.build().apply {
                                    setCancelable(false)
                                    setCanceledOnTouchOutside(false)
                                    setOnShowListener(DialogInterface.OnShowListener {
                                        val b = getButton(AlertDialog.BUTTON_POSITIVE)
                                        b.setOnClickListener(View.OnClickListener {
                                            database.books.insert(book, true)?.let {
                                                books.add(book)
                                                dismiss()
                                                booksAdapter.notifyDataSetChanged()
                                            }
                                        })
                                    })
                                }.show()
                            }
                        }
                    }
                }
                booksAdapter.notifyDataSetChanged()
                server.addEpub(publication, container, "/" + fileName, applicationContext.getExternalFilesDir(null).path + "/styles/UserProperties.json")
            }
        }
        addBookToView()
    }

    override fun recyclerViewListLongClicked(v: View, position: Int) {
        val layout = LayoutInflater.from(this).inflate(R.layout.popup_delete, catalogView, false) //Inflating the layout
        val popup = PopupWindow(this)
        popup.setContentView(layout)
        popup.setWidth(ListPopupWindow.WRAP_CONTENT)
        popup.setHeight(ListPopupWindow.WRAP_CONTENT)
        popup.isOutsideTouchable = true
        popup.isFocusable = true
        popup.showAsDropDown(v, 24, -350, Gravity.CENTER)
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
                    pub.container.drm?.let { drm: Drm ->
                        if (drm.brand == Drm.Brand.lcp) {
                            // uncomment for lcp
                            /*
                            handleLcpPublication(publicationPath, drm, {
                                val pair = parser.parseRemainingResource(pub.container, publication, it)
                                pub.container = pair.first
                                pub.publication = pair.second
                            }, {
                                if (supportedProfiles.contains(it.profile)) {
                                    server.addEpub(publication, pub.container, "/" + book.fileName)
                                    Timber.i(TAG, "handle lcp done")

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

                            */
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

    // uncomment for lcp
/*
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
*/

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data ?: return


        // The document selected by the user won't be returned in the intent.
        // Instead, a URI to that document will be contained in the return intent
        // provided to this method as a parameter.
        // Pull that URI using resultData.getData().
        if (requestCode == 1 && resultCode == RESULT_OK) {

            val progress = indeterminateProgressDialog(getString(R.string.progress_wait_while_downloading_book))
            progress.show()

            val uri: Uri?
            uri = data.data

            val fileName = UUID.randomUUID().toString()
            val publicationPath = R2TEST_DIRECTORY_PATH + fileName

            val input = contentResolver.openInputStream(uri)
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

        } else if (resultCode == RESULT_OK) {
            val filePath = data.getStringExtra(Chooser.RESULT_PATH)
            parseIntent(filePath)
        }
    }

}

class VerticalSpaceItemDecoration(private val verticalSpaceHeight: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView,
                                state: RecyclerView.State) {
        outRect.bottom = verticalSpaceHeight
    }
}
