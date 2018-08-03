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


//import org.readium.r2.lcp.LcpHttpService
//import org.readium.r2.lcp.LcpLicense
//import org.readium.r2.lcp.LcpSession
import android.app.ProgressDialog
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.*
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
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
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.json.JSONObject
import org.readium.r2.navigator.R2CbzActivity
import org.readium.r2.opds.OPDS1Parser
import org.readium.r2.opds.OPDS2Parser
import org.readium.r2.shared.Publication
import org.readium.r2.shared.drm.Drm
import org.readium.r2.shared.opds.ParseData
import org.readium.r2.shared.promise
import org.readium.r2.streamer.parser.CbzParser
import org.readium.r2.streamer.parser.EpubParser
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.streamer.server.BASE_URL
import org.readium.r2.streamer.server.Server
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
import java.net.URL
import java.util.*

class CatalogActivity : AppCompatActivity(), BooksAdapter.RecyclerViewClickListener, LcpFunctions {

    private lateinit var server: Server
    private var localPort: Int = 0

    private lateinit var booksAdapter: BooksAdapter
    private lateinit var permissionHelper: PermissionHelper
    private lateinit var permissions: Permissions
    private lateinit var preferences: SharedPreferences
    private lateinit var R2DIRECTORY: String

    private lateinit var database: BooksDatabase
    private lateinit var opdsDownloader: OPDSDownloader
    private lateinit var publication: Publication

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
        R2DIRECTORY = this.getExternalFilesDir(null).path + "/"

        permissions = Permissions(this)
        permissionHelper = PermissionHelper(this, permissions)

        opdsDownloader = OPDSDownloader(this)
        database = BooksDatabase(this)
        books = database.books.list()

        booksAdapter = BooksAdapter(this, books, "$BASE_URL:$localPort", this)

        //Unit Tests for Bookmarks db interactions
//        TestBookmarksDatabase(this).test()

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

        publication = parseData.publication ?: return
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

                val book = Book(pair.second, publication.metadata.title, author, pair.first, null, publication.coverLink?.href, publicationIdentifier, stream.toByteArray(), Publication.EXTENSION.EPUB)

