/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mostapha Idoubihi, Paul Stoica
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp.library

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.webkit.URLUtil
import android.widget.Button
import android.widget.EditText
import android.widget.ListPopupWindow
import android.widget.PopupWindow
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.mcxiaoke.koi.ext.onClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.jetbrains.anko.design.*
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.readium.r2.shared.Injectable
import org.readium.r2.shared.extensions.extension
import org.readium.r2.shared.extensions.toPng
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.format.Format
import org.readium.r2.shared.publication.ContentProtection
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.publication.services.protectionError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.File as R2File
import org.readium.r2.streamer.Streamer
import org.readium.r2.streamer.server.Server
import org.readium.r2.testapp.BuildConfig.DEBUG
import org.readium.r2.testapp.R
import org.readium.r2.testapp.R2AboutActivity
import org.readium.r2.testapp.db.*
import org.readium.r2.testapp.drm.DRMFulfilledPublication
import org.readium.r2.testapp.drm.DRMLibraryService
import org.readium.r2.testapp.opds.GridAutoFitLayoutManager
import org.readium.r2.testapp.opds.OPDSListActivity
import org.readium.r2.testapp.permissions.PermissionHelper
import org.readium.r2.testapp.permissions.Permissions
import org.readium.r2.testapp.utils.ContentResolverUtil
import org.readium.r2.testapp.utils.NavigatorContract
import org.readium.r2.testapp.utils.extensions.authorName
import org.readium.r2.testapp.utils.extensions.blockingProgressDialog
import org.readium.r2.testapp.utils.extensions.download
import org.readium.r2.testapp.utils.extensions.moveTo
import org.readium.r2.testapp.utils.extensions.toFile
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.net.ServerSocket
import java.net.URL
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

var activitiesLaunched: AtomicInteger = AtomicInteger(0)

@SuppressLint("Registered")
abstract class LibraryActivity : AppCompatActivity(), BooksAdapter.RecyclerViewClickListener, DRMLibraryService, CoroutineScope {

    /**
     * Context of this scope.
     */
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    private lateinit var server: Server
    private var localPort: Int = 0

    private lateinit var booksAdapter: BooksAdapter
    private lateinit var permissionHelper: PermissionHelper
    private lateinit var permissions: Permissions
    private lateinit var preferences: SharedPreferences
    private lateinit var R2DIRECTORY: String

    private lateinit var database: BooksDatabase
    private lateinit var positionsDB: PositionsDatabase

    protected var contentProtections: List<ContentProtection> = emptyList()
    private lateinit var streamer: Streamer

