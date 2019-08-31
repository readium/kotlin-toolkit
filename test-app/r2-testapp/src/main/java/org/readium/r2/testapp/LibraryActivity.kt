/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mostapha Idoubihi, Paul Stoica
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp


import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.text.TextUtils
import android.view.*
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.widget.Button
import android.widget.EditText
import android.widget.ListPopupWindow
import android.widget.PopupWindow
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.github.kittinunf.fuel.Fuel
import com.mcxiaoke.koi.ext.close
import com.mcxiaoke.koi.ext.onClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.jetbrains.anko.design.*
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.json.JSONObject
import org.readium.r2.navigator.R2CbzActivity
import org.readium.r2.opds.OPDS1Parser
import org.readium.r2.opds.OPDS2Parser
import org.readium.r2.shared.Injectable
import org.readium.r2.shared.Publication
import org.readium.r2.shared.drm.DRM
import org.readium.r2.shared.opds.ParseData
import org.readium.r2.shared.parsePublication
import org.readium.r2.shared.promise
import org.readium.r2.streamer.parser.AudioBookParser
import org.readium.r2.streamer.parser.CbzParser
import org.readium.r2.streamer.parser.EpubParser
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.streamer.server.BASE_URL
import org.readium.r2.streamer.server.Server
import org.readium.r2.testapp.audiobook.AudiobookActivity
import org.readium.r2.testapp.drm.LCPLibraryActivityService
import org.readium.r2.testapp.opds.GridAutoFitLayoutManager
import org.readium.r2.testapp.opds.OPDSDownloader
import org.readium.r2.testapp.opds.OPDSListActivity
import org.readium.r2.testapp.permissions.PermissionHelper
import org.readium.r2.testapp.permissions.Permissions
import org.zeroturnaround.zip.ZipUtil
import org.zeroturnaround.zip.commons.IOUtils
import timber.log.Timber
import java.io.*
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URI
import java.net.URL
import java.nio.charset.Charset
import java.util.*
import kotlin.coroutines.CoroutineContext

@SuppressLint("Registered")
open class LibraryActivity : AppCompatActivity(), BooksAdapter.RecyclerViewClickListener, LCPLibraryActivityService, CoroutineScope {


    /**
     * Context of this scope.
     */
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    protected lateinit var server: Server
    private var localPort: Int = 0

    private lateinit var booksAdapter: BooksAdapter
    private lateinit var permissionHelper: PermissionHelper
    private lateinit var permissions: Permissions
    private lateinit var preferences: SharedPreferences
    private lateinit var R2DIRECTORY: String

    private lateinit var database: BooksDatabase
    private lateinit var opdsDownloader: OPDSDownloader

    private lateinit var positionsDB: PositionsDatabase

    protected lateinit var catalogView: androidx.recyclerview.widget.RecyclerView
    private lateinit var alertDialog: AlertDialog