                runOnUiThread {
                    progress.dismiss()
                    database.books.insert(book, false)?.let {
                        book.id = it
                        books.add(book)
                        booksAdapter.notifyDataSetChanged()

                    } ?: run {

                        showDuplicateBookAlert(book)

                    }
                }
            }
        }
    }

    private fun showDuplicateBookAlert(book: Book) {
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
                        books.add(book)
                        duplicateAlert.dismiss()
                        booksAdapter.notifyDataSetChanged()
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
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)

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
                    lcp -> parseIntentLcpl(uriString)
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
            runOnUiThread {

                if (uriString.endsWith(".epub")) {
                    val parser = EpubParser()
                    val pub = parser.parse(publicationPath)
                    if (pub != null) {
                        prepareToServe(pub, fileName, file.absolutePath, true, false)
                        progress.dismiss()
                    }
                } else if (uriString.endsWith(".cbz")) {
                    val parser = CbzParser()
                    val pub = parser.parse(publicationPath)
                    if (pub != null) {
                        prepareToServe(pub, fileName, file.absolutePath, true, false)
                        progress.dismiss()
                    }
                }

            }
        } catch (e: Throwable) {
            e.printStackTrace()
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
            server.loadResources(assets, applicationContext)
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
        val list = assets.list("Samples").filter { it.endsWith(".epub") || it.endsWith(".cbz") }
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
                    prepareToServe(pub, fileName, file.absolutePath, true, false)
                }
            } else if (element.endsWith(".cbz")) {
                val parser = CbzParser()
                val pub = parser.parse(publicationPath)
                if (pub != null) {
                    prepareToServe(pub, fileName, file.absolutePath, true, false)
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

    private fun prepareToServe(pub: PubBox?, fileName: String, absolutePath: String, add: Boolean, lcp: Boolean) {
        if (pub == null) {
            snackbar(catalogView, "Invalid publication")
            return
        }
        val publication = pub.publication
        val container = pub.container

        runOnUiThread {
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
                        books.add(book)
                        booksAdapter.notifyDataSetChanged()
                    } ?: run {

                        showDuplicateBookAlert(book)

                    }
                }
                if (!lcp) {
                    server.addEpub(publication, container, "/$fileName", applicationContext.getExternalFilesDir(null).path + "/styles/UserProperties.json")
                }

            } else if (publication.type == Publication.TYPE.CBZ) {
                if (add) {
                    publication.coverLink?.href?.let {
                        val book = Book(fileName, publication.metadata.title, null, absolutePath, null, publication.coverLink?.href, UUID.randomUUID().toString(), container.data(it), Publication.EXTENSION.CBZ)
                        database.books.insert(book, false)?.let {
                            book.id = it
                            books.add(book)
                            booksAdapter.notifyDataSetChanged()
                        } ?: run {

                            showDuplicateBookAlert(book)

                        }
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
            val publicationPath = R2DIRECTORY + book.fileName
            val file = File(publicationPath)
            when {
                book.ext == Publication.EXTENSION.EPUB -> {
                    val parser = EpubParser()
                    val pub = parser.parse(publicationPath)
                    pub?.let {
                        pub.container.drm?.let { drm: Drm ->
                            prepareAndStartActivityWithLCP(drm, pub, book, file, publicationPath, parser, pub.publication)
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
                else -> null
            }
        } then {
            progress.dismiss()
        }
    }

    private fun prepareAndStartActivity(pub: PubBox?, book: Book, file: File, publicationPath: String, publication: Publication) {
        prepareToServe(pub, book.fileName, file.absolutePath, false, false)
        startActivity(intentFor<org.readium.r2.testapp.R2EpubActivity>("publicationPath" to publicationPath,
                "epubName" to book.fileName,
                "publication" to publication,
                "bookId" to book.id))
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
                        processLcpActivityResult(uri, it, progress)
                    } else {
                        processEpubResult(uri, mime, progress, name)
                    }

                }

            }

        } else if (resultCode == RESULT_OK) {
            val progress = indeterminateProgressDialog(getString(R.string.progress_wait_while_downloading_book))
            progress.show()

            task {
                val filePath = data.getStringExtra(Chooser.RESULT_PATH)
                parseIntent(filePath)
            } then {
                progress.dismiss()
            }

        }
    }


    private fun processEpubResult(uri: Uri?, mime: String, progress: ProgressDialog, name: String) {
        val fileName = UUID.randomUUID().toString()
        val publicationPath = R2DIRECTORY + fileName

        val input = contentResolver.openInputStream(uri)
        input.toFile(publicationPath)
        val file = File(publicationPath)

        try {
            runOnUiThread {
                if (mime == "application/epub+zip") {
                    val parser = EpubParser()
                    val pub = parser.parse(publicationPath)
                    if (pub != null) {
                        prepareToServe(pub, fileName, file.absolutePath, true, false)
                        progress.dismiss()

                    }
                } else if (name.endsWith(".cbz")) {
                    val parser = CbzParser()
                    val pub = parser.parse(publicationPath)
                    if (pub != null) {
                        prepareToServe(pub, fileName, file.absolutePath, true, false)
                        progress.dismiss()

                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
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
        return Pair(mimeType, fileName)
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

    class VerticalSpaceItemDecoration(private val verticalSpaceHeight: Int) : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView,
                                    state: RecyclerView.State) {
            outRect.bottom = verticalSpaceHeight
        }
    }




     override fun parseIntentLcpl(uriString: String) {
//        val uri: Uri? = Uri.parse(uriString)
//        if (uri != null) {
//            val progress = indeterminateProgressDialog(getString(R.string.progress_wait_while_downloading_book))
//            progress.show()
//            val thread = Thread(Runnable {
//                val lcpLicense = LcpLicense(URL(uri.toString()).openStream().readBytes(), this)
//                task {
//                    lcpLicense.fetchStatusDocument().get()
//                } then {
//                    lcpLicense.checkStatus()
//                    lcpLicense.updateLicenseDocument().get()
//                } then {
//                    lcpLicense.areRightsValid()
//                    lcpLicense.register()
//                    lcpLicense.fetchPublication()
//                } then {
//                    it?.let {
//                        lcpLicense.moveLicense(it, URL(uri.toString()).openStream().readBytes())
//                    }
//                    it!!
//                } successUi { path ->
//                    val file = File(path)
//                    try {
//                        runOnUiThread({
//                            val parser = EpubParser()
//                            val pub = parser.parse(path)
//                            if (pub != null) {
//                                val pair = parser.parseRemainingResource(pub.container, pub.publication, pub.container.drm)
//                                pub.container = pair.first
//                                pub.publication = pair.second
//                                prepareToServe(pub, file.name, file.absolutePath, true, true)
//                                progress.dismiss()
//
//                            }
//                        })
//                    } catch (e: Throwable) {
//                        e.printStackTrace()
//                    }
//                }
//            })
//            thread.start()
//        }
    }

     override fun prepareAndStartActivityWithLCP(drm: Drm, pub: PubBox, book: Book, file: File, publicationPath: String, parser: EpubParser, publication: Publication) {
//        if (drm.brand == Drm.Brand.Lcp) {
//            prepareToServe(pub, book.fileName, file.absolutePath, false, true)
//
//            handleLcpPublication(publicationPath, drm, {
//                val pair = parser.parseRemainingResource(pub.container, publication, it)
//                pub.container = pair.first
//                pub.publication = pair.second
//            }, {
//                if (supportedProfiles.contains(it.profile)) {
//                    server.addEpub(publication, pub.container, "/" + book.fileName, applicationContext.getExternalFilesDir(null).path + "/styles/UserProperties.json")
//
//                    val license = (drm.license as LcpLicense)
//                    val drmModel = DRMMModel(drm.brand.name,
//                            license.currentStatus(),
//                            license.provider().toString(),
//                            DateTime(license.issued()).toString(DateTimeFormat.shortDateTime()),
//                            DateTime(license.lastUpdate()).toString(DateTimeFormat.shortDateTime()),
//                            DateTime(license.rightsStart()).toString(DateTimeFormat.shortDateTime()),
//                            DateTime(license.rightsEnd()).toString(DateTimeFormat.shortDateTime()),
//                            license.rightsPrints().toString(),
//                            license.rightsCopies().toString())
//
//                    startActivity(intentFor<R2EpubActivity>("publicationPath" to publicationPath, "epubName" to book.fileName, "publication" to publication, "drmModel" to drmModel))
//                } else {
//                    alert(Appcompat, "The profile of this DRM is not supported.") {
//                        negativeButton("Ok") { }
//                    }.show()
//                }
//            }, {
//                // Do nothing
//            }).get()
//
//        }
    }

     override fun processLcpActivityResult(uri: Uri, it: Uri, progress: ProgressDialog) {
//        val input = contentResolver.openInputStream(uri)
//
//        val thread = Thread(Runnable {
//            val lcpLicense = LcpLicense(input.readBytes(), this)
//            task {
//                lcpLicense.fetchStatusDocument().get()
//            } then {
//                lcpLicense.checkStatus()
//                lcpLicense.updateLicenseDocument().get()
//            } then {
//                lcpLicense.areRightsValid()
//                lcpLicense.register()
//                lcpLicense.fetchPublication()
//            } then {
//                it?.let {
//                    lcpLicense.moveLicense(it, contentResolver.openInputStream(uri).readBytes())
//                }
//                it!!
//            } successUi { path ->
//                val file = File(path)
//                try {
//                    runOnUiThread({
//                        val parser = EpubParser()
//                        val pub = parser.parse(path)
//                        if (pub != null) {
//                            val pair = parser.parseRemainingResource(pub.container, pub.publication, pub.container.drm)
//                            pub.container = pair.first
//                            pub.publication = pair.second
//                            prepareToServe(pub, file.name, file.absolutePath, true, true)
//                            progress.dismiss()
//                        }
//                    })
//                } catch (e: Throwable) {
//                    e.printStackTrace()
//                }
//            }
//        })
//        thread.start()
    }

//     override fun handleLcpPublication(publicationPath: String, drm: Drm, parsingCallback: (drm: Drm) -> Unit, callback: (drm: Drm) -> Unit, callbackUI: () -> Unit): Promise<Unit, Exception> {
//           val lcpHttpService = LcpHttpService()
//           val session = LcpSession(publicationPath, this)
//
//           fun validatePassphrase(passphraseHash: String): Promise<LcpLicense, Exception> {
//               return task {
//                   lcpHttpService.certificateRevocationList("http://crl.edrlab.telesec.de/rl/EDRLab_CA.crl").get()
//               } then { pemCrtl ->
//                   session.resolve(passphraseHash, pemCrtl).get()
//               }
//           }
//
//           fun promptPassphrase(reason: String? = null, callback: (pass: String) -> Unit) {
//               runOnUiThread {
//                   val hint = session.getHint()
//                   alert(Appcompat, hint, reason ?: "LCP Passphrase") {
//                       var editText: EditText? = null
//                       customView {
//                           verticalLayout {
//                               textInputLayout {
//                                   editText = editText { }
//                               }
//                           }
//                       }
//                       positiveButton("OK") {
//                           task {
//                               editText!!.text.toString()
//                           } then { clearPassphrase ->
//                               val passphraseHash = HASH.sha256(clearPassphrase)
//                               session.checkPassphrases(listOf(passphraseHash))
//                           } then { validPassphraseHash ->
//                               session.storePassphrase(validPassphraseHash)
//                               callback(validPassphraseHash)
//                           }
//                       }
//                       negativeButton("Cancel") { }
//                   }.show()
//               }
//           }
//
//           return task {
//               val passphrases = session.passphraseFromDb()
//               passphrases?.let {
//                   val lcpLicense = validatePassphrase(it).get()
//                   drm.license = lcpLicense
//                   drm.profile = session.getProfile()
//                   parsingCallback(drm)
//                   callback(drm)
//               } ?: run {
//                   promptPassphrase(null, {
//                       val lcpLicense = validatePassphrase(it).get()
//                       drm.license = lcpLicense
//                       drm.profile = session.getProfile()
//                       parsingCallback(drm)
//                       callback(drm)
//                       callbackUI()
//                   })
//               }
//           }
//       }

}

interface LcpFunctions {
     fun parseIntentLcpl(uriString: String)
     fun prepareAndStartActivityWithLCP(drm: Drm, pub: PubBox, book: Book, file: File, publicationPath: String, parser: EpubParser, publication: Publication)
     fun processLcpActivityResult(uri: Uri, it: Uri, progress: ProgressDialog)
//     fun handleLcpPublication(publicationPath: String, drm: Drm, parsingCallback: (drm: Drm) -> Unit, callback: (drm: Drm) -> Unit, callbackUI: () -> Unit): Promise<Unit, Exception>
}