    private lateinit var catalogView: androidx.recyclerview.widget.RecyclerView
    private lateinit var alertDialog: AlertDialog
    private lateinit var documentPickerLauncher: ActivityResultLauncher<String>
    private lateinit var navigatorLauncher: ActivityResultLauncher<NavigatorContract.Input>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)

        streamer = Streamer(this, contentProtections = contentProtections)

        val s = ServerSocket(if (DEBUG) 8080 else 0)
        s.localPort
        s.close()

        localPort = s.localPort
        server = Server(localPort, applicationContext)

        val properties = Properties()
        val inputStream = this.assets.open("configs/config.properties")
        properties.load(inputStream)
        val useExternalFileDir = properties.getProperty("useExternalFileDir", "false")!!.toBoolean()

        R2DIRECTORY = if (useExternalFileDir) {
            this.getExternalFilesDir(null)?.path + "/"
        } else {
            this.filesDir.path + "/"
        }

        permissions = Permissions(this)
        permissionHelper = PermissionHelper(this, permissions)

        database = BooksDatabase(this)
        books = database.books.list()

        booksAdapter = BooksAdapter(this, books, this)

        documentPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { importPublicationFromUri(it) }
        }

        navigatorLauncher = registerForActivityResult(NavigatorContract()) { pubData: NavigatorContract.Output? ->
            if (pubData == null)
                return@registerForActivityResult

            tryOrNull { pubData.publication.close() }
            Timber.d("Publication closed")
            if (pubData.deleteOnResult)
                tryOrNull { pubData.file.file.delete() }
        }

        intent.data?.let { importPublicationFromUri(it) }

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

                    alertDialog = alert(Appcompat, context.getString(R.string.add_publication_to_library)) {
                        customView {
                            verticalLayout {
                                lparams {
                                    bottomPadding = dip(16)
                                }
                                button {
                                    text = context.getString(R.string.select_from_your_device)
                                    onClick {
                                        alertDialog.dismiss()
                                        documentPickerLauncher.launch("*/*")
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
                    launch {
                        copySamplesFromAssetsToStorage()
                    }
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
                if (DEBUG) Timber.e(e)
            }
            if (server.isAlive) {
//                // Add your own resources here
//                server.loadCustomResource(assets.open("scripts/test.js"), "test.js")
//                server.loadCustomResource(assets.open("styles/test.css"), "test.css")
//                server.loadCustomFont(assets.open("fonts/test.otf"), applicationContext, "test.otf")

                server.loadCustomResource(assets.open("Search/mark.js"), "mark.js", Injectable.Script)
                server.loadCustomResource(assets.open("Search/search.js"), "search.js", Injectable.Script)
                server.loadCustomResource(assets.open("Search/mark.css"), "mark.css", Injectable.Style)

                isServerStarted = true
            }
        }
    }

    private fun stopServer() {
        if (server.isAlive) {
            server.stop()
            isServerStarted = false
        }
    }

    private suspend fun copySamplesFromAssetsToStorage() = withContext(Dispatchers.IO) {
        val samples = assets.list("Samples")?.filterNotNull().orEmpty()

        for (element in samples) {
            val file = assets.open("Samples/$element").copyToTempFile()
            if (file != null)
                importPublication(file)
            else if (BuildConfig.DEBUG)
                error("Unable to load sample into the library")
        }
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
                        val url = tryOrNull { URL(editTextHref?.text.toString()) }
                            ?: return@setOnClickListener

                        launch {
                            val progress =
                                blockingProgressDialog(getString(R.string.progress_wait_while_downloading_book))
                                    .apply { show() }

                            val downloadedFile = url.copyToTempFile() ?: return@launch
                            importPublication(downloadedFile, progress)
                        }
                    }
                }
            }

        }.show()
    }

    private fun importPublicationFromUri(uri: Uri) {

        launch {
            val progress = blockingProgressDialog(getString(R.string.progress_wait_while_downloading_book))
                .apply { show() }

            uri.copyToTempFile()
                ?.let { importPublication(it, progress) }
                ?: progress.dismiss()
        }
    }

    private suspend fun importPublication(sourceFile: R2File, progress: ProgressDialog? = null) {
        val foreground = progress != null

        val publicationFile =
            if (sourceFile.format() != Format.LCP_LICENSE)
                sourceFile
            else {
                fulfill(sourceFile.file).fold(
                    {
                        val format = Format.of(fileExtension = File(it.suggestedFilename).extension)
                        R2File(it.localFile.path, format = format)
                    },
                    {
                        tryOrNull { sourceFile.file.delete() }
                        Timber.d(it)
                        progress?.dismiss()
                        if (foreground) catalogView.longSnackbar("fulfillment error: ${it.message}")
                        return
                    }
                )
            } ?: run {
                progress?.dismiss()
                return
            }

        val format = publicationFile.format()
        val fileName = "${UUID.randomUUID()}.${format?.fileExtension}"
        val libraryFile = R2File(
            R2DIRECTORY + fileName,
            format = publicationFile.format(),
            sourceUrl = publicationFile.sourceUrl
        )

        try {
            publicationFile.file.moveTo(libraryFile.file)
        } catch (e: Exception) {
            Timber.d(e)
            tryOrNull { publicationFile.file.delete() }
            progress?.dismiss()
            if (foreground) catalogView.longSnackbar("unable to move publication into the library")
            return
        }

        val extension = libraryFile.let {
            it.format()?.fileExtension
                ?: it.file.extension
        }

        val isRwpm = libraryFile.format()?.mediaType?.isRwpm ?: false

        val bddHref =
            if (!isRwpm)
                libraryFile.path
            else
                libraryFile.sourceUrl
                    ?: run {
                        Timber.e("Trying to add a RWPM to the database from a file without sourceUrl.")
                        progress?.dismiss()
                        return
                    }

        streamer.open(libraryFile, allowUserInteraction = false)
            .onSuccess {
                addPublicationToDatabase(bddHref, extension, it).let {success ->
                    progress?.dismiss()
                    val msg =
                        if (success)
                            "publication added to your library"
                        else
                            "unable to add publication to the database"
                    if (foreground)
                        catalogView.longSnackbar(msg)
                    else
                        Timber.d(msg)
                    if (success && isRwpm)
                        tryOrNull { libraryFile.file.delete() }
                }
            }
            .onFailure {
                tryOrNull { libraryFile.file.delete() }
                Timber.d(it)
                progress?.dismiss()
                if (foreground) catalogView.longSnackbar("failed to open publication")
            }
    }

    private suspend fun addPublicationToDatabase(href: String, extension: String, publication: Publication): Boolean {
        val publicationIdentifier = publication.metadata.identifier ?: ""
        val author = publication.metadata.authorName
        val cover = publication.cover()?.toPng()

        val book = Book(
            title = publication.metadata.title,
            author = author,
            href = href,
            identifier = publicationIdentifier,
            cover = cover,
            ext = ".$extension",
            progression = "{}"
        )

        return addBookToDatabase(book)
    }

    private suspend fun addBookToDatabase(book: Book, alertDuplicates: Boolean = true): Boolean {
        database.books.insert(book, allowDuplicates = !alertDuplicates)?.let { id ->
            book.id = id
            books.add(0, book)
            withContext(Dispatchers.Main) {
                booksAdapter.notifyDataSetChanged()
            }
            return true
        }

        return if (alertDuplicates && confirmAddDuplicateBook(book))
            addBookToDatabase(book, alertDuplicates = false)
        else
            false
    }

    private suspend fun URL.copyToTempFile(): R2File? = tryOrNull {
        val filename = UUID.randomUUID().toString()
        val file = File("$R2DIRECTORY$filename.$extension")
        download(file.path).let {
            if (it)
                R2File(file.path, sourceUrl = this.toString())
            else
                null
        }
    }

    private suspend fun Uri.copyToTempFile(): R2File? = tryOrNull {
        val filename = UUID.randomUUID().toString()
        val format = Format.ofUri(this, contentResolver)
        val file = R2File("$R2DIRECTORY$filename.${format?.fileExtension}", format = format)
        ContentResolverUtil.getContentInputStream(this@LibraryActivity, this, file.path)
        return file
    }

    private suspend fun InputStream.copyToTempFile(): R2File? = tryOrNull {
        val filename = UUID.randomUUID().toString()
        R2File(R2DIRECTORY + filename)
            .also { toFile(it.path) }
    }

    override fun recyclerViewListLongClicked(v: View, position: Int) {
        //Inflating the layout
        val layout = LayoutInflater.from(this).inflate(R.layout.popup_delete, catalogView, false)
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
            books.remove(book)
            booksAdapter.notifyDataSetChanged()
            tryOrNull { File(book.href).delete() }
            val deleted = database.books.delete(book)
            if (deleted > 0) {
                BookmarksDatabase(this).bookmarks.delete(deleted.toLong())
                PositionsDatabase(this).positions.delete(deleted.toLong())
            }
            popup.dismiss()
            catalogView.longSnackbar("publication deleted from your library")
        }
    }

    private suspend fun confirmAddDuplicateBook(book: Book): Boolean = suspendCoroutine { cont ->
        alert(Appcompat, "Publication already exists") {
            positiveButton("Add anyway") {
                it.dismiss()
                cont.resume(true)
            }
            negativeButton("Cancel") {
                it.dismiss()
                cont.resume(false)
            }
        }.build().apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            show()
        }
    }

    override fun recyclerViewListClicked(v: View, position: Int) {
        val progress = blockingProgressDialog(getString(R.string.progress_wait_while_preparing_book))
        /*
        FIXME: if the progress dialog were shown, the LCP popup window would not be accessible to the user.
        progress.show()
         */
        progress.dismiss()

        launch {
            val booksDB = BooksDatabase(this@LibraryActivity)
            val book = books[position]

            val remoteUrl = tryOrNull { URL(book.href).copyToTempFile() }
            val format = Format.of(fileExtension = book.ext.removePrefix("."))
            val file = remoteUrl // remote file
                ?: R2File(book.href, format = format) // local file

            streamer.open(file, allowUserInteraction = true)
                .onFailure {
                    Timber.d(it)
                    progress.dismiss()
                    catalogView.longSnackbar("unable to open publication") }
                .onSuccess {
                    if (it.isRestricted) {
                        progress.dismiss()
                        if (it.protectionError != null) {
                            Timber.d(it.protectionError)
                            catalogView.longSnackbar("unable to unlock publication")
                        }
                    } else {
                        prepareToServe(it, file)
                        progress.dismiss()
                        navigatorLauncher.launch(
                            NavigatorContract.Input(
                                file = file,
                                format = format,
                                publication = it,
                                bookId = book.id,
                                initialLocator = book.id?.let { id -> booksDB.books.currentLocator(id) },
                                deleteOnResult = remoteUrl != null,
                                baseUrl = Publication.localBaseUrlOf(file.name, localPort)
                            )
                    	)
                    }
                }
        }
    }

    private fun prepareToServe(publication: Publication, file: R2File) {
        val key = publication.metadata.identifier ?: publication.metadata.title
        preferences.edit().putString("$key-publicationPort", localPort.toString()).apply()
        val userProperties = applicationContext.filesDir.path + "/" + Injectable.Style.rawValue + "/UserProperties.json"
        server.addEpub(publication, null, "/${file.name}", userProperties)
    }

    class VerticalSpaceItemDecoration(private val verticalSpaceHeight: Int) : androidx.recyclerview.widget.RecyclerView.ItemDecoration() {

        override fun getItemOffsets(outRect: Rect, view: View, parent: androidx.recyclerview.widget.RecyclerView,
                                    state: androidx.recyclerview.widget.RecyclerView.State) {
            outRect.bottom = verticalSpaceHeight
        }
    }

    override suspend fun fulfill(file: File): Try<DRMFulfilledPublication, Exception> =
        Try.failure(Exception("DRM not supported"))

    companion object {

        var isServerStarted = false
            private set

    }

}