    protected var listener: LibraryActivity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)

        val s = ServerSocket(0)
        s.localPort
        s.close()

        localPort = s.localPort
        server = Server(localPort)

        val properties = Properties();
        val inputStream = this.assets.open("configs/config.properties");
        properties.load(inputStream);
        val useExternalFileDir = properties.getProperty("useExternalFileDir", "false")!!.toBoolean()

        R2DIRECTORY = if (useExternalFileDir) {
            this.getExternalFilesDir(null)?.path + "/"
        } else {
            this.filesDir.path + "/"
        }

        permissions = Permissions(this)
        permissionHelper = PermissionHelper(this, permissions)

        opdsDownloader = OPDSDownloader(this)
        database = BooksDatabase(this)
        books = database.books.list()

        positionsDB = PositionsDatabase(this)

        booksAdapter = BooksAdapter(this, books, "$BASE_URL:$localPort", this)

        parseIntent(null)


        coordinatorLayout {
            lparams {
                topMargin = dip(8)
                bottomMargin = dip(8)
                padding = dip(0)
                width = matchParent
                height = matchParent
            }

            catalogView = recyclerView {
                layoutManager = GridAutoFitLayoutManager(this@LibraryActivity, 120)
                adapter = booksAdapter

                lparams {
                    elevation = 2F
                    width = matchParent
                }

                addItemDecoration(VerticalSpaceItemDecoration(10))

            }

            floatingActionButton {
                imageResource = R.drawable.icon_plus_white
                contentDescription = context.getString(R.string.floating_button_add_book)

                onClick {

                    alertDialog = alert(Appcompat, "Add an ePub to your library") {
                        customView {
                            verticalLayout {
                                lparams {
                                    bottomPadding = dip(16)
                                }
                                button {
                                    text = context.getString(R.string.select_from_your_device)
                                    onClick {
                                        alertDialog.dismiss()
                                        showDocumentPicker()
                                    }
                                }
                                button {
                                    text = context.getString(R.string.download_from_url)
                                    onClick {
                                        alertDialog.dismiss()
                                        showDownloadFromUrlAlert()
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

    override fun onStart() {
        super.onStart()

        startServer()

        permissionHelper.storagePermission {
            if (books.isEmpty()) {
                if (!preferences.contains("samples")) {
                    val dir = File(R2DIRECTORY)
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                    copySamplesFromAssetsToStorage()
                    preferences.edit().putBoolean("samples", true).apply()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        booksAdapter.notifyDataSetChanged()
    }


    override fun onDestroy() {
        super.onDestroy()
        //TODO not sure if this is needed
        stopServer()
    }

    private fun showDownloadFromUrlAlert() {
        var editTextHref: EditText? = null
        alert(Appcompat, "Add a publication from URL") {

            customView {
                verticalLayout {
                    textInputLayout {
                        padding = dip(10)
                        editTextHref = editText {
                            hint = "URL"
                            contentDescription = "Enter A URL"
                        }
                    }
                }
            }
            positiveButton("Add") { }
            negativeButton("Cancel") { }

        }.build().apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            setOnShowListener {
                val b = getButton(AlertDialog.BUTTON_POSITIVE)
                b.setOnClickListener {
                    if (TextUtils.isEmpty(editTextHref!!.text)) {
                        editTextHref!!.error = "Please Enter A URL."
                        editTextHref!!.requestFocus()
                    } else if (!URLUtil.isValidUrl(editTextHref!!.text.toString())) {
                        editTextHref!!.error = "Please Enter A Valid URL."
                        editTextHref!!.requestFocus()
                    } else {
                        val parseDataPromise = parseURL(URL(editTextHref!!.text.toString()))
                        parseDataPromise.successUi { parseData ->
                            dismiss()
                            downloadData(parseData)
                        }
                        parseDataPromise.failUi {
                            editTextHref!!.error = "Please Enter A Valid OPDS Book URL."
                            editTextHref!!.requestFocus()
                        }
                    }
                }
            }

        }.show()
    }

    private fun downloadData(parseData: ParseData) {
        val progress = indeterminateProgressDialog(getString(R.string.progress_wait_while_downloading_book))
        progress.show()

        val publication = parseData.publication ?: return

        if (publication.type == Publication.TYPE.EPUB) {

            val downloadUrl = getDownloadURL(publication)

            opdsDownloader.publicationUrl(downloadUrl.toString()).successUi { pair ->

                val publicationIdentifier = publication.metadata.identifier
                val author = authorName(publication)
                task {
                    getBitmapFromURL(publication.images.first().href!!)
                }.then {
                    val bitmap = it
                    val stream = ByteArrayOutputStream()
                    bitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream)

                    val book = Book(pair.second, publication.metadata.title, author, pair.first, null, publication.coverLink?.href, publicationIdentifier, stream.toByteArray(), Publication.EXTENSION.EPUB)

                    launch {
                        progress.dismiss()
                        database.books.insert(book, false)?.let { id ->
                            book.id = id
                            books.add(0, book)
                            booksAdapter.notifyDataSetChanged()
                            catalogView.longSnackbar("publication added to your library")
                            //prepareSyntheticPageList(publication, book)
                        } ?: run {

                            showDuplicateBookAlert(book, publication, false)

                        }
                    }
                }.fail {
                    launch {
                        progress.dismiss()
                        catalogView.snackbar("$it")
                    }
                }
            }
        } else if (publication.type == Publication.TYPE.WEBPUB || publication.type == Publication.TYPE.AUDIO) {

            val self = publication.linkWithRel("self")

            when (publication.type) {
                Publication.TYPE.WEBPUB -> {
                    progress.dismiss()
                    prepareWebPublication(self?.href!!, webPub = null, add = true)
                }
                Publication.TYPE.AUDIO -> {
                    progress.dismiss()
                    prepareWebPublication(self?.href!!, webPub = null, add = true) //will be adapted later
                }
                else -> {
                    progress.dismiss()
                    catalogView.snackbar("Invalid publication")
                }
            }

        }
    }

    private fun showDuplicateBookAlert(book: Book, publication: Publication, lcp: Boolean) {
        val duplicateAlert = alert(Appcompat, "Publication already exists") {

            positiveButton("Add anyways") { }
            negativeButton("Cancel") { }

        }.build()
        duplicateAlert.apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            setOnShowListener {
                val button = getButton(AlertDialog.BUTTON_POSITIVE)
                button.setOnClickListener {
                    database.books.insert(book, true)?.let {
                        book.id = it
                        books.add(0, book)
                        duplicateAlert.dismiss()
                        booksAdapter.notifyDataSetChanged()
                        catalogView.longSnackbar("publication added to your library")
                        if (!lcp) {
                            //prepareSyntheticPageList(publication, book)
                        }
                    }
                }
                val cancelButton = getButton(AlertDialog.BUTTON_NEGATIVE)
                cancelButton.setOnClickListener {
                    File(book.fileUrl).delete()
                    duplicateAlert.dismiss()
                }
            }
        }
        duplicateAlert.show()
    }

    private fun showDocumentPicker() {
        // ACTION_GET_DOCUMENT allows to import a system file by creating a copy of it
        // with access to every app that manages files
        val intent = Intent(Intent.ACTION_GET_CONTENT)

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        // Filter to show only epubs, using the image MIME data type.
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        intent.type = "*/*"
//        val mimeTypes = arrayOf(
//                "application/epub+zip",
//                "application/x-cbz"
//        )
//        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)

        startActivityForResult(intent, 1)
    }

    private fun parseURL(url: URL): Promise<ParseData, Exception> {
        return Fuel.get(url.toString(), null).promise() then {
            val (_, _, result) = it
            if (isJson(result)) {
                OPDS2Parser.parse(result, url)
            } else {
                OPDS1Parser.parse(result, url)
            }
        }
    }

    private fun isJson(byteArray: ByteArray): Boolean {
        return try {
            JSONObject(String(byteArray))
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getPublicationURL(src: String): JSONObject? {
        return try {
            val url = URL(src)
            val connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.doInput = true
            connection.connect()

            val jsonManifestURL = URL(connection.getHeaderField("Location")
                    ?: src).openConnection()
            jsonManifestURL.connect()

            val jsonManifest = jsonManifestURL.getInputStream().readBytes()
            val stringManifest = jsonManifest.toString(Charset.defaultCharset())
            val json = JSONObject(stringManifest)

            jsonManifestURL.close()
            connection.disconnect()
            connection.close()

            json
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun getBitmapFromURL(src: String): Bitmap? {
        return try {
            val url = URL(src)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(input)
            connection.close()
            bitmap
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun getDownloadURL(publication: Publication): URL? {
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

    private fun parseIntent(filePath: String?) {

        filePath?.let {

            val progress = indeterminateProgressDialog(getString(R.string.progress_wait_while_downloading_book))
            progress.show()

            val fileName = UUID.randomUUID().toString()
            val publicationPath = R2DIRECTORY + fileName

            task {
                copyFile(File(filePath), File(publicationPath))
            } then {
                preparePublication(publicationPath, filePath, fileName, progress)
            }

        } ?: run {
            val intent = intent
            val uriString: String? = intent.getStringExtra(R2IntentHelper.URI)
            val lcp: Boolean = intent.getBooleanExtra(R2IntentHelper.LCP, false)
            uriString?.let {
                when {
                    lcp -> parseIntentLcpl(uriString, isNetworkAvailable)
                    else -> parseIntentEpub(uriString)
                }
            }
        }
    }

    private fun parseIntentEpub(uriString: String) {
        val uri: Uri? = Uri.parse(uriString)
        if (uri != null) {

            val progress = indeterminateProgressDialog(getString(R.string.progress_wait_while_downloading_book))
            progress.show()
            val fileName = UUID.randomUUID().toString()
            val publicationPath = R2DIRECTORY + fileName
            val path = RealPathUtil.getRealPath(this, uri)
            task {
                if (path != null) {
                    copyFile(File(path), File(publicationPath))
                } else {
                    val input = URL(uri.toString()).openStream()
                    input.toFile(publicationPath)
                }
            } then {
                preparePublication(publicationPath, uriString, fileName, progress)
            }

        }
    }


    private fun preparePublication(publicationPath: String, uriString: String, fileName: String, progress: ProgressDialog) {

        val file = File(publicationPath)

        try {
            launch {

                if (uriString.endsWith(".epub")) {
                    val parser = EpubParser()
                    val pub = parser.parse(publicationPath)
                    if (pub != null) {
                        prepareToServe(pub, fileName, file.absolutePath, add = true, lcp = pub.container.drm?.let { true } ?: false)
                        progress.dismiss()
                    }
                } else if (uriString.endsWith(".cbz")) {
                    val parser = CbzParser()
                    val pub = parser.parse(publicationPath)
                    if (pub != null) {
                        prepareToServe(pub, fileName, file.absolutePath, add = true, lcp = pub.container.drm?.let { true } ?: false)
                        progress.dismiss()
                    }
                }

            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun prepareSyntheticPageList(pub: Publication, book: Book) {
        if (pub.pageList.isEmpty() && !(positionsDB.positions.isInitialized(book.id!!))) {
            val syntheticPageList = R2SyntheticPageList(positionsDB, book.id!!, pub.metadata.identifier)

            when (pub.type) {
                Publication.TYPE.EPUB -> syntheticPageList.execute(Triple("$BASE_URL:$localPort/", book.fileName, pub.readingOrder))
                Publication.TYPE.WEBPUB -> syntheticPageList.execute(Triple("", book.fileName, pub.readingOrder))
                else -> {
                    //no page list
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.opds -> {
                startActivity(intentFor<OPDSListActivity>())
                false
            }
            R.id.about -> {
                startActivity(intentFor<R2AboutActivity>())
                false
            }

            else -> super.onOptionsItemSelected(item)
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        this.permissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun startServer() {
        if (!server.isAlive) {
            try {
                server.start()
            } catch (e: IOException) {
                // do nothing
                Timber.e(e)
            }
            if (server.isAlive) {

                // Add Resources from R2Navigator
                server.loadReadiumCSSResources(assets)
                server.loadR2ScriptResources(assets)
                server.loadR2FontResources(assets, applicationContext)

//                // Add your own resources here
//                server.loadCustomResource(assets.open("scripts/test.js"), "test.js")
//                server.loadCustomResource(assets.open("styles/test.css"), "test.css")
//                server.loadCustomFont(assets.open("fonts/test.otf"), applicationContext, "test.otf")
            }
        }
    }

    private fun stopServer() {
        if (server.isAlive) {
            server.stop()
        }
    }

    private fun authorName(publication: Publication): String {
        return publication.metadata.authors.firstOrNull()?.name?.let {
            return@let it
        } ?: run {
            return@run String()
        }
    }

    private fun copySamplesFromAssetsToStorage() {
        assets.list("Samples")?.filter { it.endsWith(".epub") || it.endsWith(".cbz") }?.let { list ->
            for (element in list) {
                val input = assets.open("Samples/$element")
                val fileName = UUID.randomUUID().toString()
                val publicationPath = R2DIRECTORY + fileName
                input.toFile(publicationPath)
                val file = File(publicationPath)
                if (element.endsWith(".epub")) {
                    val parser = EpubParser()
                    val pub = parser.parse(publicationPath)
                    if (pub != null) {
                        prepareToServe(pub, fileName, file.absolutePath, add = true, lcp = pub.container.drm?.let { true } ?: false)
                    }
                } else if (element.endsWith(".cbz")) {
                    val parser = CbzParser()
                    val pub = parser.parse(publicationPath)
                    if (pub != null) {
                        prepareToServe(pub, fileName, file.absolutePath, add = true, lcp = pub.container.drm?.let { true } ?: false)
                    }
                }
            }
        }
    }

    private fun copyFile(src: File, dst: File) {
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

    protected fun prepareToServe(pub: PubBox?, fileName: String, absolutePath: String, add: Boolean, lcp: Boolean) {
        if (pub == null) {
            catalogView.snackbar("Invalid publication")
            return
        }
        val publication = pub.publication
        val container = pub.container

        launch {
            if (publication.type == Publication.TYPE.EPUB) {
                val publicationIdentifier = publication.metadata.identifier
                preferences.edit().putString("$publicationIdentifier-publicationPort", localPort.toString()).apply()
                val author = authorName(publication)
                if (add) {
                    var book = Book(fileName, publication.metadata.title, author, absolutePath, null, publication.coverLink?.href, publicationIdentifier, null, Publication.EXTENSION.EPUB)
                    publication.coverLink?.href?.let {
                        val blob = ZipUtil.unpackEntry(File(absolutePath), it.removePrefix("/"))
                        blob?.let {
                            book = Book(fileName, publication.metadata.title, author, absolutePath, null, publication.coverLink?.href, publicationIdentifier, blob, Publication.EXTENSION.EPUB)
                        } ?: run {
                            book = Book(fileName, publication.metadata.title, author, absolutePath, null, publication.coverLink?.href, publicationIdentifier, null, Publication.EXTENSION.EPUB)
                        }
                    } ?: run {
                        book = Book(fileName, publication.metadata.title, author, absolutePath, null, publication.coverLink?.href, publicationIdentifier, null, Publication.EXTENSION.EPUB)
                    }

                    database.books.insert(book, false)?.let {
                        book.id = it
                        books.add(0, book)
                        booksAdapter.notifyDataSetChanged()
                        catalogView.longSnackbar("publication added to your library")
                        if (!lcp) {
                            //prepareSyntheticPageList(publication, book)
                        }
                    } ?: run {

                        showDuplicateBookAlert(book, publication, lcp)

                    }

                }
                if (!lcp) {
                    server.addEpub(publication, container, "/$fileName", applicationContext.filesDir.path + "/" + Injectable.Style.rawValue + "/UserProperties.json")
                }

            } else if (publication.type == Publication.TYPE.CBZ) {
                if (add) {
                    publication.coverLink?.href?.let {
                        val book = Book(fileName, publication.metadata.title, null, absolutePath, null, publication.coverLink?.href, UUID.randomUUID().toString(), container.data(it), Publication.EXTENSION.CBZ)
                        database.books.insert(book, false)?.let { id ->
                            book.id = id
                            books.add(0, book)
                            booksAdapter.notifyDataSetChanged()
                            if (!lcp) {
                                //prepareSyntheticPageList(publication, book)
                            }
                        } ?: run {

                            showDuplicateBookAlert(book, publication, lcp)

                        }
                    }
                }
            } else if (publication.type == Publication.TYPE.AUDIO) {
                if (add) {
                    //Getting book cover from file path to bitmap
                    val ref = publication.coverLink?.href
                    val stream = ByteArrayOutputStream()
                    ref?.let {
                        val arrayInputStream = pub.container.data(ref)
                        val bitmap = BitmapFactory.decodeByteArray(arrayInputStream, 0, arrayInputStream.size)
                        bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    }


                    //Building book object and adding it to library
                    val book = Book(fileName, publication.metadata.title, null, absolutePath, null, publication.coverLink?.href, UUID.randomUUID().toString(), stream.toByteArray(), Publication.EXTENSION.AUDIO)


                    database.books.insert(book, false)?.let {
                        book.id = it
                        books.add(0, book)
                        booksAdapter.notifyDataSetChanged()

                    } ?: run {
                        showDuplicateBookAlert(book, publication, lcp)
                    }
                }
            }
        }
    }

    override fun recyclerViewListLongClicked(v: View, position: Int) {
        val layout = LayoutInflater.from(this).inflate(R.layout.popup_delete, catalogView, false) //Inflating the layout
        val popup = PopupWindow(this)
        popup.contentView = layout
        popup.width = ListPopupWindow.WRAP_CONTENT
        popup.height = ListPopupWindow.WRAP_CONTENT
        popup.isOutsideTouchable = true
        popup.isFocusable = true
        popup.showAsDropDown(v, 24, -350, Gravity.CENTER)
        val delete: Button = layout.findViewById(R.id.delete) as Button
        delete.setOnClickListener {
            val book = books[position]
            val publicationPath = R2DIRECTORY + book.fileName
            books.remove(book)
            booksAdapter.notifyDataSetChanged()
            catalogView.longSnackbar("publication deleted from your library")
            val file = File(publicationPath)
            file.delete()
            popup.dismiss()
            val deleted = database.books.delete(book)
            if (deleted > 0) {
                BookmarksDatabase(this).bookmarks.delete(deleted.toLong())
                PositionsDatabase(this).positions.delete(deleted.toLong())
            }
        }
    }

    private val isNetworkAvailable: Boolean
        get() {
            val connectivityManager: ConnectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }

    override fun recyclerViewListClicked(v: View, position: Int) {
        val progress = indeterminateProgressDialog(getString(R.string.progress_wait_while_preparing_book))
        progress.show()
        task {
            val book = books[position]
            val publicationPath = R2DIRECTORY + book.fileName
            val file = File(publicationPath)
            when {
                book.ext == Publication.EXTENSION.EPUB -> {
                    val parser = EpubParser()
                    val pub = parser.parse(publicationPath)
                    pub?.let {
                        pub.container.drm?.let { drm: DRM ->
                            prepareAndStartActivityWithLCP(drm, pub, book, file, publicationPath, parser, pub.publication, isNetworkAvailable)
                        } ?: run {
                            prepareAndStartActivity(pub, book, file, publicationPath, pub.publication)
                        }
                    }
                }
                book.ext == Publication.EXTENSION.CBZ -> {
                    val parser = CbzParser()
                    val pub = parser.parse(publicationPath)
                    pub?.let {
                        startActivity(intentFor<R2CbzActivity>("publicationPath" to publicationPath, "cbzName" to book.fileName, "publication" to pub.publication))
                    }
                }
                book.ext == Publication.EXTENSION.AUDIO -> {

                    //If selected book is an audiobook
                    val parser = AudioBookParser()
                    //Parse book manifest to publication object
                    val pub = parser.parse(publicationPath)

                    pub?.let {
                        //Starting AudiobookActivity
                        val ref = pub.publication.coverLink?.href
                        val coverByteArray = ref?.let {
                            pub.container.data(ref)
                        }

                        startActivity(publicationPath, book, pub.publication, coverByteArray)
                    }
                }
                book.ext == Publication.EXTENSION.JSON -> {
                    prepareWebPublication(book.fileUrl, book, add = false)
                }
                else -> null
            }
        } then {
            progress.dismiss()
        } fail {
            progress.dismiss()
        }
    }

    private fun prepareWebPublication(externalManifest: String, webPub: Book?, add: Boolean) {
        task {

            getPublicationURL(externalManifest)

        } then { json ->

            json?.let {
                val externalPub = parsePublication(json)
                val externalURI = externalPub.linkWithRel("self")!!.href!!.substring(0, externalManifest.lastIndexOf("/") + 1)

                var book: Book? = null

                if (add) {

                    externalPub.coverLink?.href?.let { href ->
                        val bitmap: Bitmap? = if (URI(href).isAbsolute) {
                            getBitmapFromURL(href)
                        } else {
                            getBitmapFromURL(externalURI + href)
                        }
                        val stream = ByteArrayOutputStream()
                        bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, stream)

                        book = Book(externalURI, externalPub.metadata.title, null, externalManifest, null, externalURI + externalPub.coverLink?.href, externalPub.metadata.identifier, stream.toByteArray(), Publication.EXTENSION.JSON)

                    } ?: run {
                        book = Book(externalURI, externalPub.metadata.title, null, externalManifest, null, null, externalPub.metadata.identifier, null, Publication.EXTENSION.JSON)
                    }

                    launch {
                        database.books.insert(book!!, false)?.let { id ->
                            book!!.id = id
                            books.add(0, book!!)
                            booksAdapter.notifyDataSetChanged()
                            catalogView.longSnackbar("publication added to your library")
                            //prepareSyntheticPageList(externalPub, book!!)
                        } ?: run {
                            showDuplicateBookAlert(book!!, externalPub, false)
                        }
                    }
                } else {
                    book = webPub
                    var bookCover = book?.cover
                    startActivity(book!!.fileName, book!!, externalPub, bookCover)
                }
            }
        }
    }

    private fun prepareAndStartActivity(pub: PubBox?, book: Book, file: File, publicationPath: String, publication: Publication) {
        prepareToServe(pub, book.fileName, file.absolutePath, add = false, lcp = false)
        startActivity(publicationPath, book, publication)
    }

    private fun startActivity(publicationPath: String, book: Book, publication: Publication, coverByteArray: ByteArray? = null) {
        if (publication.type == Publication.TYPE.AUDIO) {
            coverByteArray?.let {
                startActivity(intentFor<AudiobookActivity>("publicationPath" to publicationPath,
                        "epubName" to book.fileName,
                        "publication" to publication,
                        "bookId" to book.id,
                        "cover" to coverByteArray))
            } ?: run {
                startActivity(intentFor<AudiobookActivity>("publicationPath" to publicationPath,
                        "epubName" to book.fileName,
                        "publication" to publication,
                        "bookId" to book.id))
            }
        } else {
            startActivity(intentFor<R2EpubActivity>("publicationPath" to publicationPath,
                    "epubName" to book.fileName,
                    "publication" to publication,
                    "bookId" to book.id))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data ?: return


        // The document selected by the user won't be returned in the intent.
        // Instead, a URI to that document will be contained in the return intent
        // provided to this method as a parameter.
        // Pull that URI using resultData.getData().
        if (requestCode == 1 && resultCode == RESULT_OK) {

            val progress = indeterminateProgressDialog(getString(R.string.progress_wait_while_downloading_book))

            task {

                progress.show()

            } then {

                val uri: Uri? = data.data
                uri?.let {
                    val fileType = getMimeType(uri)
                    val mime = fileType.first
                    val name = fileType.second

                    if (name.endsWith(".lcpl")) {
                        processLcpActivityResult(uri, it, progress, isNetworkAvailable)
                    } else {
                        processEpubResult(uri, mime, progress, name)
                    }

                }

            }

        } else if (resultCode == RESULT_OK) {
            val progress = indeterminateProgressDialog(getString(R.string.progress_wait_while_downloading_book))
            progress.show()

            task {
                val filePath = data.getStringExtra("resultPath")
                parseIntent(filePath)
            } then {
                progress.dismiss()
            }

        }
    }


    private fun processEpubResult(uri: Uri?, mime: String, progress: ProgressDialog, name: String) {
        val fileName = UUID.randomUUID().toString()
        val publicationPath = R2DIRECTORY + fileName

        val input = contentResolver.openInputStream(uri as Uri)

        launch {

            //If its an audiobook, unpack the .audiobook file
            if (name.endsWith(".audiobook")) {
                val output = File(publicationPath)
                if (!output.exists()) {
                    if (!output.mkdir()) {
                        throw RuntimeException("Cannot create directory")
                    }
                }
                Handler().postDelayed({
                    ZipUtil.unpack(input, output)
                }, 100)
            } else {
                input?.toFile(publicationPath)
            }

            val file = File(publicationPath)

            try {
                if (mime == "application/epub+zip") {
                    val parser = EpubParser()
                    val pub = parser.parse(publicationPath)
                    if (pub != null) {
                        prepareToServe(pub, fileName, file.absolutePath, add = true, lcp = pub.container.drm?.let { true } ?: false)
                        progress.dismiss()

                    }
                } else if (name.endsWith(".cbz")) {
                    val parser = CbzParser()
                    val pub = parser.parse(publicationPath)
                    if (pub != null) {
                        prepareToServe(pub, fileName, file.absolutePath, add = true, lcp = pub.container.drm?.let { true } ?: false)
                        progress.dismiss()

                    }
                } else if (name.endsWith(".audiobook")) {
                    val parser = AudioBookParser()
                    val pub = parser.parse(publicationPath)

                    if (pub != null) {
                        prepareToServe(pub, fileName, file.absolutePath, true, pub.container.drm?.let { true } ?: false)
                        progress.dismiss()
                    }
                } else {
                    catalogView.longSnackbar("Unsupported file")
                    progress.dismiss()
                    file.delete()
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }


    private fun getMimeType(uri: Uri): Pair<String, String> {
        val mimeType: String?
        var fileName = String()
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val contentResolver: ContentResolver = applicationContext.contentResolver
            mimeType = contentResolver.getType(uri)
            getContentName(contentResolver, uri)?.let {
                fileName = it
            }
        } else {
            val fileExtension: String = MimeTypeMap.getFileExtensionFromUrl(uri
                    .toString())
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileExtension.toLowerCase())
        }
        return Pair(mimeType!!, fileName)
    }

    private fun getContentName(resolver: ContentResolver, uri: Uri): String? {
        val cursor = resolver.query(uri, null, null, null, null)
        cursor!!.moveToFirst()
        val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
        return if (nameIndex >= 0) {
            val name = cursor.getString(nameIndex)
            cursor.close()
            name
        } else {
            null
        }
    }

    class VerticalSpaceItemDecoration(private val verticalSpaceHeight: Int) : androidx.recyclerview.widget.RecyclerView.ItemDecoration() {

        override fun getItemOffsets(outRect: Rect, view: View, parent: androidx.recyclerview.widget.RecyclerView,
                                    state: androidx.recyclerview.widget.RecyclerView.State) {
            outRect.bottom = verticalSpaceHeight
        }
    }


    override fun parseIntentLcpl(uriString: String, networkAvailable: Boolean) {
        listener?.parseIntentLcpl(uriString, networkAvailable)
    }

    override fun prepareAndStartActivityWithLCP(drm: DRM, pub: PubBox, book: Book, file: File, publicationPath: String, parser: EpubParser, publication: Publication, networkAvailable: Boolean) {
        listener?.prepareAndStartActivityWithLCP(drm, pub, book, file, publicationPath, parser, publication, networkAvailable)
    }

    override fun processLcpActivityResult(uri: Uri, it: Uri, progress: ProgressDialog, networkAvailable: Boolean) {
        listener?.processLcpActivityResult(uri, it, progress, networkAvailable)
    }

}

